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
package com.nesscomputing.event;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventDispatcher;
import com.nesscomputing.event.NessEventModule;
import com.nesscomputing.event.NessEventSender;
import com.nesscomputing.event.NessEventTransmitter;
import com.nesscomputing.event.NessEventType;
import com.nesscomputing.event.util.CountingEventReceiver;

public class NessEventTransmitterTest
{
    private static final NessEventType TEST_EVENT_TYPE = NessEventType.getForName("TEST_EVENT");
    private static final UUID USER = UUID.randomUUID();
    private static final NessEvent TEST_EVENT = NessEvent.createEvent(USER, TEST_EVENT_TYPE);

    @Inject
    private NessEventDispatcher dispatcher;

    @Inject
    private NessEventSender sender;

    @Inject
    private TestEventTransmitter testEventTransmitter;

    @Before
    public void setUp()
    {
        Assert.assertNull(dispatcher);
        Assert.assertNull(sender);
        Assert.assertNull(testEventTransmitter);
    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(sender);
        sender = null;

        Assert.assertNotNull(dispatcher);
        dispatcher = null;

        Assert.assertNotNull(testEventTransmitter);
        testEventTransmitter = null;
    }

    @Test
    public void testLocalTransport()
    {
        final CountingEventReceiver testEventReceiver = new CountingEventReceiver(TEST_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting("ness.event.transport", "local"),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               binder.bind(TestEventTransmitter.class).in(Scopes.SINGLETON);
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver);
                                                               NessEventModule.bindEventTransmitter(binder, "test").to(TestEventTransmitter.class).in(Scopes.SINGLETON);
                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        final int maxCount = 1000;
        for (int i = 0; i < maxCount; i++) {
            sender.enqueue(TEST_EVENT);
        }

        final NessEvent testEvent = testEventReceiver.getEvent();
        Assert.assertNotNull(testEvent);
        Assert.assertSame(TEST_EVENT, testEvent);
        Assert.assertEquals(maxCount, testEventReceiver.getCount());

        Assert.assertEquals(0, testEventTransmitter.getCount());
    }

    @Test
    public void testCustomTransport()
    {
        final CountingEventReceiver testEventReceiver = new CountingEventReceiver(TEST_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting("ness.event.transport", "test"),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               binder.bind(TestEventTransmitter.class).in(Scopes.SINGLETON);
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver);
                                                               NessEventModule.bindEventTransmitter(binder, "test").to(TestEventTransmitter.class).in(Scopes.SINGLETON);
                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        final int maxCount = 1000;
        for (int i = 0; i < maxCount; i++) {
            sender.enqueue(TEST_EVENT);
        }

        final NessEvent testEvent = testEventReceiver.getEvent();
        Assert.assertNotNull(testEvent);
        Assert.assertSame(TEST_EVENT, testEvent);
        Assert.assertEquals(maxCount, testEventReceiver.getCount());

        Assert.assertEquals(maxCount, testEventTransmitter.getCount());
    }

    @Test
    public void testTwoTransports()
    {
        final CountingEventReceiver testEventReceiver = new CountingEventReceiver(TEST_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting("ness.event.transport", "test\\, local"),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               binder.bind(TestEventTransmitter.class).in(Scopes.SINGLETON);
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver);
                                                               NessEventModule.bindEventTransmitter(binder, "test").to(TestEventTransmitter.class).in(Scopes.SINGLETON);
                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        final int maxCount = 1000;
        for (int i = 0; i < maxCount; i++) {
            sender.enqueue(TEST_EVENT);
        }

        final NessEvent testEvent = testEventReceiver.getEvent();
        Assert.assertNotNull(testEvent);
        Assert.assertSame(TEST_EVENT, testEvent);
        // Each event was transported twice.
        Assert.assertEquals(maxCount * 2, testEventReceiver.getCount());
        // But only once by the test transport.
        Assert.assertEquals(maxCount, testEventTransmitter.getCount());
    }


    public static class TestEventTransmitter implements NessEventTransmitter
    {
        private final NessEventDispatcher eventDispatcher;
        private final AtomicInteger count = new AtomicInteger();

        @Inject
        TestEventTransmitter(final NessEventDispatcher eventDispatcher)
        {
            this.eventDispatcher = eventDispatcher;
        }

        @Override
        public void transmit(final NessEvent event)
        {
            eventDispatcher.dispatch(event);
            count.incrementAndGet();
        }

        public int getCount()
        {
            return count.get();
        }
    }
}
