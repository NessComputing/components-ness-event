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

import com.nesscomputing.logging.Log;

import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Accepts arbitrary events and dispatches them to the event system.
 */
@Singleton
class InternalEventDispatcher implements NessEventDispatcher
{
    private static final Log LOG = Log.findLog();

    private final Set<NessEventReceiver> eventReceivers = Sets.newHashSet();

    @Inject(optional=true)
    void injectEventReceivers(@Named(EVENT_NAME) final Set<NessEventReceiver> eventReceivers)
    {
        this.eventReceivers.addAll(eventReceivers);
    }

    /**
     * Dispatch an event for distribution to the local Event receivers.
     */
    @Override
    public void dispatch(@Nonnull final NessEvent event)
    {
        if (event == null) {
            LOG.trace("Dropping null event");
        }
        else {

            for (final NessEventReceiver receiver : eventReceivers) {
                if (receiver.accept(event)) {
                    receiver.receive(event);
                }
            }
        }
    }
}
