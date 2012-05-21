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

import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventType;

public class TestNessEvent
{
    public static final UUID USER_ID = UUID.randomUUID();
    public static final DateTime TIMESTAMP = new DateTime(DateTimeZone.UTC);
    public static final UUID ID = UUID.randomUUID();
    public static final NessEventType TYPE = NessEventType.getForName("TestNessEvent");
    public static final ImmutableMap<String, Object> PAYLOAD = ImmutableMap.<String, Object>of("hello", "world");

    @Test
    public void testOk()
    {
        final NessEvent event = NessEvent.createEvent(USER_ID,
                                                      TIMESTAMP,
                                                      ID,
                                                      TYPE,
                                                      PAYLOAD);

        Assert.assertEquals(USER_ID, event.getUser());
        Assert.assertEquals(TIMESTAMP,  event.getTimestamp());
        Assert.assertEquals(ID, event.getId());
        Assert.assertEquals(TYPE, event.getType());
        Assert.assertEquals(PAYLOAD, event.getPayload());
    }

    @Test
    public void testShortOk()
    {
        final NessEvent event = NessEvent.createEvent(USER_ID,
                                                      TYPE,
                                                      PAYLOAD);

        Assert.assertEquals(USER_ID, event.getUser());
        Assert.assertEquals(TYPE, event.getType());
        Assert.assertEquals(PAYLOAD, event.getPayload());
        Assert.assertNotNull(event.getTimestamp());
        Assert.assertNotNull(event.getId());
    }

    @Test
    public void testShorterOk()
    {
        final NessEvent event = NessEvent.createEvent(USER_ID,
                                                      TYPE);

        Assert.assertEquals(USER_ID, event.getUser());
        Assert.assertEquals(TYPE, event.getType());

        final Map<String, ? extends Object> payload = event.getPayload();
        Assert.assertNotNull(payload);
        Assert.assertEquals(0, payload.size());

        Assert.assertNotNull(event.getTimestamp());
        Assert.assertNotNull(event.getId());
    }

    public void testNullUserOk()
    {
        final NessEvent event = NessEvent.createEvent(null,
                                                      TIMESTAMP,
                                                      ID,
                                                      TYPE,
                                                      PAYLOAD);
        Assert.assertNull(event.getUser());
        Assert.assertEquals(TIMESTAMP,  event.getTimestamp());
        Assert.assertEquals(ID, event.getId());
        Assert.assertEquals(TYPE, event.getType());
        Assert.assertEquals(PAYLOAD, event.getPayload());
    }


    public void testNullPayloadOk()
    {
        final NessEvent event = NessEvent.createEvent(USER_ID,
                                                      TIMESTAMP,
                                                      ID,
                                                      TYPE,
                                                      PAYLOAD);
        Assert.assertEquals(USER_ID, event.getUser());
        Assert.assertEquals(TIMESTAMP,  event.getTimestamp());
        Assert.assertEquals(ID, event.getId());
        Assert.assertEquals(TYPE, event.getType());

        final Map<String, ? extends Object> payload = event.getPayload();
        Assert.assertNotNull(payload);
        Assert.assertEquals(0, payload.size());
    }

    public void testNullTimestampIsOk()
    {
        final NessEvent event = NessEvent.createEvent(null,
                                                      null,
                                                      ID,
                                                      TYPE,
                                                      PAYLOAD);
        Assert.assertNotNull(event);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void testIdRequired()
    {
        NessEvent.createEvent(null,
                              TIMESTAMP,
                              null,
                              TYPE,
                              PAYLOAD);
    }


    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("NP_NONNULL_PARAM_VIOLATION")
    public void testTypeRequired()
    {
        NessEvent.createEvent(null,
                              TIMESTAMP,
                              ID,
                              null,
                              PAYLOAD);
    }
}
