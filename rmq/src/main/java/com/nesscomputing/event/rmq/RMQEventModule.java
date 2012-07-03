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
package com.nesscomputing.event.rmq;

import java.io.IOException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.nesscomputing.config.Config;
import com.nesscomputing.event.NessEventModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 *
 */
public class RMQEventModule extends AbstractModule
{
    private final Config config;
    private ConnectionFactory connectionFactory;

    public RMQEventModule(final Config config)
    {
        this.config = config;
        this.connectionFactory = new ConnectionFactory();
    }

    @VisibleForTesting
    RMQEventModule(final Config config, ConnectionFactory connectionFactory)
    {
        this.config = config;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void configure()
    {
        final RMQEventConfig rmqEventConfig = config.getBean(RMQEventConfig.class);
        bind(RMQEventConfig.class).toInstance(rmqEventConfig);

        if (rmqEventConfig.isEnabled()) {
            if (rmqEventConfig.isListenEnabled()) {
                bind(RMQEventReceiver.class).asEagerSingleton();
            }

            if (rmqEventConfig.isTransmitEnabled()) {
                bind(RMQEventTransmitter.class).in(Scopes.SINGLETON);
                NessEventModule.bindEventTransmitter(binder(), "rmq").to(RMQEventTransmitter.class).in(Scopes.SINGLETON);
            }
        }
    }

    @Provides
    @Singleton
    public ConnectionFactory provideConnectionFactory(RMQEventConfig config)
    {
        connectionFactory.setHost(config.getHost());
        connectionFactory.setPort(config.getPort());
        connectionFactory.setVirtualHost(config.getVHost());

        connectionFactory.setUsername(config.getUser());
        connectionFactory.setPassword(config.getPass());

        return connectionFactory;
    }

    @Provides
    @Singleton
    public Connection provideConnection(ConnectionFactory factory) {
        try {
            return factory.newConnection();
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Provides
    public Channel provideChannel(Connection connection) {
        Channel channel;
        try {

            channel = connection.createChannel();
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }

        return channel;
    }
}
