# messaging-api

> Unified broker module for the Easygram framework.
> Contains the publisher SPI, Kafka publisher, RabbitMQ publisher,
> and both Kafka/RabbitMQ consumer transports.

---

## Table of Contents

- [Maven Dependency](#maven-dependency)
- [What's Included](#whats-included)
- [Configuration Overview](#configuration-overview)
- [Publisher SPI](#publisher-spi)
- [Kafka Producer (type: PRODUCER + producer.type: KAFKA)](#kafka-producer)
- [RabbitMQ Producer (type: PRODUCER + producer.type: RABBIT)](#rabbitmq-producer)
- [Kafka Consumer Transport (type: CONSUMER + consumer.type: KAFKA)](#kafka-consumer-transport)
- [RabbitMQ Consumer Transport (type: CONSUMER + consumer.type: RABBIT)](#rabbitmq-consumer-transport)
- [Forward-Only Mode](#forward-only-mode)
- [Custom Publisher](#custom-publisher)

---

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-api</artifactId>
    <version>0.0.5</version>
</dependency>
```

Kafka and RabbitMQ are declared `optional` — add only the broker(s) you use:

```xml
<!-- Required for Kafka publishing or consuming -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Required for RabbitMQ publishing or consuming -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

`spring-boot-starter` already includes `messaging-api` with all optional deps available transitively.

---

## What's Included

| Component | Package | Description |
|-----------|---------|-------------|
| `BotUpdatePublisher` | `messaging` | SPI interface for publishing raw updates |
| `BotUpdatePublishingFilter` | `messaging` | Filter that invokes the publisher |
| `KafkaBotUpdatePublisher` | `messaging.kafka` | Publishes to Kafka topic via `KafkaTemplate` |
| `KafkaMessagingAutoConfiguration` | `messaging.kafka.autoconfigure` | Auto-configures Kafka producer |
| `RabbitBotUpdatePublisher` | `messaging.rabbit` | Publishes to RabbitMQ via `RabbitTemplate` |
| `RabbitMessagingAutoConfiguration` | `messaging.rabbit.autoconfigure` | Auto-configures RabbitMQ producer |
| `KafkaConsumerAutoConfiguration` | `messaging.kafka.consumer.autoconfigure` | Activates Kafka consumer transport |
| `RabbitConsumerAutoConfiguration` | `messaging.rabbit.consumer.autoconfigure` | Activates RabbitMQ consumer transport |

---

## Configuration Overview

`messaging.type` declares the role of the application:

| Role | Config | LongPolling |
|------|--------|-------------|
| **Producer** — receives from Telegram, publishes to broker | `messaging.type: PRODUCER` | Starts (unless `forward-only: true` disables all local dispatch) |
| **Consumer** — reads from broker, dispatches to `@BotController` | `messaging.type: CONSUMER` | **Blocked** — long-polling never starts for consumer bots |

```yaml
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: PRODUCER           # or CONSUMER
    producer:
      type: KAFKA            # or RABBIT  (only for PRODUCER)
    consumer:
      type: KAFKA            # or RABBIT  (only for CONSUMER)

    kafka:                   # shared by producer + consumer
      topic: easygram-updates
      create-if-absent: true
      partitions: 1
      replication-factor: 1

    rabbit:                  # shared by producer + consumer
      exchange: easygram-exchange
      queue: easygram-updates
      routing-key: easygram.updates
      create-if-absent: true
```

---

## Publisher SPI

`BotUpdatePublisher` is the core SPI. The `BotUpdatePublishingFilter` calls it at order
`BotFilterOrder.PUBLISHING` (`Integer.MIN_VALUE + 1000`), after context filters have run.

```java
public interface BotUpdatePublisher {
    void publish(Update update);
}
```

---

## Kafka Producer

Activate by setting `messaging.type: PRODUCER` and `messaging.producer.type: KAFKA`:

```yaml
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: PRODUCER
    producer:
      type: KAFKA
    kafka:
      topic: easygram-updates
      create-if-absent: true
      partitions: 3
      replication-factor: 1

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
```

---

## RabbitMQ Producer

Activate by setting `messaging.type: PRODUCER` and `messaging.producer.type: RABBIT`:

```yaml
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: PRODUCER
    producer:
      type: RABBIT
    rabbit:
      exchange: easygram-exchange
      routing-key: easygram-updates
      create-if-absent: true

spring:
  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    port: 5672
    username: guest
    password: guest
```

---

## Kafka Consumer Transport

Activate by setting `messaging.type: CONSUMER` and `messaging.consumer.type: KAFKA`.
No `update.transport` is needed — long-polling is automatically suppressed for consumer bots.

```yaml
easygram:
  token: ${BOT_TOKEN}           # still required to send replies via Telegram API
  messaging:
    type: CONSUMER
    consumer:
      type: KAFKA
    kafka:
      topic: easygram-updates
      group-id: my-bot-group

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

---

## RabbitMQ Consumer Transport

Activate by setting `messaging.type: CONSUMER` and `messaging.consumer.type: RABBIT`:

```yaml
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: CONSUMER
    consumer:
      type: RABBIT
    rabbit:
      queue: easygram-updates
      exchange: easygram-exchange
      routing-key: easygram-updates

spring:
  rabbitmq:
    host: ${RABBIT_HOST:localhost}
```

---

## Forward-Only Mode

By default, updates are both published to the broker **and** processed locally. Set
`forward-only: true` to skip local bot handlers entirely — useful for a dedicated
forwarding instance:

```yaml
easygram:
  messaging:
    type: PRODUCER
    forward-only: true       # publish ONLY, skip local handlers
    producer:
      type: KAFKA
```

---

## Custom Publisher

Implement your own publisher (SNS, SQS, Redis Pub/Sub, NATS, …) and register it as a `@Bean`:

```java
@Component
public class MyCustomPublisher implements BotUpdatePublisher {

    @Override
    public void publish(Update update) {
        // forward to any system
    }
}
```

Because `KafkaBotUpdatePublisher` and `RabbitBotUpdatePublisher` are both `@ConditionalOnMissingBean`,
your custom bean takes precedence automatically.

---

## See Also

- [spring-boot-starter/README.md](../spring-boot-starter/README.md) — one-stop dependency
- [samples/README.md](../samples/README.md) — producer and consumer examples

