package com.scalefocus.mile.jms.auth.poc.consumer;

import com.scalefocus.mile.jms.auth.poc.core.ThreadSafe;
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

/**
 * A service for consuming JMS messages from a Solace queue. This class establishes
 * and maintains a connection to the JMS broker, listens for messages on the configured
 * queue, and processes incoming messages.
 *
 * <p>This class is designed to be thread-safe and automatically starts up with the
 * application. It periodically validates the JMS connection and re-establishes it if
 * necessary.</p>
 */
@ApplicationScoped
@Unremovable
@Startup
@ThreadSafe
final class MessageConsumerService implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerService.class);

    private final ConnectionFactory connectionFactory;
    private final String solaceQueue;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private ScheduledExecutorService scheduler;

    /**
     * Constructs a new {@code MessageConsumerService} with the specified JMS connection factory
     * and queue name.
     *
     * @param connectionFactory the JMS connection factory
     * @param solaceQueue       the name of the Solace queue to consume messages from
     */
    MessageConsumerService(
            ConnectionFactory connectionFactory,
            @ConfigProperty(name = "solace.queue.data") String solaceQueue) {
        this.connectionFactory = connectionFactory;
        this.solaceQueue = solaceQueue;
        initialize();
    }

    /**
     * Initializes the JMS consumer service by establishing a connection to the JMS broker
     * and scheduling periodic validation of the connection.
     */
    @PostConstruct
    private void initialize() {
        try {
            establishBrokerConnection();
            scheduleConnectionValidation();
        } catch (JMSException e) {
            logger.error("Error initializing JMS consumer: {}", e.getMessage());
        }
    }

    /**
     * Establishes a connection to the JMS broker, creates a session and a consumer for
     * the specified queue, and sets this service as the message listener. The connection
     * is then started to begin receiving messages.
     *
     * @throws JMSException if an error occurs while establishing the connection or creating the session/consumer
     */
    private synchronized void establishBrokerConnection() throws JMSException {
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(solaceQueue);
        consumer = session.createConsumer(queue);
        consumer.setMessageListener(this);
        connection.start();
    }

    /**
     * Schedules periodic validation of the JMS connection using a single-threaded scheduled executor
     * service. The connection is validated every 5 minutes.
     */
    private synchronized void scheduleConnectionValidation() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::validateConnection, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Validates the JMS connection and re-establishes it if necessary. This method is called
     * periodically by the scheduled executor service.
     */
    private void validateConnection() {
        try {
            synchronized (this) {
                if (connection == null || session == null) {
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

    /**
     * Cleans up JMS resources by closing the consumer, session, and connection,
     * and shutting down the scheduler. This method is called when the service is destroyed.
     */
    @PreDestroy
    private synchronized void cleanup() {
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

    /**
     * Handles incoming JMS messages. If the message is a {@code TextMessage}, it logs the
     * message content. Otherwise, it logs a warning indicating a non-text message was received.
     *
     * @param message the incoming JMS message
     */
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
