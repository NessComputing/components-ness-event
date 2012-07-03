package com.nesscomputing.event.rmq;

import java.io.IOException;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.nesscomputing.config.Config;
import com.nesscomputing.logging.Log;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.AMQImpl;
import com.rabbitmq.client.impl.MethodArgumentReader;

/**
 *
 */
public class RMQEventModuleTest
{
    private static final Log LOG = Log.findLog();

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;

    @Inject(optional = true)
    public void setConnectionFactory(ConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    @Inject(optional = true)
    public void setConnection(Connection connection)
    {

        this.connection = connection;
    }

    @Inject(optional = true)
    public void setChannel(Channel channel)
    {
        this.channel = channel;
    }

    @Test
    public void testModule() throws IOException {
        ConnectionFactory mockConnectionFactory = EasyMock.createMock(ConnectionFactory.class);
        Connection mockConnection = EasyMock.createMock(Connection.class);
        Channel mockChannel = EasyMock.createMock(Channel.class);

        EasyMock.expect(mockConnectionFactory.newConnection()).andReturn(mockConnection).once();
        EasyMock.expect(mockConnection.createChannel()).andReturn(mockChannel);

        mockConnectionFactory.setHost(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().anyTimes();

        mockConnectionFactory.setPassword(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().anyTimes();
        mockConnectionFactory.setUsername(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().anyTimes();
        mockConnectionFactory.setPort(EasyMock.anyInt());
        EasyMock.expectLastCall().anyTimes();
        mockConnectionFactory.setVirtualHost(EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().anyTimes();

        Capture<Map<String, Object>> capture = new Capture<Map<String, Object>>();
        EasyMock.expect(
            mockChannel.queueDeclare(
                EasyMock.anyObject(String.class),
                EasyMock.anyBoolean(),
                EasyMock.anyBoolean(),
                EasyMock.anyBoolean(),
                EasyMock.capture(capture)
            )
        ).andStubReturn(null);

        EasyMock.replay(mockConnectionFactory, mockConnection, mockChannel);

        Injector injector = Guice.createInjector(new RMQEventModule(Config.getEmptyConfig(), mockConnectionFactory));
        injector.injectMembers(this);

        Assert.assertNotNull(connectionFactory);
        Assert.assertNotNull(connection);
        Assert.assertNotNull(channel);
//        Assert.assertNull(capture.getValue());
    }
}
