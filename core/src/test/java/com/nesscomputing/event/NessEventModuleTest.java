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


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventModule;
import com.nesscomputing.event.NessEventSender;
import com.nesscomputing.event.NessEventType;
import com.nesscomputing.event.util.CountingEventReceiver;

public class NessEventModuleTest
{
    private static final NessEventType TEST_EVENT_TYPE = NessEventType.getForName("TEST_EVENT");
    private static final UUID USER = UUID.randomUUID();
    private static final NessEvent TEST_EVENT = NessEvent.createEvent(USER, TEST_EVENT_TYPE);

    private static final NessEventType IGNORE_EVENT_TYPE = NessEventType.getForName("IGNORE_EVENT");
    private static final NessEvent IGNORE_EVENT = NessEvent.createEvent(USER, IGNORE_EVENT_TYPE);

    @Inject
    private NessEventSender sender;

    @Before
    public void setUp()
    {
        Assert.assertNull(sender);
    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(sender);
        sender = null;
    }

    @Test
    public void testNoEventReceivers()
    {
        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting(),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        for (int i = 0; i < 1000; i++) {
            sender.enqueue(TEST_EVENT);
        }
    }

    @Test
    public void testSingleEventReceiver()
    {
        final CountingEventReceiver testEventReceiver = new CountingEventReceiver(TEST_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting(),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver);
                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        sender.enqueue(TEST_EVENT);

        final NessEvent testEvent = testEventReceiver.getEvent();
        Assert.assertNotNull(testEvent);
        Assert.assertSame(TEST_EVENT, testEvent);
        Assert.assertEquals(1, testEventReceiver.getCount());
    }

    @Test
    public void testTwoEventReceivers()
    {
        final CountingEventReceiver testEventReceiver1 = new CountingEventReceiver(TEST_EVENT_TYPE);
        final CountingEventReceiver testEventReceiver2 = new CountingEventReceiver(TEST_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting(),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver1);
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver2);

                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        sender.enqueue(TEST_EVENT);

        final NessEvent testEvent1 = testEventReceiver1.getEvent();
        Assert.assertNotNull(testEvent1);
        Assert.assertSame(TEST_EVENT, testEvent1);
        Assert.assertEquals(1, testEventReceiver1.getCount());

        final NessEvent testEvent2 = testEventReceiver2.getEvent();
        Assert.assertNotNull(testEvent2);
        Assert.assertSame(TEST_EVENT, testEvent2);
        Assert.assertEquals(1, testEventReceiver2.getCount());
    }

    @Test
    public void testLotsOfEvents()
    {
        final CountingEventReceiver testEventReceiver1 = new CountingEventReceiver(TEST_EVENT_TYPE);
        final CountingEventReceiver testEventReceiver2 = new CountingEventReceiver(TEST_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting(),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver1);
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver2);

                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        int maxCount = 1000;
        for (int i = 0; i < maxCount; i++) {
            sender.enqueue(TEST_EVENT);
        }

        final NessEvent testEvent1 = testEventReceiver1.getEvent();
        Assert.assertNotNull(testEvent1);
        Assert.assertSame(TEST_EVENT, testEvent1);
        Assert.assertEquals(maxCount, testEventReceiver1.getCount());

        final NessEvent testEvent2 = testEventReceiver2.getEvent();
        Assert.assertNotNull(testEvent2);
        Assert.assertSame(TEST_EVENT, testEvent2);
        Assert.assertEquals(maxCount, testEventReceiver2.getCount());
    }


    @Test
    public void testIgnoreEvent()
    {
        final CountingEventReceiver testEventReceiver = new CountingEventReceiver(TEST_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting(),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver);
                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        sender.enqueue(IGNORE_EVENT);

        final NessEvent testEvent = testEventReceiver.getEvent();
        Assert.assertNull(testEvent);
        Assert.assertEquals(0, testEventReceiver.getCount());
    }

    @Test
    public void testOneForYouOneForMe()
    {
        final CountingEventReceiver testEventReceiver1 = new CountingEventReceiver(TEST_EVENT_TYPE);
        final CountingEventReceiver testEventReceiver2 = new CountingEventReceiver(IGNORE_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting(),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver1);
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver2);

                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        sender.enqueue(TEST_EVENT);
        sender.enqueue(IGNORE_EVENT);

        final NessEvent testEvent1 = testEventReceiver1.getEvent();
        Assert.assertNotNull(testEvent1);
        Assert.assertSame(TEST_EVENT, testEvent1);
        Assert.assertEquals(1, testEventReceiver1.getCount());

        final NessEvent testEvent2 = testEventReceiver2.getEvent();
        Assert.assertNotNull(testEvent2);
        Assert.assertSame(IGNORE_EVENT, testEvent2);
        Assert.assertEquals(1, testEventReceiver2.getCount());
    }

    @Test
    public void testHammerHorror() throws Exception
    {
        final CountingEventReceiver testEventReceiver1 = new CountingEventReceiver(TEST_EVENT_TYPE);
        final CountingEventReceiver testEventReceiver2 = new CountingEventReceiver(TEST_EVENT_TYPE);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting(),
                                                       new Module() {

                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver1);
                                                               NessEventModule.bindEventReceiver(binder).toInstance(testEventReceiver2);

                                                               binder.disableCircularProxies();
                                                               binder.requireExplicitBindings();
                                                           }},
                                                       new NessEventModule());

        injector.injectMembers(this);

        final int maxCount = 1000;
        final int threadCount = 50;

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < maxCount; i++) {
                    sender.enqueue(TEST_EVENT);
                }
            }
        };

        final Thread [] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }

        final NessEvent testEvent1 = testEventReceiver1.getEvent();
        Assert.assertNotNull(testEvent1);
        Assert.assertSame(TEST_EVENT, testEvent1);
        Assert.assertEquals(maxCount * threadCount, testEventReceiver1.getCount());

        final NessEvent testEvent2 = testEventReceiver2.getEvent();
        Assert.assertNotNull(testEvent2);
        Assert.assertSame(TEST_EVENT, testEvent2);
        Assert.assertEquals(maxCount * threadCount, testEventReceiver2.getCount());
    }
}

