package com.scalefocus.mile.jms.auth.poc.consumer;

import com.scalefocus.mile.jms.auth.poc.core.ThreadSafe;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
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

/**
 * A service that schedules and manages the consumption of messages from a Solace queue.
 * This class uses the {@code MessageConsumerProvider} to establish connections and consume
 * messages, ensuring all messages are processed within a scheduled interval.
 */
@ApplicationScoped
@Unremovable
@Startup
@ThreadSafe
final class MessageConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerService.class);

    private final MessageConsumerProvider messageConsumerProvider;

    private MessageConsumer consumer;

    /**
     * Constructs a new {@code MessageConsumerService} with the specified message consumer provider.
     *
     * @param messageConsumerProvider the provider to use for consuming messages
     */
    MessageConsumerService(MessageConsumerProvider messageConsumerProvider) {
        this.messageConsumerProvider = messageConsumerProvider;
    }

    /**
     * Schedules the consumption of messages at fixed intervals, starting immediately.
     */
    @PostConstruct
    void scheduleMessageConsumption() {
        ScheduledExecutorService scheduler = messageConsumerProvider.getScheduler();
        scheduler.scheduleAtFixedRate(this::consumeMessages, 0, 5, TimeUnit.MINUTES);
    }

    /**
     * Consumes messages from the queue, processing each one and waiting until all messages
     * are processed before returning.
     */
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

    /**
     * Retrieves the number of messages currently enqueued in the specified Solace queue.
     *
     * @return the number of messages in the queue
     * @throws JMSException if an error occurs while browsing the queue
     */
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

    /**
     * Cleans up JMS resources by closing the consumer.
     */
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
