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
package com.nesscomputing.event.server;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Injector;
import com.nesscomputing.config.Config;
import com.nesscomputing.event.server.NessEventResource;
import com.nesscomputing.event.server.NessEventServerMain;
import com.nesscomputing.testing.lessio.AllowNetworkListen;

@AllowNetworkListen(ports={0})
public class TestNessEventServerMain
{
    @Test
    public void testSpinup()
    {
        final NessEventServerMain server = new NessEventServerMain() {
            @Override
            public Config getConfig() {
                return Config.getConfig("classpath:/test-config", "event");
            }
        };

        final Injector injector = server.getInjector();

        final NessEventResource config = injector.getInstance(NessEventResource.class);
        Assert.assertNotNull(config);
    }
}
