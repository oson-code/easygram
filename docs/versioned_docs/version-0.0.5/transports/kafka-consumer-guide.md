---
id: kafka-consumer-guide
title: Kafka Consumer Transport
description: Run a Kafka consumer bot with Easygram — configure easygram.messaging.type=CONSUMER, consumer type KAFKA, topic, group-id, bootstrap-servers, and Docker Compose with KRaft.
keywords: [telegram bot kafka consumer, easygram kafka consumer, spring boot telegram kafka transport, kafka consumer telegram bot java, easygram messaging.type consumer]
---

# Kafka Consumer Transport

The Kafka consumer transport lets your bot receive Telegram updates from an Apache Kafka topic instead of polling Telegram directly. A separate **producer bot** publishes updates to the topic; this bot only processes them.

This pattern decouples your processing logic from Telegram's API, enables horizontal scaling (multiple consumer instances on the same consumer group), and integrates naturally with event-driven architectures.

## Architecture

```
Telegram API
    ↓  (long-polling or webhook)
Producer Bot  →  [Kafka Topic: easygram-updates]
                            ↓
          ┌─────────────────┼─────────────────┐
    Consumer Bot #1   Consumer Bot #2   Consumer Bot #3
    (same group — Kafka distributes partitions)
```

## Add Dependencies

`messaging-api` handles both Kafka producer and consumer functionality, but `spring-kafka` is **optional** in `messaging-api` to avoid unwanted broker connections. Declare it explicitly:

```xml
<!-- Easygram starter (already includes messaging-api) -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>

<!-- Required: spring-kafka is marked optional in messaging-api -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Or without the starter:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-api</artifactId>
    <version>0.0.5</version>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

## Configuration

**application.yml:**
```yaml
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: CONSUMER
    consumer:
      type: KAFKA
    kafka:
      topic: easygram-updates
      group-id: my-bot-group
      create-if-absent: true
      partitions: 1
      replication-factor: 1

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

:::info Consumer transport and `easygram.update.transport`
When `easygram.messaging.type=CONSUMER`, the bot receives updates from Kafka — not from
Telegram directly. The `easygram.update.transport` property (long-polling / webhook) has no
effect and the Telegram update transport is not started.
:::

## Configuration Reference

### `easygram.messaging.*`

| Property | Required | Default | Description |
|---|---|---|---|
| `easygram.messaging.type` | **Yes** | — | Must be `CONSUMER` |
| `easygram.messaging.consumer.type` | **Yes** | — | Must be `KAFKA` |

### `easygram.messaging.kafka.*`

| Property | Required | Default | Description |
|---|---|---|---|
| `easygram.messaging.kafka.topic` | **Yes** | — | Kafka topic to consume updates from |
| `easygram.messaging.kafka.group-id` | No | `easygram-bot` | Kafka consumer group ID |
| `easygram.messaging.kafka.create-if-absent` | No | `true` | Auto-create topic if it does not exist |
| `easygram.messaging.kafka.partitions` | No | `1` | Partitions for auto-created topic |
| `easygram.messaging.kafka.replication-factor` | No | `1` | Replication factor for auto-created topic |

### Standard Spring Kafka properties

| Property | Required | Default | Description |
|---|---|---|---|
| `spring.kafka.bootstrap-servers` | **Yes** | — | Kafka broker address(es) |

## Message Format

The producer serialises each `Update` as JSON. The consumer expects the same format:

```json
{
  "updateId": 123456789,
  "message": {
    "messageId": 42,
    "date": 1700000000,
    "chat": { "id": 987654321, "type": "private" },
    "from": { "id": 111222333, "firstName": "Alice", "isBot": false },
    "text": "Hello bot"
  }
}
```

## Example Bot

```java
@SpringBootApplication
public class KafkaConsumerBotApp {
    public static void main(String[] args) {
        SpringApplication.run(KafkaConsumerBotApp.class, args);
    }
}

@BotController
public class KafkaConsumerBot {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello " + user.getFirstName() + " — update received from Kafka!";
    }

    @BotDefaultHandler
    public String onAny(@BotTextValue String text) {
        return "Processed via Kafka: " + text;
    }
}
```

## Docker Deployment

The following `docker-compose.yml` uses **Bitnami Kafka in KRaft mode** (no ZooKeeper required):

```yaml
services:
  kafka:
    image: bitnami/kafka:latest
    environment:
      KAFKA_CFG_NODE_ID: "1"
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
    ports:
      - "9092:9092"

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    depends_on:
      - kafka

  bot:
    build: .
    environment:
      easygram.token: ${BOT_TOKEN}
      easygram.messaging.type: CONSUMER
      easygram.messaging.consumer.type: KAFKA
      easygram.messaging.kafka.topic: easygram-updates
      easygram.messaging.kafka.group-id: my-bot-group
      spring.kafka.bootstrap-servers: kafka:9092
    depends_on:
      - kafka
    restart: unless-stopped
```

**Run:**
```bash
BOT_TOKEN="your_token" docker compose up
```

Open Kafka UI at [http://localhost:8090](http://localhost:8090) to inspect topics and messages.

## Horizontal Scaling

All instances with the same `group-id` share a consumer group. Kafka distributes topic partitions across them — each update is processed by exactly one instance:

```bash
# Three instances, same group — partitions distributed among them
easygram.messaging.kafka.group-id=my-bot-group

# A second group processes every update independently (e.g., for analytics)
easygram.messaging.kafka.group-id=analytics-group
```

For this to work correctly, the topic must have enough partitions to distribute across your instances (one partition per instance maximum):

```yaml
easygram:
  messaging:
    kafka:
      topic: easygram-updates
      partitions: 3            # supports up to 3 concurrent consumer instances
      create-if-absent: true
```

## Use Cases

| Use case | Pattern |
|---|---|
| Decoupled microservices | Producer bot forwards; multiple consumer bots handle specific domains |
| Event replay | Consumer reads from earliest offset to re-process historical updates |
| Analytics pipeline | Second consumer group reads all updates for logging/analytics |
| Horizontal scaling | Multiple consumer instances in same group share the load |

---

Next: Learn about [RabbitMQ Consumer Transport](rabbitmq-consumer-guide).

