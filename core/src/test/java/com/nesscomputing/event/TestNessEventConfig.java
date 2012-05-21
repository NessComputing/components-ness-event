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

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.config.ConfigProvider;
import com.nesscomputing.event.NessEventConfig;

public class TestNessEventConfig
{
    @Inject
    public NessEventConfig eventConfig;

    @Test
    public void testSimple()
    {
        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting(),
                                                       new Module() {
                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               binder.bind(NessEventConfig.class).toProvider(ConfigProvider.of(NessEventConfig.class)).in(Scopes.SINGLETON);
                                                           }
                                                       });

        injector.injectMembers(this);

        Assert.assertNotNull(eventConfig);

        final String [] transports = eventConfig.getTransports();

        Assert.assertNotNull(transports);
        Assert.assertEquals(1, transports.length);
        Assert.assertEquals("local", transports[0]);
    }

    @Test
    public void testTwoTransports()
    {
        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting("ness.event.transport", "local, remote"),
                                                       new Module() {
                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               binder.bind(NessEventConfig.class).toProvider(ConfigProvider.of(NessEventConfig.class)).in(Scopes.SINGLETON);
                                                           }
                                                       });

        injector.injectMembers(this);

        Assert.assertNotNull(eventConfig);

        final String [] transports = eventConfig.getTransports();

        Assert.assertNotNull(transports);
        Assert.assertEquals(2, transports.length);
        Assert.assertEquals("local", transports[0]);
        Assert.assertEquals("remote", transports[1]);
    }

    @Test
    public void testEmptyransport()
    {
        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       ConfigModule.forTesting("ness.event.transport", ""),
                                                       new Module() {
                                                           @Override
                                                           public void configure(final Binder binder) {
                                                               binder.bind(NessEventConfig.class).toProvider(ConfigProvider.of(NessEventConfig.class)).in(Scopes.SINGLETON);
                                                           }
                                                       });

        injector.injectMembers(this);

        Assert.assertNotNull(eventConfig);

        final String [] transports = eventConfig.getTransports();

        Assert.assertNotNull(transports);
        Assert.assertEquals(0, transports.length);
    }
}
