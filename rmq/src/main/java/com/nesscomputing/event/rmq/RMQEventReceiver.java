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
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventDispatcher;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;


/** Receives an Event from the RMQ message queue and dispatches it to the event system. */
@Singleton
public class RMQEventReceiver
{
    private static final Log LOG = Log.findLog();

    private final RMQEventConfig rmqEventConfig;
    private Channel channel;
    private final NessEventDispatcher dispatcher;
    private final ObjectMapper mapper;

    @Inject
    RMQEventReceiver(
        final RMQEventConfig rmqEventConfig,
        final Channel channel,
        final NessEventDispatcher dispatcher,
        final ObjectMapper mapper
    )
    {
        this.channel = channel;
        this.dispatcher = dispatcher;
        this.mapper = mapper;
        this.rmqEventConfig = rmqEventConfig;
    }


    @OnStage(LifecycleStage.START)
    void start() throws IOException
    {
        channel.exchangeDeclare(rmqEventConfig.getExchangeName(), "direct", rmqEventConfig.isQueueDurable());
        String queueName = channel.queueDeclare().getQueue();
        LOG.info("Queue Name: %s", queueName);
        channel.queueBind(queueName, rmqEventConfig.getExchangeName(), rmqEventConfig.getRoutingKey());
        RMQConsumer consumer = new RMQConsumer(channel, dispatcher, mapper);
        channel.basicConsume(queueName, rmqEventConfig.isAutoDeleteQueue(), consumer);
    }

    @OnStage(LifecycleStage.STOP)
    void stop() throws IOException
    {
        channel.close();
    }

    private static class RMQConsumer extends DefaultConsumer
    {
        private ObjectMapper mapper;
        private NessEventDispatcher dispatcher;

        /**
         * Constructs a new instance and records its association to the passed-in channel.
         *
         * @param channel the channel to which this consumer is attached
         */
        public RMQConsumer(Channel channel, NessEventDispatcher dispatcher, ObjectMapper mapper)
        {
            super(channel);
            this.dispatcher = dispatcher;
            this.mapper = mapper;
            LOG.trace("Creating Consumer");
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException
        {
            LOG.trace("Received Body: %s", new String(body, Charsets.UTF_8));
            NessEvent event = mapper.readValue(body, NessEvent.class);
            LOG.trace("Received Event: %s", event);
            dispatcher.dispatch(event);
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
    }
}
