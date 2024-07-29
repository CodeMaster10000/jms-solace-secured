package com.scalefocus.mile.jms.auth.poc.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;

@ApplicationScoped
public final class MessageProducerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);

    private final ConnectionFactory connectionFactory;
    private final String solaceQueue;

    public MessageProducerService(
            ConnectionFactory connectionFactory,
            @ConfigProperty(name = "solace.queue.data") String solaceQueue) {
        this.connectionFactory = connectionFactory;
        this.solaceQueue = solaceQueue;
    }

    public Response sendMessageToBroker(String messageContent) {
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
