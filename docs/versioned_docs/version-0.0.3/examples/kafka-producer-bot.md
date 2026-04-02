---
id: kafka-producer-bot
title: Kafka Producer Bot Example
---

# Kafka Producer Bot Example

A long-polling bot that forwards every incoming Telegram update to Apache Kafka. With
`forward-only: true`, local handlers are never called — the bot acts as a pure ingest
gateway. A downstream Kafka consumer service handles the actual processing.

**Full source:** [`samples/longpolling-as-producer`](https://github.com/oson-code/easygram/tree/main/samples/longpolling-as-producer)

## Architecture

```
Telegram
  ↓ long-polling
BotContextSetterFilter
  ↓
BotUpdatePublishingFilter → Kafka topic "telegram-updates"
  ↓ (forward-only: true)
  STOP — local @BotController handlers never run
```

## Use Cases

- **Audit / analytics** — persist every update to a data warehouse via Kafka Connect
- **Fan-out** — multiple consumer groups each process the same update stream differently
- **Decoupling** — scale the Telegram gateway and processing service independently
- **Event sourcing** — Kafka as the source of truth for all bot interactions

## Project Setup

### Maven

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.3</version>
</dependency>
```

### application.yml

```yaml
telegram:
  bot:
    token: "${BOT_TOKEN}"
    # transport: LONG_POLLING is the default

    messaging:
      # true  → forward ONLY to broker; local @BotController handlers are skipped
      # false → forward to broker AND run local handlers
      forward-only: true

      producer:
        producer-type: kafka

      kafka:
        topic: telegram-updates
        create-if-absent: true  # Auto-create topic on startup
        partitions: 1
        replication-factor: 1

spring:
  application:
    name: longpolling-as-producer

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer:   org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3

logging:
  level:
    uz.osoncode: DEBUG
    org.springframework.kafka: WARN
```

## Application Class

```java
package uz.example.producer.longpolling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pure long-polling producer bot.
 * All infrastructure uses framework defaults — no overrides needed.
 * Updates are forwarded to Kafka; local handlers are never called when forward-only=true.
 */
@SpringBootApplication
public class LongpollingProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LongpollingProducerApplication.class, args);
    }
}
```

## Optional Local Controller

You can keep a `@BotController` even in `forward-only: true` mode. It will be available when
you switch to `forward-only: false` for hybrid operation:

```java
@BotController
public class EchoBotController {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName()
               + "! Updates are being forwarded to the message broker.";
    }

    @BotTextDefault
    public String onText(@BotTextValue String text) {
        return "Forwarded to broker: " + text;
    }

    @BotDefaultHandler
    public SendMessage onUnknown(BotRequest request) {
        return SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("Update forwarded to broker.")
                .build();
    }
}
```

:::note
With `forward-only: true` these handlers are never invoked. Set `forward-only: false` to run
both the broker publish **and** the local handlers simultaneously.
:::

## Kafka Message Format

Each update is serialized as a JSON string using the configured `ObjectMapper`:

```json
{
  "update_id": 123456789,
  "message": {
    "message_id": 42,
    "from": { "id": 12345, "first_name": "Alice", "language_code": "en" },
    "chat": { "id": 12345, "type": "private" },
    "date": 1718000000,
    "text": "/start"
  }
}
```

**Kafka message key**: `update_id` as a string. This enables partition ordering per update ID
when using a custom key-based partitioner.

## Downstream Consumer

Use `messaging-kafka-consumer` transport to consume updates through the full bot pipeline
in a separate application:

```yaml
# consumer/application.yml
telegram:
  bot:
    token: "${BOT_TOKEN}"
    transport: KAFKA_CONSUMER
    messaging:
      kafka:
        topic:    telegram-updates
        group-id: my-bot-consumer-group

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      key-deserializer:   org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      group-id: my-bot-consumer-group
      auto-offset-reset: earliest
```

See the [Kafka Consumer Guide](../transports/kafka-consumer-guide) for full configuration.

## RabbitMQ Variant

Switch to RabbitMQ by changing two properties:

```yaml
telegram:
  bot:
    messaging:
      producer:
        producer-type: rabbit    # was: kafka
      rabbit:
        exchange:     telegram-exchange
        routing-key:  telegram.updates
        queue:        telegram-updates
        create-if-absent: true

spring:
  rabbitmq:
    host:     ${RABBITMQ_HOST:localhost}
    port:     5672
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
```

## Running with Docker Compose

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on: [zookeeper]
    ports: ["9092:9092"]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  bot:
    image: my-kafka-producer-bot:latest
    depends_on: [kafka]
    environment:
      BOT_TOKEN: "${BOT_TOKEN}"
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
```

## Error Handling

- If Kafka is unavailable and retries are exhausted, the exception is logged and the update is
  dropped. The long-polling offset still advances — the update is not retried.
- For guaranteed delivery configure a Dead-Letter Topic (DLT):

```yaml
spring:
  kafka:
    producer:
      retries: 5
```

---

See also:
- [Broker Publishing](../advanced/broker-publishing) — full configuration reference
- [Kafka Consumer Guide](../transports/kafka-consumer-guide) — consuming updates
- [Echo Bot Example](./echo-bot) — local processing without a broker
