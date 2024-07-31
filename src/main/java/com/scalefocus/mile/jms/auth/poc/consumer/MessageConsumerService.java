package com.scalefocus.mile.jms.auth.poc.consumer;

import com.scalefocus.mile.jms.auth.poc.core.ThreadSafe;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Unremovable
@Startup
@ThreadSafe
final class MessageConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerService.class);

    private final MessageConsumerProvider messageConsumerProvider;

    private MessageConsumer consumer;

    MessageConsumerService(MessageConsumerProvider messageConsumerProvider) {
        this.messageConsumerProvider = messageConsumerProvider;
    }

    @PostConstruct
    void scheduleMessageConsumption() {
        ScheduledExecutorService scheduler = messageConsumerProvider.getScheduler();
        scheduler.scheduleAtFixedRate(this::consumeMessages, 0, 5, TimeUnit.MINUTES);
    }

    private void consumeMessages() {
        try {
            int queueSize = getQueueSize();
            CountDownLatch latch = new CountDownLatch(queueSize);
            messageConsumerProvider.setLatch(latch);
            consumer = messageConsumerProvider.createConsumer();
            latch.await();
        } catch (JMSException | InterruptedException e) {
            logger.error("Error initializing Message message handler service", e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
        } finally {
            cleanup();
        }
    }

    private int getQueueSize() throws JMSException {
        int count = 0;
        Session session = messageConsumerProvider.getSession();
        String queueEndpoint = messageConsumerProvider.getQueueUrl();
        try (QueueBrowser browser = session.createBrowser(session.createQueue(queueEndpoint))) {
            Enumeration<?> messages = browser.getEnumeration();
            while (messages.hasMoreElements()) {
                messages.nextElement();
                count++;
            }
        }
        return count;
    }

    private void cleanup() {
        try {
            if (consumer != null) {
                consumer.close();
            }
        } catch (JMSException e) {
            logger.error("Error closing Message consumer", e);
        }
    }
}
