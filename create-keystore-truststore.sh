#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# Define variables from environment variables
keystorePassword=$KEYSTORE_PASSWORD
truststorePassword=$TRUSTSTORE_PASSWORD
caKey="src/main/resources/security/ca-key.pem"
caCert="src/main/resources/security/ca-cert.pem"
serverKey="src/main/resources/security/server-key.pem"
serverCert="src/main/resources/security/server-cert.pem"
combinedCertKey="src/main/resources/security/combined-cert-key.pem"

# Get the current directory as the project root
projectRoot=$(pwd)

# Define the paths for keystore and truststore relative to the project root
keystorePath="$projectRoot/src/main/resources/security/keystore.jks"
truststorePath="$projectRoot/src/main/resources/security/truststore.jks"

# Remove the security directory if it exists
rm -rf "$projectRoot/src/main/resources/security"

# Create the security directory
mkdir -p "$projectRoot/src/main/resources/security"

# Generate a CA private key and certificate
openssl genpkey -algorithm RSA -out $caKey
openssl req -x509 -new -nodes -key $caKey -sha256 -days 365 -out $caCert -subj "/CN=MyCA"

# Generate a server private key and certificate signing request (CSR)
openssl genpkey -algorithm RSA -out $serverKey
openssl req -new -key $serverKey -out server.csr -subj "/CN=localhost"

# Sign the server CSR with the CA key to get the server certificate
openssl x509 -req -in server.csr -CA $caCert -CAkey $caKey -CAcreateserial -out $serverCert -days 365 -sha256

# Combine server certificate and key into one PEM file for Solace
cat $serverCert $serverKey > $combinedCertKey

# Clean up temporary files
rm server.csr
rm "$projectRoot/src/main/resources/security/ca-cert.srl"

# Generate the keystore and truststore
keytool -genkeypair -alias serverkey -keyalg RSA -keystore "$keystorePath" -storepass $keystorePassword -dname "CN=localhost, OU=Development, O=MyCompany, L=MyCity, ST=MyState, C=US"
keytool -importcert -alias myca -file "$caCert" -keystore "$truststorePath" -storepass $truststorePassword -noprompt
keytool -importcert -alias servercert -file "$serverCert" -keystore "$truststorePath" -storepass $truststorePassword -noprompt

echo "Keystore and truststore created successfully in src/main/resources/security directory."
echo "Combined certificate and key created in src/main/resources/security/combined-cert-key.pem"

# Test the connection using OpenSSL
openssl s_client -connect localhost:55443 -CAfile "$caCert"

echo "Revocation Check should be disabled. You can now try to re-import the certificate into Solace and verify the connection."
