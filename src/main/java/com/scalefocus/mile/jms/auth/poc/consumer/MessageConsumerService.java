package com.scalefocus.mile.jms.auth.poc.consumer;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Unremovable
@Startup
final class MessageConsumerService implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerService.class);

    private final ConnectionFactory connectionFactory;
    private final String solaceQueue;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private ScheduledExecutorService scheduler;

    MessageConsumerService(
            ConnectionFactory connectionFactory,
            @ConfigProperty(name = "solace.queue.data") String solaceQueue) {
        this.connectionFactory = connectionFactory;
        this.solaceQueue = solaceQueue;
        initialize();
    }

    @PostConstruct
    void initialize() {
        try {
            establishBrokerConnection();
            scheduleConnectionValidation();
        } catch (JMSException e) {
            logger.error("Error initializing JMS consumer {}", e.getMessage());
        }
    }

    private void establishBrokerConnection() throws JMSException {
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(solaceQueue);
        consumer = session.createConsumer(queue);
        consumer.setMessageListener(this);
        connection.start();
    }

    private void scheduleConnectionValidation() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::validateConnection, 5, 5, TimeUnit.MINUTES);
    }

    private void validateConnection() {
        try {
            if (connection == null || session == null || !connection.getMetaData().getJMSProviderName().equals("Solace")) {
                logger.warn("JMS connection/session is null or inactive. Reinitializing...");
                cleanup();
                establishBrokerConnection();
            }
        } catch (JMSException e) {
            logger.error("Error validating/reinitializing JMS connection/session", e);
            cleanup();
            try {
                establishBrokerConnection();
            } catch (JMSException ex) {
                logger.error("Error reinitializing JMS connection/session", ex);
            }
        }
    }

    @PreDestroy
    void cleanup() {
        try {
            if (consumer != null) {
                consumer.close();
            }
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
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
        }
    }
}
