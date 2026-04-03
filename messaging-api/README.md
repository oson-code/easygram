# messaging-api

> Unified broker module for the Easygram framework.
> Contains the publisher SPI, Kafka publisher, RabbitMQ publisher, smart routing,
> and both Kafka/RabbitMQ consumer transports — consolidated from six modules in 0.0.5.

---

## Table of Contents

- [Maven Dependency](#maven-dependency)
- [What's Included](#whats-included)
- [Publisher SPI](#publisher-spi)
- [Kafka Publisher](#kafka-publisher)
- [RabbitMQ Publisher](#rabbitmq-publisher)
- [Smart Producer Routing](#smart-producer-routing)
- [Kafka Consumer Transport](#kafka-consumer-transport)
- [RabbitMQ Consumer Transport](#rabbitmq-consumer-transport)
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
| `BotUpdatePublisher` | `messaging.*` | SPI interface for publishing raw updates |
| `BotUpdatePublishingFilter` | `messaging.*` | Filter that invokes the publisher |
| `KafkaBotUpdatePublisher` | `messaging.kafka.*` | Publishes to Kafka topic via `KafkaTemplate` |
| `KafkaBotUpdatePublisherAutoConfiguration` | `messaging.kafka.*` | Auto-configures Kafka publisher |
| `RabbitBotUpdatePublisher` | `messaging.rabbit.*` | Publishes to RabbitMQ via `RabbitTemplate` |
| `RabbitBotUpdatePublisherAutoConfiguration` | `messaging.rabbit.*` | Auto-configures RabbitMQ publisher |
| Producer routing auto-config | `messaging.producer.*` | Activates Kafka or RabbitMQ publisher via one property |
| Kafka consumer transport | `messaging.consumer.kafka.*` | `@KafkaListener` → Bot pipeline |
| RabbitMQ consumer transport | `messaging.consumer.rabbit.*` | `@RabbitListener` → Bot pipeline |
| Consumer routing auto-config | `messaging.consumer.*` | Activates Kafka or RabbitMQ consumer via one property |

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

## Kafka Publisher

```yaml
telegram:
  bot:
    messaging:
      producer:
        producer-type: kafka
      kafka:
        topic: telegram-updates
        create-if-absent: true   # auto-create topic on startup
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

## RabbitMQ Publisher

```yaml
telegram:
  bot:
    messaging:
      producer:
        producer-type: rabbit
      rabbit:
        exchange: telegram-exchange
        routing-key: telegram-updates
        declare-infrastructure: true   # auto-declare exchange + queue on startup

spring:
  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    port: 5672
    username: guest
    password: guest
```

---

## Smart Producer Routing

Set `producer-type` to select the active publisher at runtime without changing code:

```yaml
telegram:
  bot:
    messaging:
      producer:
        producer-type: kafka   # or: rabbit
```

Only one publisher is active at a time. Both `spring-kafka` and `spring-boot-starter-amqp`
can coexist on the classpath; the property controls which one is wired.

---

## Kafka Consumer Transport

Set `transport: KAFKA_CONSUMER` to receive updates from a Kafka topic:

```yaml
telegram:
  bot:
    transport: KAFKA_CONSUMER
    messaging:
      consumer:
        consumer-type: kafka
      kafka-consumer:
        topic: telegram-updates
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

Set `transport: RABBIT_CONSUMER` to receive updates from a RabbitMQ queue:

```yaml
telegram:
  bot:
    transport: RABBIT_CONSUMER
    messaging:
      consumer:
        consumer-type: rabbit
      rabbit-consumer:
        queue: telegram-updates
        exchange: telegram-exchange
        routing-key: telegram-updates

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
telegram:
  bot:
    messaging:
      forward-only: true    # publish ONLY, skip local handlers
      producer:
        producer-type: kafka
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
