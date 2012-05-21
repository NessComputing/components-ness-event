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


import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.nesscomputing.config.Config;
import com.nesscomputing.event.NessEventModule;
import com.nesscomputing.jms.JmsModule;

public class JmsEventModule extends AbstractModule
{
    public static final String JMS_EVENT_NAME = "jms-event";

    public static final Named JMS_EVENT_NAMED = Names.named(JMS_EVENT_NAME);

    private final Config config;

    public JmsEventModule(final Config config)
    {
        this.config = config;
    }

    @Override
    public void configure()
    {
        final JmsEventConfig jmsEventConfig = config.getBean(JmsEventConfig.class);
        bind(JmsEventConfig.class).toInstance(jmsEventConfig);

        if (jmsEventConfig.isEnabled()) {
            install (new JmsModule(config, JMS_EVENT_NAME));

            if (jmsEventConfig.isListenEnabled()) {
                bind(JmsEventReceiver.class).asEagerSingleton();
            }
            if (jmsEventConfig.isTransmitEnabled()) {
                bind(JmsEventTransmitter.class).in(Scopes.SINGLETON);
                NessEventModule.bindEventTransmitter(binder(), "jms").to(JmsEventTransmitter.class).in(Scopes.SINGLETON);
            }
        }
    }
}
