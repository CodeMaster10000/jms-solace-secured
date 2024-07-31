package com.scalefocus.mile.jms.auth.poc.consumer;

import jakarta.jms.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

class MessageConsumerServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerServiceTest.class);

    private AutoCloseable openedMocks = null;

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

    @BeforeEach
    void setUp() throws JMSException {
        openedMocks = MockitoAnnotations.openMocks(this);

        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
        when(session.createQueue(anyString())).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(consumer);

        messageConsumerService = new MessageConsumerService(connectionFactory, "demo-queue");
        messageConsumerService.establishBrokerConnection();
    }

    @AfterEach
    void tearDown() {
        messageConsumerService.cleanup();
        try {
            openedMocks.close();
        } catch (Exception ignored) {}
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
        MessageConsumer[] consumers = new MessageConsumer[threads.length];

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try (MessageConsumer messageConsumer = messageConsumerService.createConsumer()){
                    consumers[index] = messageConsumer;
                } catch (JMSException e) {
                    logger.error("Error creating consumer", e);
                }
            });

            threads[i].start();

            try {
                threads[i].join();
            } catch (InterruptedException e) {
                logger.error("Current thread interrupted", e);
            } finally {
                for (MessageConsumer c : consumers) {
                    if (c != null) c.close();
                }
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
