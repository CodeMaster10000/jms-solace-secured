package com.scalefocus.mile.jms.auth.poc.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;

/**
 * A service for producing and sending JMS messages to a Solace queue.
 *
 * <p>This class provides functionality to create a connection to the JMS broker,
 * send a message to the specified queue, and handle any exceptions that may occur
 * during the process.</p>
 */
@ApplicationScoped
final class MessageProducerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);

    private final ConnectionFactory connectionFactory;
    private final String solaceQueue;

    /**
     * Constructs a new {@code MessageProducerService} with the specified JMS connection factory
     * and queue name.
     *
     * @param connectionFactory the JMS connection factory
     * @param solaceQueue       the name of the Solace queue to send messages to
     */
    MessageProducerService(
            ConnectionFactory connectionFactory,
            @ConfigProperty(name = "solace.queue.data") String solaceQueue) {
        this.connectionFactory = connectionFactory;
        this.solaceQueue = solaceQueue;
    }

    /**
     * Sends a text message to the configured Solace queue.
     *
     * @param messageContent the content of the message to be sent
     * @return a {@code Response} indicating the result of the send operation
     */
    Response sendMessageToBroker(String messageContent) {
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue queue = session.createQueue(solaceQueue);
            try (MessageProducer producer = session.createProducer(queue)) {
                TextMessage message = session.createTextMessage(messageContent);
                producer.send(message);
                logger.info("Message sent {}", messageContent);
                return Response.ok("Message sent successfully: " + messageContent).build();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.serverError().entity("Failed to send message").build();
        }
    }
}
