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

import org.skife.config.Config;
import org.skife.config.Default;

import com.rabbitmq.client.ConnectionFactory;

public abstract class RMQEventConfig
{
    /**
     * Global enable / disable.
     */
    @Config("ness.event.rabbitmq.enabled")
    @Default("false")
    public boolean isEnabled()
    {
        return false;
    }

    /**
     * Listen for events on RabbitMQ.
     */
    @Config("ness.event.rabbitmq.listen-enabled")
    @Default("true")
    public boolean isListenEnabled()
    {
        return true;
    }

    /**
     * Send events to RabbitMQ.
     */
    @Config("ness.event.rabbitmq.transmit-enabled")
    @Default("true")
    public boolean isTransmitEnabled()
    {
        return true;
    }

    /** Hostname for RabbitMQ */
    @Config("ness.event.rabbitmq.host")
    @Default(ConnectionFactory.DEFAULT_HOST)
    public String getHost() {
        return ConnectionFactory.DEFAULT_HOST;
    }

    /** Port for RabbitMQ */
    @Config("ness.event.rabbitmq.port")
    @Default("5672")
    public int getPort() {
        return ConnectionFactory.DEFAULT_AMQP_PORT;
    }

    /** RabbitMQ username */
    @Config("ness.event.rabbitmq.user")
    @Default(ConnectionFactory.DEFAULT_USER)
    public String getUser() {
        return ConnectionFactory.DEFAULT_USER;
    }

    /** RabbitMQ password */
    @Config("ness.event.rabbitmq.pass")
    @Default(ConnectionFactory.DEFAULT_PASS)
    public String getPass() {
        return ConnectionFactory.DEFAULT_PASS;
    }

    /** RabbitMQ VHost */
    @Config("ness.event.rabbitmq.vhost")
    @Default(ConnectionFactory.DEFAULT_VHOST)
    public String getVHost() {
        return ConnectionFactory.DEFAULT_VHOST;
    }

    /** RabbitMQ Queue to pull messages from */
    @Config("ness.event.rabbitmq.queue.name")
    @Default("logging")
    public String getQueueName() {
        return "logging";
    }

    /** Create a durable queue? */
    @Config("ness.event.rabbitmq.queue.durable")
    @Default("true")
    public boolean isQueueDurable() {
        return true;
    }

    /** Create an exclusive queue (only readable by us)? */
    @Config("ness.event.rabbitmq.queue.exclusive")
    @Default("false")
    public boolean isQueueExclusive() {
        return false;
    }

    /** Automatically delete the queue when we disconnect? */
    @Config("ness.event.rabbitmq.queue.auto.delete")
    @Default("false")
    public boolean isAutoDeleteQueue() {
        return false;
    }

    /** Routing key to listen for. */
    @Config("ness.event.rabbitmq.queue.routing.key")
    @Default("default")
    public String getRoutingKey() {
        return "default";
    }

    /** Exchange to listen on. */
    @Config("ness.event.rabbitmq.queue.exchange.name")
    @Default("default")
    public String getExchangeName() {
        return "default";
    }
}

