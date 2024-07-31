package com.scalefocus.mile.jms.auth.poc.consumer;

import com.scalefocus.mile.jms.auth.poc.core.ThreadSafe;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.*;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@Unremovable
@Startup
@ThreadSafe
final class MessageConsumerProvider implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerProvider.class);

    private final ConnectionFactory connectionFactory;

    @Getter
    private final String queueUrl;

    private final AtomicReference<Connection> connection = new AtomicReference<>();

    @Getter
    private Session session;

    @Getter
    private ScheduledExecutorService scheduler;

    @Setter
    private CountDownLatch latch;

    MessageConsumerProvider(
            ConnectionFactory connectionFactory,
            @ConfigProperty(name = "solace.queue.data") String solaceQueue) {
        this.connectionFactory = connectionFactory;
        this.queueUrl = solaceQueue;
        initialize();
    }

    @PostConstruct
    void initialize() {
        try {
            establishBrokerConnection();
            scheduleConnectionValidation();
        } catch (JMSException e) {
            logger.error("Error initializing JMS consumer: {}", e.getMessage());
        }
    }

    void establishBrokerConnection() throws JMSException {
        if (connection.get() == null) {
            synchronized (this) {
                if (connection.get() == null) {
                    Connection newConnection = null;
                    try {
                        newConnection = connectionFactory.createConnection();
                        session = newConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        newConnection.start();
                        connection.set(newConnection);
                    } catch (JMSException e) {
                        if (newConnection != null) {
                            newConnection.close();
                        }
                        throw e;
                    }
                }
            }
        }
    }

    MessageConsumer createConsumer() throws JMSException {
        Queue queue = session.createQueue(queueUrl);
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(this);
        return consumer;
    }

    private synchronized void scheduleConnectionValidation() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::validateConnection, 5, 5, TimeUnit.MINUTES);
    }

    private void validateConnection() {
        try {
            synchronized (this) {
                if (connection.get() == null || session == null) {
                    logger.warn("JMS connection/session is null or inactive. Reinitializing...");
                    cleanup();
                    establishBrokerConnection();
                }
            }
        } catch (JMSException e) {
            logger.error("Error validating/reinitializing JMS connection/session", e);
            synchronized (this) {
                cleanup();
            }
            try {
                establishBrokerConnection();
            } catch (JMSException ex) {
                logger.error("Error reinitializing JMS connection/session", ex);
            }
        }
    }

    @PreDestroy
    synchronized void cleanup() {
        try {
            if (session != null) {
                session.close();
            }

            Connection c = connection.get();
            if (c != null) {
                c.close();
            }

            if (scheduler != null) {
                scheduler.shutdown();
            }

        } catch (JMSException e) {
            logger.error("Error closing JMS resources", e);
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                String messageContent = textMessage.getText();
                logger.info("Received message: {}", messageContent);
            } else {
                logger.warn("Received non-text message");
            }
        } catch (JMSException e) {
            logger.error("Error processing JMS message", e);
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }
    }

}
