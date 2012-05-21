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

import static java.lang.String.format;

import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventModule;
import com.nesscomputing.event.NessEventSender;
import com.nesscomputing.event.NessEventType;
import com.nesscomputing.event.jms.JmsEventConfig;
import com.nesscomputing.event.jms.JmsEventModule;
import com.nesscomputing.event.jms.util.CountingEventReceiver;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.jms.JmsConfig;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.testing.lessio.AllowDNSResolution;

@AllowDNSResolution
public class TestJmsEventTransport
{
    private static final NessEventType TEST_EVENT_TYPE = NessEventType.getForName("TEST_EVENT");
    private static final UUID USER = UUID.randomUUID();
    private static final NessEvent TEST_EVENT = NessEvent.createEvent(USER, TEST_EVENT_TYPE);

    @Inject
    private Lifecycle lifecycle = null;

    @Inject
    private NessEventSender sender;

    @Inject
    private CountingEventReceiver receiver;

    private static Connection CONNECTION = null;
    private static String BROKER_URI = null;

    @BeforeClass
    public static void startBroker() throws Exception
    {
        BROKER_URI = format("vm:broker:(vm://testbroker-%s)?persistent=false&useJmx=false", UUID.randomUUID().toString());
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URI);
        Assert.assertNull(CONNECTION);
        CONNECTION = connectionFactory.createConnection();
        Thread.sleep(2000L);
    }

    @AfterClass
    public static void shutdownBroker() throws Exception
    {
        Assert.assertNotNull(CONNECTION);
        CONNECTION.close();
        CONNECTION = null;
    }

    @Before
    public void setUp() throws Exception
    {
        Assert.assertNotNull(CONNECTION);

        final Config config = Config.getFixedConfig(ImmutableMap.of("ness.event.jms.enabled", "true",
                                                                                         "ness.event.transport", "jms",
                                                                                         "ness.jms.jms-event.enabled", "true",
                                                                                         "ness.jms.jms-event.connection-url", BROKER_URI));
        final CountingEventReceiver testEventReceiver = new CountingEventReceiver(TEST_EVENT_TYPE);
        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       new ConfigModule(config),
                                                       new LifecycleModule(),
                                                       new NessEventModule(),
                                                       new NessJacksonModule(),
                                                       new JmsEventModule(config),
                                                       new Module() {
                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                               binder.bind(CountingEventReceiver.class).toInstance(testEventReceiver);
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver);
                                                           }
                                                       });

        final JmsEventConfig eventConfig = injector.getInstance(JmsEventConfig.class);
        final JmsConfig jmsConfig = injector.getInstance(Key.get(JmsConfig.class, JmsEventModule.JMS_EVENT_NAMED));
        Assert.assertTrue(eventConfig.isEnabled());
        Assert.assertTrue(jmsConfig.isEnabled());

        injector.injectMembers(this);

        Assert.assertNotNull(sender);
        Assert.assertNotNull(receiver);

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);
    }

    @After
    public void tearDown() throws Exception
    {
        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.STOP_STAGE);
        lifecycle = null;
    }

    @Test
    public void testSendAndReceive() throws Exception
    {
        final int maxCount = 1000;

        // Warm up the ObjectMapper.
        sender.enqueue(TEST_EVENT);
        Thread.sleep(100L);

        for (int i = 1; i < maxCount; i++) {
            Thread.sleep(4L);
            sender.enqueue(TEST_EVENT);
        }

        Thread.sleep(1000L);

        final NessEvent testEvent = receiver.getEvent();
        Assert.assertNotNull(testEvent);
        Assert.assertEquals(TEST_EVENT, testEvent);
        Assert.assertEquals(maxCount, receiver.getCount());
    }
}



