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

import static com.nesscomputing.event.NessEventModule.EVENT_NAME;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.nesscomputing.logging.Log;

@Singleton
public class NessEventSender
{
    private static final Log LOG = Log.findLog();

    private final NessEventConfig eventConfig;

    private final Set<NessEventTransmitter> eventTransmitters = Sets.newHashSet();

    @Inject
    public NessEventSender(@Nullable final NessEventConfig eventConfig)
    {
        this.eventConfig = eventConfig;
    }

    @Inject(optional=true)
    void injectTransmitters(@Named(EVENT_NAME) final Map<String, NessEventTransmitter> availableTransmitters)
    {
        if (eventConfig != null) {
            final String [] transports = eventConfig.getTransports();

            for (int i = 0; i < transports.length; i++) {
                if (availableTransmitters.containsKey(transports[i])) {
                    addEventTransmitter(availableTransmitters.get(transports[i]));
                    LOG.trace("Added %s as an event transport.", transports[i]);
                }
                else {
                    LOG.warn("Event transport %s configured but not available.", transports[i]);
                }
            }
        }
        else {
            LOG.warn("Event config is null but transports were injected!");
        }
    }

    /**
     * Add a new transmitter to this sender.
     */
    public void addEventTransmitter(final NessEventTransmitter eventTransmitter)
    {
        this.eventTransmitters.add(eventTransmitter);
    }

    /**
     * Enqueue an event into the messaging system.
     */
    public void enqueue(@Nonnull NessEvent event)
    {
        for (NessEventTransmitter transport : eventTransmitters) {
            transport.transmit(event);
        }
    }
}
