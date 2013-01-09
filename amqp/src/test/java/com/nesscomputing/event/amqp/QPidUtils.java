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
package com.nesscomputing.event.amqp;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;

import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;

import com.google.common.base.Throwables;
import com.google.common.io.Resources;

/**
 * Starts and stops a local QPid for testing.
 */
public class QPidUtils
{
    private Broker b;
    private int port = 0;

    public void startup() throws Exception
    {
        final URL configUrl = Resources.getResource(QPidUtils.class, "/qpid/config.xml");
        final File configFile = new File(configUrl.toURI());
        BrokerOptions options = new BrokerOptions();
        options.setConfigFile(configFile.getAbsolutePath());

        // XXX: this requires a log4j file to not be in a JAR, which sucks.

//        final URL log4jUrl = Resources.getResource(QPidUtils.class, "/log4j.xml");
//        final File log4jFile = new File(log4jUrl.toURI());
//        options.setLogConfigFile(log4jFile.getAbsolutePath());

        port = findUnusedPort();
        options.addPort(port);

        System.setProperty("QPID_HOME", configFile.getParentFile().getAbsolutePath());

        b = new Broker();
        b.startup(options);
    }

    public String getUri()
    {
        return String.format("amqp://localhost:%d", port);
    }

    public void shutdown()
    {
        b.shutdown();
    }

    private static final int findUnusedPort()
    {
        int port;

        ServerSocket socket = null;
        try {
            socket = new ServerSocket();
            socket.bind(new InetSocketAddress(0));
            port = socket.getLocalPort();
        }
        catch (IOException ioe) {
            throw Throwables.propagate(ioe);
        }
        finally {
            try {
                socket.close();
            } catch (IOException ioe) {
                // GNDN
            }
        }

        return port;
    }
}
