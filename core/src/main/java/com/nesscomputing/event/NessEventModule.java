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

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.nesscomputing.config.ConfigProvider;

public class NessEventModule extends AbstractModule
{
    public static final String EVENT_NAME = "eventModule";
    public static final Named EVENT_NAMED = Names.named(EVENT_NAME);

    @Override
    public void configure()
    {
        bind(NessEventConfig.class).toProvider(ConfigProvider.of(NessEventConfig.class)).in(Scopes.SINGLETON);

        bind(NessEventDispatcher.class).to(InternalEventDispatcher.class).in(Scopes.SINGLETON);
        bind(NessEventSender.class).in(Scopes.SINGLETON);

        bind(InternalEventDispatcher.class).in(Scopes.SINGLETON);
        bind(InternalLocalEventTransmitter.class).in(Scopes.SINGLETON);
        NessEventModule.bindEventTransmitter(binder(), "local").to(InternalLocalEventTransmitter.class).in(Scopes.SINGLETON);
    }

    public static LinkedBindingBuilder<NessEventReceiver> bindEventReceiver(final Binder binder)
    {
        final Multibinder<NessEventReceiver> eventReceivers = Multibinder.newSetBinder(binder, NessEventReceiver.class, EVENT_NAMED);
        return eventReceivers.addBinding();
    }

    public static LinkedBindingBuilder<NessEventTransmitter> bindEventTransmitter(final Binder binder, final String key)
    {
        final MapBinder<String, NessEventTransmitter> eventTransmitters = MapBinder.newMapBinder(binder, String.class, NessEventTransmitter.class, EVENT_NAMED);
        return eventTransmitters.addBinding(key);
    }
}
