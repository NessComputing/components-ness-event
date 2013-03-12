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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Named;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventModule;
import com.nesscomputing.event.NessEventSender;
import com.nesscomputing.event.NessEventType;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.jms.JmsConfig;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.testing.lessio.AllowDNSResolution;
import com.nesscomputing.testing.lessio.AllowNetworkListen;

@AllowDNSResolution
@AllowNetworkListen(ports={0})
public class TestJmsEventModule
{
    @Inject
    private final JmsEventConfig eventConfig = null;

    @Inject(optional=true)
    @Named(JmsEventModule.JMS_EVENT_NAME)
    private final JmsConfig jmsConfig = null;

    @Inject(optional=true)
    private final Lifecycle lifecycle = null;

    @Inject(optional=true)
    private final JmsEventReceiver jmsEventReceiver = null;

    @Inject(optional=true)
    private final JmsEventTransmitter jmsEventTransport = null;

    @Inject(optional=true)
    private final NessEventSender eventSender = null;

    private static Connection CONNECTION = null;
    private static String BROKER_URI = null;

    private void doInjection(final Config config)
    {
        Guice.createInjector(Stage.PRODUCTION,
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
                                     binder.requestInjection(TestJmsEventModule.this);
                                 }
                             });

    }

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
    public void setUp()
    {
        Assert.assertNotNull(CONNECTION);
    }

    @Test
    public void testSpinupDisabled()
    {
        doInjection(Config.getEmptyConfig());

        Assert.assertNotNull(eventConfig);
        Assert.assertFalse(eventConfig.isEnabled());
    }

    @Test
    public void testSpinupEnabledButJmsDisabled()
    {
        final Config config = Config.getFixedConfig(ImmutableMap.of("ness.event.jms.enabled", "true",
                                                                                         "ness.event.transport", "jms",
                                                                                         "ness.jms.jms-event.enabled", "false"));
        doInjection(config);
        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(jmsConfig);
        Assert.assertFalse(jmsConfig.isEnabled());
    }

    @Test
    public void testSpinupEnabled()
    {
        final Config config = Config.getFixedConfig(ImmutableMap.of("ness.event.jms.enabled", "true",
                                                                                         "ness.event.transport", "jms",
                                                                                         "ness.jms.jms-event.enabled", "true",
                                                                                         "ness.jms.jms-event.connection-url", BROKER_URI));
        doInjection(config);
        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(jmsConfig);
        Assert.assertTrue(jmsConfig.isEnabled());
    }

    @Test
    public void testSpinupLifeycle() throws Exception
    {
        final Config config = Config.getFixedConfig(ImmutableMap.of("ness.event.jms.enabled", "true",
                                                                                         "ness.event.transport", "jms",
                                                                                         "ness.jms.jms-event.enabled", "true",
                                                                                         "ness.jms.jms-event.connection-url", BROKER_URI));
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(jmsConfig);
        Assert.assertTrue(jmsConfig.isEnabled());

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);

        Thread.sleep(200L);

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);

        Thread.sleep(200L);
    }

    @Test
    public void testEnabledAndInert() throws Exception
    {
        final Config config = Config.getFixedConfig(ImmutableMap.<String, String>builder()
                                                                         .put("ness.event.jms.enabled", "true")
                                                                         .put("ness.event.transport", "jms")
                                                                         .put("ness.jms.jms-event.enabled", "true")
                                                                         .put("ness.jms.jms-event.connection-url", BROKER_URI)
                                                                         .put("ness.event.jms.listen-enabled", "false")
                                                                         .put("ness.event.jms.transmit-enabled", "false")
                                                                         .build());
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(jmsConfig);
        Assert.assertTrue(jmsConfig.isEnabled());

        Assert.assertFalse(eventConfig.isListenEnabled());
        Assert.assertNull(jmsEventReceiver);

        Assert.assertFalse(eventConfig.isTransmitEnabled());
        Assert.assertNull(jmsEventTransport);

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);

        Thread.sleep(200L);

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);

        Thread.sleep(200L);
    }

    @Test
    public void testEnabledAndReadOnly() throws Exception
    {
        final Config config = Config.getFixedConfig(ImmutableMap.<String, String>builder()
                                                                         .put("ness.event.jms.enabled", "true")
                                                                         .put("ness.event.transport", "jms")
                                                                         .put("ness.jms.jms-event.enabled", "true")
                                                                         .put("ness.jms.jms-event.connection-url", BROKER_URI)
                                                                         .put("ness.event.jms.listen-enabled", "true")
                                                                         .put("ness.event.jms.transmit-enabled", "false")
                                                                         .build());
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(jmsConfig);
        Assert.assertTrue(jmsConfig.isEnabled());

        Assert.assertTrue(eventConfig.isListenEnabled());
        Assert.assertNotNull(jmsEventReceiver);

        Assert.assertFalse(eventConfig.isTransmitEnabled());
        Assert.assertNull(jmsEventTransport);

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);

        Thread.sleep(500L);

        Assert.assertTrue(jmsEventReceiver.isConnected());

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);

        Thread.sleep(200L);
    }

    @Test
    public void testEnabledAndWriteOnly() throws Exception
    {
        final Config config = Config.getFixedConfig(ImmutableMap.<String, String>builder()
                                                                         .put("ness.event.jms.enabled", "true")
                                                                         .put("ness.event.transport", "jms")
                                                                         .put("ness.jms.jms-event.enabled", "true")
                                                                         .put("ness.jms.jms-event.connection-url", BROKER_URI)
                                                                         .put("ness.event.jms.listen-enabled", "false")
                                                                         .put("ness.event.jms.transmit-enabled", "true")
                                                                         .build());
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(jmsConfig);
        Assert.assertTrue(jmsConfig.isEnabled());

        Assert.assertFalse(eventConfig.isListenEnabled());
        Assert.assertNull(jmsEventReceiver);

        Assert.assertTrue(eventConfig.isTransmitEnabled());
        Assert.assertNotNull(jmsEventTransport);

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);

        Thread.sleep(200L);

        Assert.assertFalse(jmsEventTransport.isConnected());
        Assert.assertNotNull(eventSender);
        eventSender.enqueue(NessEvent.createEvent(null, NessEventType.getForName(null)));
        Thread.sleep(1000L);
        Assert.assertTrue(jmsEventTransport.isConnected());

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);

        Thread.sleep(200L);
    }

    @Test
    public void testEnabledAndReadWrite() throws Exception
    {
        final Config config = Config.getFixedConfig(ImmutableMap.<String, String>builder()
                                                                         .put("ness.event.jms.enabled", "true")
                                                                         .put("ness.event.transport", "jms")
                                                                         .put("ness.jms.jms-event.enabled", "true")
                                                                         .put("ness.jms.jms-event.connection-url", BROKER_URI)
                                                                         .put("ness.event.jms.listen-enabled", "true")
                                                                         .put("ness.event.jms.transmit-enabled", "true")
                                                                         .build());
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(jmsConfig);
        Assert.assertTrue(jmsConfig.isEnabled());

        Assert.assertTrue(eventConfig.isListenEnabled());
        Assert.assertNotNull(jmsEventReceiver);

        Assert.assertTrue(eventConfig.isTransmitEnabled());
        Assert.assertNotNull(jmsEventTransport);

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);

        Thread.sleep(200L);

        Assert.assertTrue(jmsEventReceiver.isConnected());
        Assert.assertFalse(jmsEventTransport.isConnected());
        Assert.assertNotNull(eventSender);
        eventSender.enqueue(NessEvent.createEvent(null, NessEventType.getForName(null)));
        Thread.sleep(1000L);
        Assert.assertTrue(jmsEventTransport.isConnected());

        Assert.assertEquals(1, jmsEventTransport.getEventsTransmittedCount());
        Assert.assertEquals(1, jmsEventReceiver.getEventsReceivedCount());

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);

        Thread.sleep(200L);
    }
}



