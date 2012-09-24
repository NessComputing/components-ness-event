/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nesscomputing.event.amqp;

import static com.nesscomputing.event.amqp.AmqpEventModule.AMQP_EVENT_NAME;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.nesscomputing.amqp.AmqpRunnableFactory;
import com.nesscomputing.amqp.ConsumerCallback;
import com.nesscomputing.amqp.ExchangeConsumer;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventDispatcher;
import com.nesscomputing.jackson.Json;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;
import com.rabbitmq.client.QueueingConsumer.Delivery;

/**
 * Received an Event from the JMS message queue and dispatches it to the event system.
 */
@Singleton
public class AmqpEventReceiver implements ConsumerCallback
{
    private static final Log LOG = Log.findLog();

    private final AmqpEventConfig amqpEventConfig;
    private final NessEventDispatcher eventDispatcher;
    private final ObjectMapper mapper;

    private final AtomicReference<ExchangeConsumer> exchangeConsumerHolder = new AtomicReference<ExchangeConsumer>();
    private final AtomicReference<Thread> consumerThreadHolder = new AtomicReference<Thread>();

    private final AtomicInteger eventsReceived = new AtomicInteger(0);

    @Inject
    AmqpEventReceiver(final AmqpEventConfig amqpEventConfig,
                     final NessEventDispatcher eventDispatcher,
                     @Json final ObjectMapper mapper)
    {
        this.eventDispatcher = eventDispatcher;
        this.mapper = mapper;
        this.amqpEventConfig = amqpEventConfig;
    }

    @Inject(optional = true)
    void injectExchangeFactory(@Named(AMQP_EVENT_NAME) final AmqpRunnableFactory exchangeFactory)
    {
        this.exchangeConsumerHolder.set(exchangeFactory.createExchangeListener(amqpEventConfig.getExchangeName(), this));
    }

    @OnStage(LifecycleStage.START)
    void start()
    {
        final ExchangeConsumer exchangeConsumer = exchangeConsumerHolder.get();
        if (exchangeConsumer != null) {
            Preconditions.checkState(consumerThreadHolder.get() == null, "already started, boldly refusing to start twice!");
            final Thread consumerThread = new Thread(exchangeConsumer, "ness-event-amqp-consumer");
            Preconditions.checkState(consumerThreadHolder.getAndSet(consumerThread) == null, "thread already set, this should not happen!");

            consumerThread.setDaemon(true);
            consumerThread.start();
        }
        else {
            LOG.debug("AMQP seems to be disabled, skipping Event receiver start!");
        }
    }

    @OnStage(LifecycleStage.STOP)
    void stop()
        throws InterruptedException
    {
        final Thread consumerThread = consumerThreadHolder.getAndSet(null);
        if (consumerThread != null) {
            final ExchangeConsumer exchangeConsumer = exchangeConsumerHolder.getAndSet(null);
            if (exchangeConsumer != null) {
                exchangeConsumer.shutdown();

                consumerThread.interrupt();
                consumerThread.join(500L);
            }
        }
        else {
            LOG.debug("Never started, ignoring stop()");
        }
    }

    public boolean isConnected()
    {
        final ExchangeConsumer exchangeConsumer = exchangeConsumerHolder.get();
        return (exchangeConsumer != null && exchangeConsumer.isConnected());
    }

    public int getEventsReceivedCount()
    {
        return eventsReceived.get();
    }

    @Override
    public boolean withDelivery(final Delivery delivery) throws IOException
    {
        if (delivery != null) {
            try {
                final NessEvent event = mapper.readValue(delivery.getBody(), NessEvent.class);
                eventsReceived.incrementAndGet();
                eventDispatcher.dispatch(event);
            }
            catch (Exception e) {
                Throwables.propagateIfInstanceOf(e, IOException.class);
                // Make sure that we catch all possible exceptions here that could
                // be thrown by the deserializer. Otherwise, e.g. an IllegalArgumentException might
                // kill the receiver thread.
                LOG.warnDebug(e, "Could not parse message '%s', ignoring!", delivery);
            }
        }

        return true;
    }
}
