package com.scalefocus.mile.jms.auth.poc.consumer;

import com.scalefocus.mile.jms.auth.poc.core.BrokerClientConfig;
import jakarta.jms.*;
import lombok.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static java.lang.System.out;

public class MessageConsumerExample {
    private static final int NUM_CLIENT_THREADS = 1;
    private static final String PROPERTIES_FILE_NAME = "src/main/resources/application.properties";
    private static final int RETURN_CODE_SUCCESS = 0;
    private static final int RETURN_CODE_ERROR_CONSUMER_CONNECTION = 1;
    private static final int RETURN_CODE_ERROR_PROPERTIES = 2;
    private static final int RETURN_CODE_ERROR_CONNECTION_FACTORY = 3;

    private final CountDownLatch latch = new CountDownLatch(NUM_CLIENT_THREADS);

    public static void main(String[] args) {
        out.println("Starting up...");

        MessageConsumerExample example = new MessageConsumerExample();
        int returnCode;

        try {
            returnCode = example.run();
        } catch (Exception e) {
            returnCode = -1;
            e.printStackTrace();
        }

        out.printf("%1$s ending with return code [%2$d]%n", MessageConsumerExample.class.getSimpleName(), returnCode);
        System.exit(returnCode);
    }

    public int run() throws InterruptedException {
        out.println("Setting up and starting consumers...");

        // Prepare message broker configuration from properties file

        File propertiesFile = new File(PROPERTIES_FILE_NAME);
        String queueName;
        BrokerClientConfig messageBrokerConfig;
        try {
            Properties solasProperties = loadProperties(propertiesFile);
            queueName = solasProperties.getProperty("solace.queue.data");
            messageBrokerConfig = new BrokerClientConfig(solasProperties);
        } catch (IOException e) {
            out.printf("Error, cannot load Solace properties from file: [%1$s], error: %2$s %n",
                    propertiesFile.getAbsolutePath(), e);
            return RETURN_CODE_ERROR_PROPERTIES;
        }

        // Prepare the connection factory

        ConnectionFactory connectionFactory = messageBrokerConfig.createConnectionFactory();
        if (connectionFactory == null) {
            out.println("Cannot create connection factory");
            return RETURN_CODE_ERROR_CONNECTION_FACTORY;
        }

        // Prepare the consumer of a queue

        MessageConsumerProvider consumerProvider;
        try {
            consumerProvider = new MessageConsumerProvider(connectionFactory, queueName);
            consumerProvider.establishBrokerConnection();
        } catch (JMSException e) {
            out.printf("Error, cannot establish connection or create consumer: %1$s%n", e);
            return RETURN_CODE_ERROR_CONSUMER_CONNECTION;
        }

        // Prepare the consumer threads which run and consumes messages

        Thread[] consumerThreads = new Thread[NUM_CLIENT_THREADS];
        for (int i = 0 ; i < NUM_CLIENT_THREADS ; ++i) {
            try {
                MessageConsumer consumer = consumerProvider.createConsumer(true);
                consumerThreads[i] = new Thread(createConsumerThreadRunnable(consumer, "consumer_" + i));
                consumerThreads[i].start();
            } catch (JMSException e) {
                consumerThreads[i] = null;
            }
        }

        // Wait for the completion of the consumer threads

        latch.await();

        return RETURN_CODE_SUCCESS;
    }

    private Runnable createConsumerThreadRunnable(MessageConsumer consumer, String consumerId) {
        return () -> {
            out.printf("[%1$s]: Consumer thread started%n", consumerId);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message message = consumer.receive(1000);
                    if (message != null) {
                        int result = onMessage(message, consumerId);
                        if (result != 0) {
                            latch.countDown();
                            break;
                        }
                    }
                } catch (JMSException e) {
                    out.printf("[%1$s]: Error: %2$s%n", consumerId, e);
                }
            }

            closeConsumer(consumer);
            out.printf("[%1$s]: Consumer thread ended%n", consumerId);
        };
    }

    private int onMessage(@NonNull Message message, String consumerId) throws JMSException {
        int returnCode = 0;

        if (message instanceof TextMessage) {
            String content = ((TextMessage) message).getText();
            out.printf("[%1$s]: Message received: [%2$s]%n", consumerId, content);

            returnCode = content.equalsIgnoreCase("goodbye") ? 1 : 0;
        } else {
            out.printf("[%1$s]: Received non-text message.%n", consumerId);
        }

        return returnCode;
    }

    private void closeConsumer(MessageConsumer consumer) {
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException ignored) {}
        }
    }

    private static Properties loadProperties(File propertiesFile) throws IOException {
        Properties properties = loadProperties(propertiesFile.getAbsolutePath());

        // Load system environment variables where needed
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value.startsWith("${") && value.endsWith("}")) {
                String envVar = System.getenv(value.substring(2, value.length() - 1));
                if (envVar != null) {
                    properties.setProperty(key, envVar);
                }
            }
        }

        return properties;
    }

    private static Properties loadProperties(String fileName) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(fileName)) {
            properties.load(inputStream);
        }
        return properties;
    }

}
