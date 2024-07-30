package com.scalefocus.mile.jms.auth.poc.consumer;

import jakarta.jms.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

class MessageConsumerServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerServiceTest.class);

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Session session;

    @Mock
    private Queue queue;

    @Mock
    private MessageConsumer consumer;

    private MessageConsumerService messageConsumerService;
    private final String solaceQueue = "testQueue";

    @BeforeEach
    void setUp() throws JMSException {
        MockitoAnnotations.openMocks(this);

        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
        when(session.createQueue(anyString())).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(consumer);

        messageConsumerService = new MessageConsumerService(connectionFactory, solaceQueue);
        messageConsumerService.establishBrokerConnection();
    }

    @Test
    void testEstablishBrokerConnection() throws JMSException {
        verify(connectionFactory, times(1)).createConnection();
        verify(connection, times(1)).createSession(false, Session.AUTO_ACKNOWLEDGE);
        verify(connection, times(1)).start();
    }

    @Test
    void testThreadSafety() throws JMSException {
        Thread[] threads = new Thread[4];

        for (int i = 0; i < threads.length; i++) {
            final int ti = i;

            threads[i] = new Thread(() -> {
                try {
                    messageConsumerService.createConsumer("queue" + ti);
                } catch (JMSException e) {
                    logger.error("Error creating consumer [{}]", ti, e);
                }
            });

            threads[i].start();

            try {
                threads[i].join();
            } catch (InterruptedException e) {
                logger.error("Current thread interrupted", e);
            }
        }

        // Verify that createConsumer was called twice, once for each queue
        verify(session, times(threads.length)).createQueue(anyString());
        verify(session, times(threads.length)).createConsumer(queue);
    }

    @Test
    void testResourceClosure() throws JMSException {
        messageConsumerService.cleanup();

        verify(session, times(1)).close();
        verify(connection, times(1)).close();
    }
}
