
# JMS-Solace-PoC

This project leverages Quarkus, the supersonic subatomic Java framework,
to create a high-performance, modern application.
For more information about Quarkus, please visit its official website.

In this proof of concept,
we focus on secure, asynchronous communication between a JMS producer and consumer using Solace as the message broker.
The communication is fortified with TLS and tcps protocols,
ensuring robust security and data integrity throughout the messaging process.
This setup effectively demonstrates the seamless integration and secured message exchange capabilities of
Quarkus and Solace in a real-world scenario.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/jms-auth-poc-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- RESTEasy Classic JSON-B ([guide](https://quarkus.io/guides/rest-json)): JSON-B serialization support for RESTEasy Classic
- OpenID Connect ([guide](https://quarkus.io/guides/security-openid-connect)): Verify Bearer access tokens and authenticate users with Authorization Code Flow
- RESTEasy Classic ([guide](https://quarkus.io/guides/resteasy)): REST endpoint framework implementing Jakarta REST and more
- Artemis JMS ([guide](https://docs.quarkiverse.io/quarkus-artemis/dev/index.html)): Use JMS APIs to connect to ActiveMQ Artemis via its native protocol

## Initial Setup

1. Execute the PowerShell script at the root of the project:

```shell script
./create-keystore-truststore.ps1
```

2. Navigate to the Docker configuration directory and bring up the Docker containers:

```shell script
cd src/main/docker
docker-compose up
```

3. Open your web browser and navigate to [http://localhost:8080](http://localhost:8080). Log in with the credentials `admin:admin`.

4. Go to `Default VPN -> Queues` and create a queue with the name `demo-queue`.

5. Go to `System -> TLS Configuration`. Edit and add the `combined-cert-key.pem` located in the `src/main/resources/security` folder, leaving the password field blank. Click apply.

## Running the Application

After completing the initial setup, you can run the application using the following command:

```shell script
./mvnw compile quarkus:dev
```

## Test Process

Navigate to the Quarkus DEV-UI and find the Endpoint `solace/{message}` and send the request.
This will trigger the JMS Producer `MessageProducerService` to send a message to the `demo-queue`. 
The `MessageConsumerService` acts as a JMS Consumer and will listen for events on that queue.
Verify the application logs for operation validity.