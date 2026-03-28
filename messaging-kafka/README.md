# messaging-kafka

> Kafka `BotUpdatePublisher` implementation for the Easygram framework.

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-kafka</artifactId>
    <version>0.0.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

## What It Does

Serializes each incoming Telegram `Update` to JSON and sends it to a configured Kafka topic using Spring's `KafkaTemplate`. The message key is the `updateId` (String) and the value is the JSON-serialized `Update` object. Integrates with the `BotUpdatePublishingFilter` from the `messaging-api` module to forward every update before any handler logic runs.

## Architecture

```
Telegram API
     │
     ▼
[LongPollingBot / WebhookBot]
     │
     ▼
[BotUpdatePublishingFilter]   ← runs at order MIN_VALUE + 1000
     │
     ▼
[KafkaBotUpdatePublisher]
     │
     ▼
[Kafka Topic: telegram-updates]   ← consumed by messaging-kafka-consumer
```

## Configuration

```yaml
telegram:
  bot:
    messaging:
      producer:
        producer-type: kafka
      kafka:
        topic: telegram-updates
        create-if-absent: true         # default: true
        partitions: 1                  # default: 1
        replication-factor: 1          # default: 1

spring:
  kafka:
    bootstrap-servers: localhost:9092
```

## All Properties

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.messaging.producer.producer-type` | ✅ | — | Must be `kafka` to activate this publisher |
| `telegram.bot.messaging.kafka.topic` | ✅ | — | Kafka topic to publish updates to |
| `telegram.bot.messaging.kafka.create-if-absent` | ❌ | `true` | Auto-create the topic if absent |
| `telegram.bot.messaging.kafka.partitions` | ❌ | `1` | Partitions for auto-created topic |
| `telegram.bot.messaging.kafka.replication-factor` | ❌ | `1` | Replication factor for auto-created topic |
| `spring.kafka.bootstrap-servers` | ✅ | — | Kafka broker addresses |

## Message Format

| Field | Value |
|---|---|
| **Key** | `updateId` as a `String` |
| **Value** | JSON-serialised `Update` object |
| **Headers** | None (default) |
| **Serializer** | `StringSerializer` for key; `StringSerializer` for value (JSON string) |

The `ObjectMapper` used for JSON serialization is provided by `BotObjectMapperProvider` and defaults to the shared Spring `ObjectMapper` bean. Override it by declaring your own `BotObjectMapperProvider` bean:

```java
@Bean
public BotObjectMapperProvider botObjectMapperProvider() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return () -> mapper;
}
```

## Forward-Only Mode

To publish updates to Kafka without processing them locally, set `forward-only: true`:

```yaml
telegram:
  bot:
    messaging:
      forward-only: true
      producer:
        producer-type: kafka
      kafka:
        topic: telegram-updates
```

In forward-only mode the bot receives each update, publishes it to Kafka, and then stops — it does **not** dispatch the update to `@BotController` handlers. Use this pattern to run a dedicated forwarding process that feeds updates to multiple consumer instances.

## Producer Ordering

With the default single-partition configuration, Kafka guarantees that messages are consumed **in the same order they were produced**. If you increase `partitions > 1`, different updates may land on different partitions and be consumed out of order by separate consumer threads. Keep `partitions: 1` unless you need horizontal scaling and can tolerate out-of-order delivery.

## Error Handling

If the Kafka broker is unreachable at publish time, `KafkaTemplate` throws a `KafkaProducerException`. Spring Kafka's default behavior is to surface this exception, which causes the `BotUpdatePublishingFilter` to propagate it through the filter chain — your `@BotExceptionHandler` or a custom `BotFilter` can catch it. To configure producer retries, set `spring.kafka.producer.retries` and `spring.kafka.producer.properties.retry.backoff.ms` in your `application.yml`.

## Customization

You can provide a custom `KafkaTemplate` by registering a `BotKafkaTemplateProvider` bean. This is useful if you need to use a specific `ProducerFactory` or custom configuration that differs from the default Spring Boot auto-configuration.

```java
@Bean
public BotKafkaTemplateProvider botKafkaTemplateProvider(ProducerFactory<String, String> producerFactory) {
    return () -> new KafkaTemplate<>(producerFactory);
}
```

## See Also

- [messaging-kafka-consumer/README.md](../messaging-kafka-consumer/README.md) — consume updates from Kafka
- [messaging-producer/README.md](../messaging-producer/README.md) — smart broker routing
- [messaging-api/README.md](../messaging-api/README.md) — SPI and custom publisher interface

