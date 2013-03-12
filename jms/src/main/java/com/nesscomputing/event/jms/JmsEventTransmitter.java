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

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventTransmitter;
import com.nesscomputing.jms.JmsRunnableFactory;
import com.nesscomputing.jms.TopicProducer;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;

/**
 * Transmits an event onto a JMS topic.
 */
@Singleton
public class JmsEventTransmitter implements NessEventTransmitter
 {
     private static final Log LOG = Log.findLog();

     private final JmsEventConfig jmsEventConfig;

     private final AtomicReference<TopicProducer<Object>> topicProducerHolder = new AtomicReference<TopicProducer<Object>>();
     private final AtomicReference<Thread> producerThreadHolder = new AtomicReference<Thread>();

     private final AtomicInteger eventsTransmitted = new AtomicInteger(0);

     @Inject
     public JmsEventTransmitter(final JmsEventConfig jmsEventConfig)
     {
         Preconditions.checkNotNull(jmsEventConfig, "The config must not be null!");
         this.jmsEventConfig = jmsEventConfig;
     }

     @Inject(optional = true)
     public void injectTopicFactory(@Named(JMS_EVENT_NAME) final JmsRunnableFactory topicFactory)
     {
         Preconditions.checkNotNull(topicFactory, "The topic factory must not be null!");
         this.topicProducerHolder.set(topicFactory.createTopicJsonProducer(jmsEventConfig.getTopicName()));
     }

     @OnStage(LifecycleStage.START)
     public void start()
     {
         final TopicProducer<Object> topicProducer = topicProducerHolder.get();
         if (topicProducer != null) {
             Preconditions.checkState(producerThreadHolder.get() == null, "already started, boldly refusing to start twice!");
             final Thread producerThread = new Thread(topicProducer, "ness-event-jms-producer");
             Preconditions.checkState(producerThreadHolder.getAndSet(producerThread) == null, "thread already set, this should not happen!");

             producerThread.setDaemon(true);
             producerThread.start();
         }
         else {
             LOG.debug("JMS seems to be disabled, skipping Event transmitter start!");
         }
     }

     @OnStage(LifecycleStage.STOP)
     public void stop()
     {
         final Thread producerThread = producerThreadHolder.getAndSet(null);
         if (producerThread != null) {
             try {
                 final TopicProducer<Object> topicProducer = topicProducerHolder.getAndSet(null);
                 if (topicProducer != null) {
                     topicProducer.shutdown();

                     producerThread.interrupt();
                     producerThread.join(500L);
                 }
             }
             catch (InterruptedException ie) {
                 Thread.currentThread().interrupt(); // Someone else needs to handle that.
             }
         }
         else {
             LOG.debug("Never started, ignoring stop()");
         }
     }

     public boolean isConnected()
     {
         final TopicProducer<Object> topicProducer = topicProducerHolder.get();
         return (topicProducer != null && topicProducer.isConnected());
     }

     public int getEventsTransmittedCount()
     {
         return eventsTransmitted.get();
     }

     @Override
     public void transmit(@Nonnull final NessEvent event)
     {
         Preconditions.checkArgument(event != null, "An event can not be null!");

         final TopicProducer<Object> topicProducer = topicProducerHolder.get();
         if (topicProducer == null) {
             return;
         }
         if (!topicProducer.offerWithTimeout(event)) {
             LOG.warn("Could not offer message '%s' to topic, maybe stuck?", event);
         }
         else {
             eventsTransmitted.incrementAndGet();
             LOG.trace("Successfully offered '%s' to queue", event);
         }
     }
}
