---
id: kafka-consumer-guide
title: Kafka Consumer Transport
---

# Kafka Consumer Transport

Consume Telegram updates from a Kafka topic instead of polling or webhook.

## Use Cases

- Decouple bot from Telegram API
- Process updates through event streaming pipeline
- Multiple bot instances consuming same topic
- Real-time analytics on updates

## Configuration

**application.yml:**
```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: KAFKA_CONSUMER
    kafka-consumer:
      topic: telegram-updates
      create-if-absent: true
      partitions: 1
      replication-factor: 1

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: bot-group-1
```

## Configuration Options

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.kafka-consumer.topic` | Yes | — | Kafka topic to consume from |
| `telegram.bot.kafka-consumer.create-if-absent` | No | `true` | Auto-create topic if not present |
| `telegram.bot.kafka-consumer.partitions` | No | `1` | Partitions for auto-created topic |
| `telegram.bot.kafka-consumer.replication-factor` | No | `1` | Replication factor for auto-created topic |
| `spring.kafka.bootstrap-servers` | Yes | — | Kafka broker address(es) |
| `spring.kafka.consumer.group-id` | Yes | — | Consumer group ID |

## Message Format

Updates must be published as JSON in Kafka topic:

```json
{
  "updateId": 123,
  "message": {
    "messageId": 456,
    "date": 1234567890,
    "chat": {"id": 789, "type": "private"},
    "from": {"id": 999, "firstName": "John"},
    "text": "Hello bot"
  }
}
```

## Architecture

```
Telegram API
    ↓
[Kafka Topic: telegram-updates]
    ↓
[Bot Instance 1] [Bot Instance 2] [Bot Instance 3]
    (all consuming same topic)
```

## Example Setup

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  bot:
    build: .
    environment:
      TELEGRAM_BOT_TRANSPORT: KAFKA_CONSUMER
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      TELEGRAM_BOT_KAFKA_CONSUMER_TOPIC: telegram-updates
    depends_on:
      - kafka
```

## Publishing Updates to Kafka

Use messaging-kafka module to publish:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-kafka</artifactId>
    <version>0.0.2</version>
</dependency>
```

**application.yml:**
```yaml
telegram:
  bot:
    messaging:
      forward-only: true
      producer:
        producer-type: kafka
    kafka-consumer:
      topic: telegram-updates

spring:
  kafka:
    bootstrap-servers: localhost:9092
```

See [Broker Publishing](../advanced/broker-publishing) for details.

## Scaling

Perfect for horizontal scaling:

```bash
# Bot instance 1
export BOT_CONSUMER_GROUP=bot-group-1
mvn spring-boot:run

# Bot instance 2 (same group)
export BOT_CONSUMER_GROUP=bot-group-1
mvn spring-boot:run

# Bot instance 3 (different group, processes all updates)
export BOT_CONSUMER_GROUP=bot-group-2
mvn spring-boot:run
```

Kafka automatically distributes partitions among instances in same group.

---

Next: Learn about [RabbitMQ Consumer Transport](rabbitmq-consumer-guide).
