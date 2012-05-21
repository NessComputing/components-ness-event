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
package com.nesscomputing.event.jms;

import javax.annotation.Nonnull;


import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.event.NessEvent;
import com.nesscomputing.event.NessEventDispatcher;
import com.nesscomputing.event.jms.JmsEventReceiver;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.logging.Log;

/**
 *
 */
public class TestJmsEventReceiver
{
    private static final Log LOG = Log.findLog();
    private JmsEventReceiver eventReceiver;

    @Before
    public void setUp() throws Exception
    {
        Injector injector = Guice.createInjector(new ConfigModule(Config.getEmptyConfig()), new NessJacksonModule());
        NessEventDispatcher eventDispatcherStub = new NessEventDispatcher()
        {
            @Override
            public void dispatch(@Nonnull NessEvent event)
            {
                LOG.debug("Event: %s", event);
            }
        };

        eventReceiver = new JmsEventReceiver(
            null, eventDispatcherStub, injector.getInstance(ObjectMapper.class)
        );
    }

    @Test
    public void testWithMethod() throws Exception
    {
        String problemJson =  "{\"user\":\"00000000-02bb-cb0b-c000-000000026810\",\"timestamp\":1327531243690,\"id\":\"31ab3710-0741-40a3-8e04-12cfb8073e9e\",\"type\":\"LOCATION_ACCURACY\",\"payload\":{\"timestamp\":1327530971278,\"fixes\":[{\"lat\":43.60727603742065,\"accuracy\":99.9153705888414,\"timestamp\":1327530971220,\"lon\":-83.85103888690809}],\"eventType\":\"LOCATION_ACCURACY\",\"desiredAccuracy\":500},\"v\":2}";

        eventReceiver.withMessage(problemJson);

    }

}
