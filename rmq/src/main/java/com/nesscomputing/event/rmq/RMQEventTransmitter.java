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

import javax.annotation.Nonnull;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventTransmitter;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.OnStage;
import com.nesscomputing.logging.Log;
import com.rabbitmq.client.Channel;

/** Transmits an event onto a RMQ topic. */
@Singleton
public class RMQEventTransmitter implements NessEventTransmitter
{
    private static final Log LOG = Log.findLog();

    private RMQEventConfig rmqEventConfig;
    private Channel channel;
    private ObjectMapper mapper;

    @Inject
    RMQEventTransmitter(RMQEventConfig rmqEventConfig, Channel channel, ObjectMapper mapper)
    {
        this.rmqEventConfig = rmqEventConfig;
        this.channel = channel;
        this.mapper = mapper;
    }

    @OnStage(LifecycleStage.START)
    void start()
    {
    }

    @OnStage(LifecycleStage.STOP)
    void stop() throws IOException
    {
        channel.close();
    }


    @Override
    public void transmit(@Nonnull final NessEvent event)
    {
        Preconditions.checkArgument(event != null, "An event can not be null!");

        LOG.trace("Transmitting event: %s", event);

        try {
            channel.basicPublish(
                rmqEventConfig.getExchangeName(),
                rmqEventConfig.getRoutingKey(),
                null,
                mapper.writeValueAsBytes(event)
            );
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
