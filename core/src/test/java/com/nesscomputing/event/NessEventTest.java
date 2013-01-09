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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NessEventTest
{
	private static final DateTime ENTRY_TIMESTAMP = new DateTime("2011-03-03T14:41:56.279", DateTimeZone.UTC);
	private static final UUID EVENT_ID = UUID.fromString("5ad87404-9c14-4edd-ae52-7c26eb472f03");
    private static final UUID USER = UUID.fromString("00000000-0000-04d2-c000-000000026810");

	private HashMap<String, Object> payload;
    private ObjectMapper mapper;

	@Before
	public void setUp()
	{
        mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Assert.assertNull(payload);

        payload = new HashMap<String, Object>();
		payload.put("udid", "wizbang");
		payload.put("searchStr", "mac and cheese");
	}

	@After
	public void tearDown()
	{
	    Assert.assertNotNull(mapper);
        mapper = null;

        Assert.assertNotNull(payload);
        payload = null;
	}

	@Test
	public void testSerialize() throws IOException
	{
		NessEvent e = NessEvent.createEvent(USER, ENTRY_TIMESTAMP, EVENT_ID, NessEventTypes.SEARCH, payload);

		String serialized = mapper.writeValueAsString(e);
		Assert.assertEquals(mapper.readValue(loadJson("/serializedEvent.json"), TreeMap.class), mapper.readValue(serialized, TreeMap.class));
	}

	@Test
	public void testDeserialize() throws IOException
	{
		NessEvent event = mapper.readValue(loadJson("/serializedEvent.json"), NessEvent.class);

		Assert.assertEquals(payload, event.getPayload());
		Assert.assertEquals(EVENT_ID, event.getId());
		Assert.assertEquals(ENTRY_TIMESTAMP, event.getTimestamp());
		Assert.assertEquals(NessEventTypes.SEARCH, event.getType());
		Assert.assertEquals(USER, event.getUser());
	}

	@Test
	public void testFuckImmutability() throws Exception
	{
	    final Map<String, Object> nullPayLoad = Collections.<String, Object>singletonMap("test", null);
        String serialized = mapper.writeValueAsString(NessEvent.createEvent(null, NessEventType.getForName(null), nullPayLoad));
        Assert.assertTrue(StringUtils.contains(serialized, "\"test\":null"));
	}

	private String loadJson(String path) throws IOException
	{
		return IOUtils.toString(this.getClass().getResourceAsStream(path), Charsets.UTF_8);
	}
}
