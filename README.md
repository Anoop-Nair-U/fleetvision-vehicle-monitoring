# FleetVision — Real-time Vehicle Telemetry & Alert System

**Short:** FleetVision is a Spring Boot application that ingests vehicle telemetry (via Kafka), performs windowed aggregations with Kafka Streams, and pushes live updates to a browser dashboard over STOMP/WebSockets (SockJS). It uses MySQL for persistence and demonstrates a full streaming → realtime UI pipeline — perfect for a resume/demo.

---

## Table of Contents

* [Features](#features)
* [Architecture](#architecture)
* [Tech stack](#tech-stack)
* [Getting started (quick)](#getting-started-quick)
* [Docker Compose (recommended for dev)](#docker-compose-recommended-for-dev)
* [Manual setup](#manual-setup)
* [Configuration](#configuration)
* [Build & run](#build--run)
* [API endpoints & samples](#api-endpoints--samples)
* [Web UI / WebSocket details](#web-ui--websocket-details)
* [Troubleshooting](#troubleshooting)
* [Next improvements](#next-improvements)
* [License](#license)

---

## Features

* REST API to accept telemetry (simulate IoT device)
* Kafka producer sends telemetry to `VEHICLE-TELEMETRY`
* Kafka Streams topology aggregates telemetry into 10s windows
* Aggregated metrics and raw telemetry streamed to clients via STOMP over WebSocket (`/topic/metrics`, `/topic/telemetry`)
* MySQL persistence for entities (users, telemetry history)
* Lightweight browser dashboard (no framework) that shows live feed + window metrics

---

## Architecture

```
[Device / POST -> /api/telemetry] 
       │
       └→ Spring Boot Producer → Kafka topic (VEHICLE-TELEMETRY)
            ├→ Kafka Streams (windowed aggregation) ──→ SimpMessagingTemplate ──→ WebSocket /topic/metrics
            └→ Optional Consumer / DB persister ──→ MySQL
Browser ← STOMP/SockJS ← WebSocket endpoint /ws
```

---

## Tech stack

* Java 17 (or 17+)
* Spring Boot (Web, WebSocket, Kafka, Kafka Streams, Data JPA)
* Apache Kafka & Kafka Streams
* MySQL 8
* Jackson (JSON serialization)
* STOMP over SockJS (client)
* Frontend: vanilla HTML + SockJS + Stomp.js

---

## Getting started (quick)

1. Install prerequisites:

    * Java 17 JDK
    * Maven
    * Docker & Docker Compose (recommended) **or** a running Kafka broker + MySQL
2. Clone repo:

```bash
git clone https://github.com/Anoop-Nair-U/fleetvision-vehicle-monitoring.git
cd fleetvision-vehicle-monitoring
```

3. Start services (recommended: Docker Compose — see next section)
4. Configure `src/main/resources/application.properties` (see below)
5. Build and run:

```bash
mvn -DskipTests clean package
mvn spring-boot:run
```

6. Open dashboard: `http://localhost:8080/` — status should show `connected`.

---

## Docker Compose (recommended for dev)

Create a `docker-compose.yml` (example below) and run `docker compose up -d`. This will bring up Zookeeper, Kafka and MySQL.

```yaml
version: "3.8"
services:
  zookeeper:
    image: bitnami/zookeeper:3
    environment:
      - ALLOW_ANONYMOUS_LOGIN=yes
    ports:
      - "2181:2181"

  kafka:
    image: bitnami/kafka:latest
    environment:
      - KAFKA_BROKER_ID=1
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_LISTENERS=PLAINTEXT://:9092
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
      - KAFKA_CREATE_TOPICS=VEHICLE-TELEMETRY:1:1,USER-AUTHENTICATION:1:1
      - ALLOW_PLAINTEXT_LISTENER=yes
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"

  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: rootpwd
      MYSQL_DATABASE: kafkadb
      MYSQL_USER: app
      MYSQL_PASSWORD: apppwd
    ports:
      - "3306:3306"
    command: --default-authentication-plugin=mysql_native_password
```

Notes:

* `KAFKA_CREATE_TOPICS` creates topics for you on container start. If it doesn't, create topics manually (see below).
* After `docker compose up -d` check `docker logs` for health and topic creation messages.

---

## Manual setup (if not using Docker Compose)

### Kafka

* Start Zookeeper and Kafka (local installation).
* Create topic:

```bash
kafka-topics.sh --create --topic VEHICLE-TELEMETRY --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### MySQL

* Create database `kafkadb`, create user, set password.
* Update `application.properties` with credentials.

---

## Configuration (`src/main/resources/application.properties`)

Example minimal configuration (update passwords, hosts as needed):

```properties
# Server
server.port=8080

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/kafkadb?useSSL=false&serverTimezone=UTC
spring.datasource.username=app
spring.datasource.password=apppwd
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# Topics used by the app
kafka.telemetry.topic=VEHICLE-TELEMETRY
kafka.user.topic=USER-AUTHENTICATION

# Kafka Streams
spring.kafka.streams.application-id=telemetry-streams-app
spring.kafka.streams.properties.default.key.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
spring.kafka.streams.properties.default.value.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
```

---

## Build & Run

1. Build:

```bash
mvn -DskipTests clean package
```

2. Run:

```bash
mvn spring-boot:run
```

or run the packaged jar:

```bash
java -jar target/Telimatry-0.0.1-SNAPSHOT.jar
```

---

## API endpoints & sample requests

### 1. Send telemetry (simulates device)

```
POST /api/telemetry
Content-Type: application/json
{
  "vehicleId":"KL01AB1234",
  "speed":72.3,
  "fuelLevel":44.8,
  "engineTemp":92.0
}
```

Example `curl`:

```bash
curl -X POST http://localhost:8080/api/telemetry \
 -H "Content-Type: application/json" \
 -d '{"vehicleId":"KL01AB1234","speed":72.3,"fuelLevel":44.8,"engineTemp":92.0}'
```

### 2. Create user (Phase 1)

```
POST /api/users
{
  "name":"Anoop Nair",
  "email":"anoop@example.com"
}
```

### 3. Test WebSocket push (manual)

```
GET /api/test/metrics
GET /api/test/telemetry
```

These endpoints (if present) push a sample message to `/topic/metrics` and `/topic/telemetry` respectively — useful to verify frontend subscriptions.

---

## Web UI & WebSocket

* App root: `http://localhost:8080/` — the static `index.html` dashboard.
* WebSocket handshake endpoint: `ws://localhost:8080/ws` (client uses SockJS)
* STOMP subscriptions:

    * `/topic/telemetry` — raw telemetry feed (for live table / charts)
    * `/topic/metrics` — aggregated window metrics (10-second windows by default)

Frontend connects via SockJS + Stomp.js. If status shows `connected` but no rows appear, follow [Troubleshooting](#troubleshooting).

---

## Troubleshooting (common issues)

* **No messages in UI**

    1. Confirm STOMP connected (browser console) and you see SUBSCRIBE frames (Network → WS).
    2. Use `/api/test/metrics` to verify server → WebSocket pipeline. If test works, Kafka/Streams is upstream problem.
    3. Check server logs for `Broadcasting metric` entries (we log before sending).
* **Streams crashes with deserialization errors**

    * This happens if the topic contains non-JSON messages (e.g., `toString()` output). Options:

        * Recreate the topic (delete & create), or
        * Change Streams code to consume raw `String` and parse with `ObjectMapper` (topology already handles this), or
        * Configure `LogAndContinueExceptionHandler` for Streams.
* **Kafka producer errors**

    * Ensure `spring.kafka.bootstrap-servers` is correct, topic exists, and Kafka is reachable.
* **MySQL errors**

    * Ensure DB credentials in `application.properties` are correct and the DB is reachable. Check `spring.jpa.hibernate.ddl-auto=update` to create tables automatically in dev.
* **WebSocket handshake fails on cross-origin**

    * Ensure `WebSocketConfig` used `setAllowedOriginPatterns("*")` (dev only) or set proper `allowedOrigins`.

Useful commands:

* Tail Spring Boot logs: `mvn spring-boot:run` or `tail -f logs`
* Inspect Kafka topic:

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic VEHICLE-TELEMETRY --from-beginning --max-messages 10
```

---

## Contributing

PRs welcome — please open issues/PRs for bugs, new features or doc fixes. Keep changes small & documented.

---

## License

MIT © Anoop Nair

