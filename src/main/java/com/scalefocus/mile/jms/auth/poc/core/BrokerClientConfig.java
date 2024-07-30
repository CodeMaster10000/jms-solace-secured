package com.scalefocus.mile.jms.auth.poc.core;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Configuration class for setting up the JMS broker client with SSL/TLS.
 *
 * <p>This class is responsible for loading the SSL context using the provided key store
 * and trust store configurations, and producing a configured {@code ConnectionFactory} for
 * connecting to the JMS broker.</p>
 */
@ApplicationScoped
public class BrokerClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(BrokerClientConfig.class);

    @ConfigProperty(name = "solace.host")
    String solaceHost;

    @ConfigProperty(name = "solace.username")
    String solaceUsername;

    @ConfigProperty(name = "solace.password")
    String solacePassword;

    @ConfigProperty(name = "solace.ssl.trust-store")
    String trustStorePath;

    @ConfigProperty(name = "solace.ssl.trust-store-password")
    String trustStorePassword;

    @ConfigProperty(name = "solace.ssl.key-store")
    String keyStorePath;

    @ConfigProperty(name = "solace.ssl.key-store-password")
    String keyStorePassword;

    /**
     * Initializes the SSL context by setting system properties for the key store and trust store paths and passwords.
     */
    @PostConstruct
    void initializeSslContext() {
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
    }

    /**
     * Creates and configures a JMS {@code ConnectionFactory} with SSL context.
     *
     * @return the configured {@code ConnectionFactory}, or {@code null} if an error occurs during setup
     */
    @Produces
    ConnectionFactory createConnectionFactory() {
        try {
            KeyStore keyStore = loadKeystore(keyStorePath, keyStorePassword);
            KeyStore trustStore = loadKeystore(trustStorePath, trustStorePassword);
            KeyManagerFactory kmf = getKeyManagerFactory(keyStore);
            TrustManagerFactory tmf = getTrustManagerFactory(trustStore);
            SSLContext sslContext = getSslContext(kmf, tmf);
            JmsConnectionFactory factory = new JmsConnectionFactory(solaceUsername, solacePassword, solaceHost);
            factory.setSslContext(sslContext);
            return factory;
        } catch (Exception e) {
            logger.error("Could not open Connection to Broker, cause: {}", e.getMessage());
            return null;
        }
    }

    private static SSLContext getSslContext(KeyManagerFactory kmf, TrustManagerFactory tmf) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

    private static TrustManagerFactory getTrustManagerFactory(KeyStore trustStore) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf;
    }

    private KeyManagerFactory getKeyManagerFactory(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword.toCharArray());
        return kmf;
    }

    private KeyStore loadKeystore(String keyStorePath, String keyStorePassword) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
        return keyStore;
    }
}
