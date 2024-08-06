
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

## Initial Setup

1. Execute the PowerShell script at the root of the project:

```shell script
./create-keystore-truststore.ps1
```

If you are running on Mac/Linux, execute:
```shell script
./create-keystore-truststore.sh
```

2. Make sure you create a .env file at the root of the project and 
   add the environment variables that the `application.properties` file uses.

3. Navigate to the Docker configuration directory and bring up the Docker containers:

```shell script
cd src/main/docker
docker-compose up
```

4. Open your web browser and navigate to [http://localhost:8085](http://localhost:8085). Log in with the credentials `admin:admin`.

5. Go to `Default VPN -> Queues` and create a queue with the name `demo-queue`.

6. Go to `System -> TLS Configuration`. Edit and add the `combined-cert-key.pem` located in the `src/main/resources/security` folder, leaving the password field blank. Click apply.

## Running the Application

After completing the initial setup, you can run the application using the following command:

```shell script
./mvnw compile quarkus:dev
```

## Test Process

Navigate to the Quarkus DEV-UI and find the Endpoint `solace/{message}` and send the request.
This will trigger the JMS Producer `MessageProducerService` to send a message to the `demo-queue`. 
The `MessageConsumerService` acts as a JMS Consumer and will periodically listen for events on that queue.
Verify the application logs for operation validity.

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
