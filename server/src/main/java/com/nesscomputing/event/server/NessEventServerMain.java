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


import com.google.inject.AbstractModule;
import com.google.inject.Module;

import com.nesscomputing.config.Config;
import com.nesscomputing.event.NessEventModule;
import com.nesscomputing.event.jms.JmsEventModule;
import com.nesscomputing.server.StandaloneServer;
import com.nesscomputing.server.templates.BasicDiscoveryServerModule;
import com.nesscomputing.service.discovery.httpserver.DiscoveryStandaloneServer;

public class NessEventServerMain extends DiscoveryStandaloneServer
{
    public static void main(final String [] args)
    {
        final StandaloneServer server = new NessEventServerMain();
        server.startServer();
    }

    @Override
    protected String getServiceName()
    {
        return "event";
    }

    @Override
    protected String getServerType()
    {
        return "event";
    }

    @Override
    public Module getMainModule(final Config config)
    {
        return new AbstractModule() {
            @Override
            public void configure()
            {
                install(new BasicDiscoveryServerModule(config));

                install(new NessEventModule());
                install(new JmsEventModule(config));

                bind(NessEventResource.class);
            }
        };
    }
}

