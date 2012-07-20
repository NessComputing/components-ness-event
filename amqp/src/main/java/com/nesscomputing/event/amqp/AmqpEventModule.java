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


import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.nesscomputing.amqp.AmqpModule;
import com.nesscomputing.config.Config;
import com.nesscomputing.event.NessEventModule;

public class AmqpEventModule extends AbstractModule
{
    public static final String AMQP_EVENT_NAME = "amqp-event";

    public static final Named AMQP_EVENT_NAMED = Names.named(AMQP_EVENT_NAME);

    private final Config config;

    public AmqpEventModule(final Config config)
    {
        this.config = config;
    }

    @Override
    public void configure()
    {
        final AmqpEventConfig amqpEventConfig = config.getBean(AmqpEventConfig.class);
        bind(AmqpEventConfig.class).toInstance(amqpEventConfig);

        if (amqpEventConfig.isEnabled()) {
            install (new AmqpModule(config, AMQP_EVENT_NAME));

            if (amqpEventConfig.isListenEnabled()) {
                bind(AmqpEventReceiver.class).asEagerSingleton();
            }
            if (amqpEventConfig.isTransmitEnabled()) {
                bind(AmqpEventTransmitter.class).in(Scopes.SINGLETON);
                NessEventModule.bindEventTransmitter(binder(), "amqp").to(AmqpEventTransmitter.class).in(Scopes.SINGLETON);
            }
        }
    }
}
