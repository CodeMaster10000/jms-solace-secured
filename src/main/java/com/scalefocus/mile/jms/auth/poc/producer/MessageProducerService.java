package com.scalefocus.mile.jms.auth.poc.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

@ApplicationScoped
final class MessageProducerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);

    private final ConnectionFactory connectionFactory;
    private final String solaceQueue;

    MessageProducerService(
             ConnectionFactory connectionFactory,
            @ConfigProperty(name = "solace.queue.data") String solaceQueue) {
        this.connectionFactory = connectionFactory;
        this.solaceQueue = solaceQueue;
    }

    Response sendMessageToBroker(String messageContent) {
        Connection connection = null;
        Session session = null;
        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(solaceQueue);
            MessageProducer producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage(messageContent);
            producer.send(message);
            logger.info("Message sent {}", messageContent);
            return Response.ok("Message sent successfully: " + messageContent).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.serverError().entity("Failed to send message").build();
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
