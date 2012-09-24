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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.nesscomputing.amqp.AmqpConfig;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventModule;
import com.nesscomputing.event.NessEventSender;
import com.nesscomputing.event.NessEventType;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.testing.lessio.AllowDNSResolution;
import com.nesscomputing.testing.lessio.AllowNetworkListen;

@AllowDNSResolution
@AllowNetworkListen(ports={0})
public class TestAmqpEventModule
{
    @Inject
    private AmqpEventConfig eventConfig = null;

    @Inject(optional=true)
    @Named(AmqpEventModule.AMQP_EVENT_NAME)
    private AmqpConfig amqpConfig = null;

    @Inject(optional=true)
    private Lifecycle lifecycle = null;

    @Inject(optional=true)
    private AmqpEventReceiver amqpEventReceiver = null;

    @Inject(optional=true)
    private AmqpEventTransmitter amqpEventTransport = null;

    @Inject(optional=true)
    private NessEventSender eventSender = null;

    private QPidUtils qpid = new QPidUtils();

    private String brokerUri;

    private void doInjection(final Config config)
    {
        Guice.createInjector(Stage.PRODUCTION,
                             new ConfigModule(config),
                             new LifecycleModule(),
                             new NessEventModule(),
                             new NessJacksonModule(),
                             new AmqpEventModule(config),
                             new Module() {
                                 @Override
                                 public void configure(final Binder binder) {
                                     binder.disableCircularProxies();
                                     binder.requireExplicitBindings();
                                     binder.requestInjection(TestAmqpEventModule.this);
                                 }
                             });

    }

    @Before
    public void setUp() throws Exception
    {
        qpid.startup();

        brokerUri = qpid.getUri();
        Assert.assertNotNull(brokerUri);
    }

    @After
    public void shutdownBroker() throws Exception
    {
        qpid.shutdown();
    }

    @Test
    public void testSpinupDisabled()
    {
        doInjection(Config.getEmptyConfig());

        Assert.assertNotNull(eventConfig);
        Assert.assertFalse(eventConfig.isEnabled());
    }

    @Test
    public void testSpinupEnabledButAmqpDisabled()
    {
        final Config config = Config.getFixedConfig(ImmutableMap.of("ness.event.amqp.enabled", "true",
                                                                    "ness.event.transport", "amqp",
                                                                    "ness.amqp.amqp-event.enabled", "false"));
        doInjection(config);
        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(amqpConfig);
        Assert.assertFalse(amqpConfig.isEnabled());
    }

    @Test
    public void testSpinupEnabled()
    {
        final Config config = Config.getFixedConfig(ImmutableMap.of("ness.event.amqp.enabled", "true",
                                                                    "ness.event.transport", "amqp",
                                                                    "ness.amqp.amqp-event.enabled", "true",
                                                                    "ness.amqp.amqp-event.connection-url", brokerUri));
        doInjection(config);
        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(amqpConfig);
        Assert.assertTrue(amqpConfig.isEnabled());
    }

    @Test
    public void testSpinupLifeycle() throws Exception
    {
        final Config config = Config.getFixedConfig(ImmutableMap.of("ness.event.amqp.enabled", "true",
                                                                    "ness.event.transport", "amqp",
                                                                    "ness.amqp.amqp-event.enabled", "true",
                                                                    "ness.amqp.amqp-event.connection-url", brokerUri));
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(amqpConfig);
        Assert.assertTrue(amqpConfig.isEnabled());

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
                                                                         .put("ness.event.amqp.enabled", "true")
                                                                         .put("ness.event.transport", "amqp")
                                                                         .put("ness.amqp.amqp-event.enabled", "true")
                                                                         .put("ness.amqp.amqp-event.connection-url", brokerUri)
                                                                         .put("ness.event.amqp.listen-enabled", "false")
                                                                         .put("ness.event.amqp.transmit-enabled", "false")
                                                                         .build());
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(amqpConfig);
        Assert.assertTrue(amqpConfig.isEnabled());

        Assert.assertFalse(eventConfig.isListenEnabled());
        Assert.assertNull(amqpEventReceiver);

