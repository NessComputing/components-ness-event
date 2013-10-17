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
package com.nesscomputing.event.jms;

import static com.nesscomputing.event.jms.JmsEventModule.JMS_EVENT_NAME;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.JMSException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventDispatcher;
import com.nesscomputing.jms.AbstractConsumer;
import com.nesscomputing.jms.ConsumerCallback;
import com.nesscomputing.jms.JmsRunnableFactory;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;

/**
 * Received an Event from the JMS message queue and dispatches it to the event system.
 */
@Singleton
public class JmsEventReceiver implements ConsumerCallback<String>
{
    private static final Log LOG = Log.findLog();

    private final JmsEventConfig jmsEventConfig;
    private final NessEventDispatcher eventDispatcher;
    private final ObjectMapper mapper;

    private final AtomicReference<AbstractConsumer> consumerHolder = new AtomicReference<AbstractConsumer>();
    private final AtomicReference<Thread> consumerThreadHolder = new AtomicReference<Thread>();

    private final AtomicInteger eventsReceived = new AtomicInteger(0);

    @Inject
    public JmsEventReceiver(final JmsEventConfig jmsEventConfig,
                     final NessEventDispatcher eventDispatcher,
                     final ObjectMapper mapper)
    {
        this.eventDispatcher = eventDispatcher;
        this.mapper = mapper;
        this.jmsEventConfig = jmsEventConfig;
    }

    @Inject(optional = true)
    public void injectTopicFactory(@Named(JMS_EVENT_NAME) final JmsRunnableFactory topicFactory)
    {
        final AbstractConsumer consumer;
        if (jmsEventConfig.isUseQueue()) {
            consumer = topicFactory.createQueueTextMessageListener(jmsEventConfig.getTopicName(), this);
        } else {
            consumer = topicFactory.createTopicTextMessageListener(jmsEventConfig.getTopicName() , this);
        }
        this.consumerHolder.set(consumer);
    }

    @OnStage(LifecycleStage.START)
    public void start()
    {
        final AbstractConsumer consumer = consumerHolder.get();
        if (consumer != null) {
            Preconditions.checkState(consumerThreadHolder.get() == null, "already started, boldly refusing to start twice!");
            final Thread consumerThread = new Thread(consumer, "ness-event-jms-consumer");
            Preconditions.checkState(consumerThreadHolder.getAndSet(consumerThread) == null, "thread already set, this should not happen!");

            consumerThread.setDaemon(true);
            consumerThread.start();
        }
        else {
            LOG.debug("JMS seems to be disabled, skipping Event receiver start!");
        }
    }

    @OnStage(LifecycleStage.STOP)
    public void stop()
    {
        final Thread consumerThread = consumerThreadHolder.getAndSet(null);
        if (consumerThread != null) {
            try {
                final AbstractConsumer consumer = consumerHolder.getAndSet(null);
                if (consumer != null) {
                    consumer.shutdown();

                    consumerThread.interrupt();
                    consumerThread.join(500L);
                }
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt();  // Someone else needs to handle that.
            }
        }
        else {
            LOG.debug("Never started, ignoring stop()");
        }
    }

    public boolean isConnected()
    {
        final AbstractConsumer consumer = consumerHolder.get();
        return (consumer != null && consumer.isConnected());
    }

    public int getEventsReceivedCount()
    {
        return eventsReceived.get();
    }

    @Override
    public boolean withMessage(final String text) throws JMSException
    {
        if (text == null) {
            return true;
        }

        final NessEvent event;
        try {
            event = mapper.readValue(text, NessEvent.class);
        }
        catch (Exception e) {
            // Make sure that we catch all possible exceptions here that could
            // be thrown by the deserializer. Otherwise, e.g. an IllegalArgumentException will
            // kill the JMS receiver thread.
            LOG.warnDebug(e, "Could not parse message '%s', ignoring!", text);
            return true;
        }

        try {
            eventsReceived.incrementAndGet();
            eventDispatcher.dispatch(event);
        }
        catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, JMSException.class);
            LOG.error(e, "Exception in event dispatcher.");
        }

        return true;
    }
}
