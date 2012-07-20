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
package com.nesscomputing.event.amqp.util;

import java.util.concurrent.atomic.AtomicInteger;


import org.junit.Ignore;

import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventReceiver;
import com.nesscomputing.event.NessEventType;

@Ignore
public class CountingEventReceiver implements NessEventReceiver
{
    private NessEvent event = null;
    private AtomicInteger count = new AtomicInteger();

    private final NessEventType eventType;

    public CountingEventReceiver(final NessEventType eventType)
    {
        this.eventType = eventType;
    }

    @Override
    public boolean accept(final NessEvent event)
    {
        return event != null && eventType.equals(event.getType());
    }

    public NessEvent getEvent()
    {
        return event;
    }

    public int getCount()
    {
        return count.get();
    }

    @Override
    public void receive(NessEvent event)
    {
        this.event = event;
        count.incrementAndGet();
    }
}