        Assert.assertFalse(eventConfig.isTransmitEnabled());
        Assert.assertNull(amqpEventTransport);

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
                                                                         .put("ness.event.amqp.enabled", "true")
                                                                         .put("ness.event.transport", "amqp")
                                                                         .put("ness.amqp.amqp-event.enabled", "true")
                                                                         .put("ness.amqp.amqp-event.connection-url", brokerUri)
                                                                         .put("ness.event.amqp.listen-enabled", "true")
                                                                         .put("ness.event.amqp.transmit-enabled", "false")
                                                                         .build());
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(amqpConfig);
        Assert.assertTrue(amqpConfig.isEnabled());

        Assert.assertTrue(eventConfig.isListenEnabled());
        Assert.assertNotNull(amqpEventReceiver);

        Assert.assertFalse(eventConfig.isTransmitEnabled());
        Assert.assertNull(amqpEventTransport);

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);

        Thread.sleep(500L);

        Assert.assertTrue(amqpEventReceiver.isConnected());

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);

        Thread.sleep(200L);
    }

    @Test
    public void testEnabledAndWriteOnly() throws Exception
    {
        final Config config = Config.getFixedConfig(ImmutableMap.<String, String>builder()
                                                                         .put("ness.event.amqp.enabled", "true")
                                                                         .put("ness.event.transport", "amqp")
                                                                         .put("ness.amqp.amqp-event.enabled", "true")
                                                                         .put("ness.amqp.amqp-event.connection-url", brokerUri)
                                                                         .put("ness.event.amqp.listen-enabled", "false")
                                                                         .put("ness.event.amqp.transmit-enabled", "true")
                                                                         .build());
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(amqpConfig);
        Assert.assertTrue(amqpConfig.isEnabled());

        Assert.assertFalse(eventConfig.isListenEnabled());
        Assert.assertNull(amqpEventReceiver);

        Assert.assertTrue(eventConfig.isTransmitEnabled());
        Assert.assertNotNull(amqpEventTransport);

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);

        Thread.sleep(200L);

        Assert.assertFalse(amqpEventTransport.isConnected());
        Assert.assertNotNull(eventSender);
        eventSender.enqueue(NessEvent.createEvent(null, NessEventType.getForName(null)));
        Thread.sleep(100L);
        Assert.assertTrue(amqpEventTransport.isConnected());

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);

        Thread.sleep(200L);
    }

    @Test
    public void testEnabledAndReadWrite() throws Exception
    {
        final Config config = Config.getFixedConfig(ImmutableMap.<String, String>builder()
                                                                         .put("ness.event.amqp.enabled", "true")
                                                                         .put("ness.event.transport", "amqp")
                                                                         .put("ness.amqp.amqp-event.enabled", "true")
                                                                         .put("ness.amqp.amqp-event.connection-url", brokerUri)
                                                                         .put("ness.event.amqp.listen-enabled", "true")
                                                                         .put("ness.event.amqp.transmit-enabled", "true")
                                                                         .build());
        doInjection(config);

        Assert.assertNotNull(eventConfig);
        Assert.assertTrue(eventConfig.isEnabled());

        Assert.assertNotNull(amqpConfig);
        Assert.assertTrue(amqpConfig.isEnabled());

        Assert.assertTrue(eventConfig.isListenEnabled());
        Assert.assertNotNull(amqpEventReceiver);

        Assert.assertTrue(eventConfig.isTransmitEnabled());
        Assert.assertNotNull(amqpEventTransport);

        Assert.assertNotNull(lifecycle);
        lifecycle.executeTo(LifecycleStage.START_STAGE);

        Thread.sleep(200L);

        Assert.assertTrue(amqpEventReceiver.isConnected());
        Assert.assertFalse(amqpEventTransport.isConnected());
        Assert.assertNotNull(eventSender);
        eventSender.enqueue(NessEvent.createEvent(null, NessEventType.getForName(null)));
        Thread.sleep(100L);
        Assert.assertTrue(amqpEventTransport.isConnected());

        Assert.assertEquals(1, amqpEventTransport.getEventsTransmittedCount());
        Assert.assertEquals(1, amqpEventReceiver.getEventsReceivedCount());

        lifecycle.executeTo(LifecycleStage.STOP_STAGE);

        Thread.sleep(200L);
    }
}



