package com.scalefocus.mile.jms.auth.poc.core;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.jms.ConnectionFactory;

@ApplicationScoped
public final class BrokerClientConfig {

    private final String solaceHost;
    private final String solaceVpn;
    private final String solaceUsername;
    private final String solacePassword;
    private final String trustStorePath;
    private final String trustStorePassword;
    private final String keyStorePath;
    private final String keyStorePassword;

    public BrokerClientConfig(
            @ConfigProperty(name = "solace.host") String solaceHost,
            @ConfigProperty(name = "solace.vpn") String solaceVpn,
            @ConfigProperty(name = "solace.username") String solaceUsername,
            @ConfigProperty(name = "solace.password") String solacePassword,
            @ConfigProperty(name = "solace.ssl.trust-store") String trustStorePath,
            @ConfigProperty(name = "solace.ssl.trust-store-password") String trustStorePassword,
            @ConfigProperty(name = "solace.ssl.key-store") String keyStorePath,
            @ConfigProperty(name = "solace.ssl.key-store-password") String keyStorePassword
    ) {
        this.solaceHost = solaceHost;
        this.solaceVpn = solaceVpn;
        this.solaceUsername = solaceUsername;
        this.solacePassword = solacePassword;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
    }

    @Produces
    ConnectionFactory connectionFactory() throws Exception {
        SolConnectionFactory connectionFactory = SolJmsUtility.createConnectionFactory();
        connectionFactory.setHost(solaceHost);
        connectionFactory.setVPN(solaceVpn);
        connectionFactory.setUsername(solaceUsername);
        connectionFactory.setPassword(solacePassword);

        // Set SSL properties
        connectionFactory.setSSLTrustStore(trustStorePath);
        connectionFactory.setSSLTrustStorePassword(trustStorePassword);
        connectionFactory.setSSLKeyStore(keyStorePath);
        connectionFactory.setSSLKeyStorePassword(keyStorePassword);

        return connectionFactory;
    }
}
