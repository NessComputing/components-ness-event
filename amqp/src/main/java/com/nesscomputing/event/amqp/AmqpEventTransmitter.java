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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.nesscomputing.amqp.AmqpRunnableFactory;
import com.nesscomputing.amqp.ExchangePublisher;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventTransmitter;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;

/**
 * Transmits an event onto an AMQP exchange.
 */
@Singleton
public class AmqpEventTransmitter implements NessEventTransmitter
 {
     private static final Log LOG = Log.findLog();

     private final AmqpEventConfig amqpEventConfig;

     private final AtomicReference<ExchangePublisher<NessEvent>> exchangePublisherHolder = new AtomicReference<ExchangePublisher<NessEvent>>();
     private final AtomicReference<Thread> producerThreadHolder = new AtomicReference<Thread>();

     private AtomicInteger eventsTransmitted = new AtomicInteger(0);

     @Inject
     AmqpEventTransmitter(final AmqpEventConfig amqpEventConfig)
     {
         this.amqpEventConfig = amqpEventConfig;
     }

     @Inject(optional = true)
     void injectExchangeFactory(@Named(AMQP_EVENT_NAME) final AmqpRunnableFactory exchangeFactory)
     {
         this.exchangePublisherHolder.set(exchangeFactory.<NessEvent>createExchangeJsonPublisher(amqpEventConfig.getExchangeName()));
     }

     @OnStage(LifecycleStage.START)
     void start()
     {
         final ExchangePublisher<NessEvent> exchangePublisher = exchangePublisherHolder.get();
         if (exchangePublisher != null) {
             Preconditions.checkState(producerThreadHolder.get() == null, "already started, boldly refusing to start twice!");
             final Thread producerThread = new Thread(exchangePublisher, "ness-event-amqp-producer");
             Preconditions.checkState(producerThreadHolder.getAndSet(producerThread) == null, "thread already set, this should not happen!");

             producerThread.setDaemon(true);
             producerThread.start();
         }
         else {
             LOG.debug("AMQP seems to be disabled, skipping Event transmitter start!");
         }
     }

     @OnStage(LifecycleStage.STOP)
     void stop()
         throws InterruptedException
     {
         final Thread producerThread = producerThreadHolder.getAndSet(null);
         if (producerThread != null) {
             final ExchangePublisher<NessEvent> exchangePublisher = exchangePublisherHolder.getAndSet(null);
             if (exchangePublisher != null) {
                 exchangePublisher.shutdown();

                 producerThread.interrupt();
                 producerThread.join(500L);
             }
         }
         else {
             LOG.debug("Never started, ignoring stop()");
         }
     }

     public boolean isConnected()
     {
         final ExchangePublisher<NessEvent> exchangePublisher = exchangePublisherHolder.get();
         return (exchangePublisher != null && exchangePublisher.isConnected());
     }

     public int getEventsTransmittedCount()
     {
         return eventsTransmitted.get();
     }

     @Override
     public void transmit(@Nonnull final NessEvent event)
     {
         Preconditions.checkArgument(event != null, "An event can not be null!");

         final ExchangePublisher<NessEvent> exchangePublisher = exchangePublisherHolder.get();
         if (exchangePublisher == null) {
             return;
         }
         if (!exchangePublisher.offerWithTimeout(event)) {
             LOG.warn("Could not offer message '%s' to exchange, maybe stuck?", event);
         }
         else {
             eventsTransmitted.incrementAndGet();
             LOG.trace("Successfully offered '%s' to queue", event);
         }
     }
}
