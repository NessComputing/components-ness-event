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

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.nesscomputing.event.NessEventType;

public class TestNessEventType
{
    public static final UUID USER_ID = UUID.randomUUID();
    public static final DateTime TIMESTAMP = new DateTime(DateTimeZone.UTC);
    public static final UUID ID = UUID.randomUUID();
    public static final NessEventType TYPE = NessEventType.getForName("TestNessEvent");
    public static final ImmutableMap<String, String> PAYLOAD = ImmutableMap.of("hello", "world");

    @Test
    public void testSimple()
    {
        final NessEventType eventType = new NessEventType("FOO");
        Assert.assertEquals("FOO", eventType.getName());
    }

    @Test
    public void testToUpper()
    {
        final NessEventType eventType = new NessEventType("foo");
        Assert.assertEquals("FOO", eventType.getName());
    }

    @Test
    public void testEquality()
    {
        final NessEventType eventType1 = new NessEventType("foo");
        final NessEventType eventType2 = new NessEventType("FOO");
        Assert.assertTrue(eventType1.equals(eventType2));
    }

    @Test(expected=IllegalArgumentException.class)
    @SuppressWarnings({"NP_NONNULL_PARAM_VIOLATION"})
    public void testNullCtor()
    {
        new NessEventType(null);
    }

    @Test
    public void testSimpleFactory()
    {
        final NessEventType eventType = NessEventType.getForName("FOO");
        Assert.assertEquals("FOO", eventType.getName());
    }

    @Test
    public void testToUpperFactory()
    {
        final NessEventType eventType = NessEventType.getForName("foo");
        Assert.assertEquals("FOO", eventType.getName());
    }

    @Test
    public void testEqualityFactory()
    {
        final NessEventType eventType1 = NessEventType.getForName("foo");
        final NessEventType eventType2 = NessEventType.getForName("FOO");
        Assert.assertTrue(eventType1.equals(eventType2));
    }

    public void testNullFactory()
    {
        final NessEventType eventType = NessEventType.getForName(null);
        Assert.assertEquals("", eventType.getName());
    }
}
