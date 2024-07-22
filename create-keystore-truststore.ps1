# Load environment variables from .env file
Get-Content -Path ".env" | ForEach-Object {
    $parts = $_ -split "="
    if ($parts.Length -eq 2) {
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        [Environment]::SetEnvironmentVariable($name, $value)
    }
}

# Define variables
$keystorePassword = [Environment]::GetEnvironmentVariable("KEYSTORE_PASSWORD")
$truststorePassword = [Environment]::GetEnvironmentVariable("TRUSTSTORE_PASSWORD")
$caKey = "src/main/resources/security/ca-key.pem"
$caCert = "src/main/resources/security/ca-cert.pem"
$serverKey = "src/main/resources/security/server-key.pem"
$serverCert = "src/main/resources/security/server-cert.pem"
$combinedCertKey = "src/main/resources/security/combined-cert-key.pem"

# Get the current directory as the project root
$projectRoot = Get-Location

# Define the paths for keystore and truststore relative to the project root
$keystorePath = "$projectRoot\src\main\resources\security\keystore.jks"
$truststorePath = "$projectRoot\src\main\resources\security\truststore.jks"

# Remove the security directory if it exists
if (Test-Path -Path "$projectRoot\src\main\resources\security") {
    Remove-Item -Recurse -Force "$projectRoot\src\main\resources\security"
}

# Create the security directory
New-Item -ItemType Directory -Path "$projectRoot\src\main\resources\security"

# Generate a CA private key and certificate
openssl genpkey -algorithm RSA -out $caKey
openssl req -x509 -new -nodes -key $caKey -sha256 -days 365 -out $caCert -subj "/CN=MyCA"

# Generate a server private key and certificate signing request (CSR)
openssl genpkey -algorithm RSA -out $serverKey
openssl req -new -key $serverKey -out server.csr -subj "/CN=localhost"

# Sign the server CSR with the CA key to get the server certificate
openssl x509 -req -in server.csr -CA $caCert -CAkey $caKey -CAcreateserial -out $serverCert -days 365 -sha256

# Combine server certificate and key into one PEM file for Solace
Get-Content $serverCert, $serverKey | Out-File -Encoding ascii $combinedCertKey

# Clean up temporary files
Remove-Item server.csr
Remove-Item "$projectRoot\src\main\resources\security\ca-cert.srl"

# Generate the keystore and truststore
keytool -genkeypair -alias serverkey -keyalg RSA -keystore $keystorePath -storepass $keystorePassword -dname "CN=localhost, OU=Development, O=MyCompany, L=MyCity, ST=MyState, C=US"
keytool -export -alias serverkey -keystore $keystorePath -rfc -file $serverCert -storepass $keystorePassword
keytool -importcert -alias myca -file $caCert -keystore $truststorePath -storepass $truststorePassword -noprompt
keytool -import -alias servercert -file $serverCert -keystore $truststorePath -storepass $truststorePassword -noprompt

Write-Host "Keystore and truststore created successfully in src/main/resources/security directory."
Write-Host "Combined certificate and key created in src/main/resources/security/combined-cert-key.pem"

# Test the connection using OpenSSL
openssl s_client -connect localhost:55443 -CAfile $caCert

Write-Host "Revocation Check should be disabled. You can now try to re-import the certificate into Solace and verify the connection."