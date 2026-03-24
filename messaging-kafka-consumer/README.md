# kafka-consumer

> Kafka consumer transport for the Easygram framework.
> Receives Telegram updates from a Kafka topic and feeds them into the bot handler pipeline.

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-kafka-consumer</artifactId>
    <version>0.0.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

## What It Does

Activates when `telegram.bot.transport=KAFKA_CONSUMER`. Reads JSON-serialised `Update` objects from the configured Kafka topic using a `@KafkaListener` and deserialises each message before passing it to `Bot.handleUpdate()`. This lets you decouple update ingestion (handled by a separate producer process) from update processing (your bot logic).

## Architecture

```
[messaging-kafka (producer side)]
         │
         ▼ JSON Update
[Kafka Topic: telegram-updates]
         │
         ▼  @KafkaListener
[KafkaBotUpdateListener]
         │ deserialise Update
         ▼
[Bot.handleUpdate()]
         │
         ▼
[BotFilter chain → BotDispatcher → @BotController handlers]
```

## Transport Selection

Set `telegram.bot.transport=KAFKA_CONSUMER` to activate this transport.

## Configuration

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: KAFKA_CONSUMER
    kafka-consumer:
      topic: telegram-updates
      create-if-absent: true          # default: true
      partitions: 1                   # default: 1
      replication-factor: 1           # default: 1

spring:
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: my-bot-group
      auto-offset-reset: earliest
```

## All Properties

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.token` | ✅ | — | Bot token (shared across transports) |
| `telegram.bot.kafka-consumer.topic` | ✅ | — | Topic to consume from |
| `telegram.bot.kafka-consumer.create-if-absent` | ❌ | `true` | Auto-create the topic if absent |
| `telegram.bot.kafka-consumer.partitions` | ❌ | `1` | Partitions for auto-created topic |
| `telegram.bot.kafka-consumer.replication-factor` | ❌ | `1` | Replication factor for auto-created topic |
| `spring.kafka.bootstrap-servers` | ✅ | — | Kafka broker addresses |
| `spring.kafka.consumer.group-id` | ✅ | — | Consumer group |
| `spring.kafka.consumer.auto-offset-reset` | ❌ | `latest` | Offset reset policy |

## Concurrency Model

Each Kafka message is consumed sequentially by the `@KafkaListener` thread and then handed off to the `ExecutorService` provided by `BotExecutorServiceProvider` (default: `newSingleThreadExecutor()`). This means update processing is single-threaded by default.

To handle updates concurrently, declare a custom executor service provider:

```java
@Bean
public BotExecutorServiceProvider botExecutorServiceProvider() {
    ExecutorService pool = Executors.newFixedThreadPool(4);
    return () -> pool;
}
```

> **Important:** if your handlers share state or write to a shared resource, ensure thread safety when using a multi-threaded executor.

## Consumer Group Strategy

The `spring.kafka.consumer.group-id` identifies which consumer group this bot belongs to. All instances of the same bot should share the **same** group ID so that each Kafka partition is assigned to exactly one instance — preventing duplicate update processing across replicas.

If you run multiple independent bot services (e.g. different bots, or a fan-out scenario), give each a distinct `group-id`.

## Deserialization Error Handling

If a Kafka message cannot be deserialized to an `Update` (e.g. malformed JSON), the `KafkaBotUpdateListener` propagates the error, which will cause the `@KafkaListener` to retry (according to your Spring Kafka error handler config) and potentially route the message to a Dead Letter Topic (DLT) if one is configured.

To configure a DLT:

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
    listener:
      ack-mode: RECORD
```

Then define a `DefaultErrorHandler` with a `DeadLetterPublishingRecoverer` bean in your application configuration.

## Customizing Infrastructure

Every infrastructure concern is exposed as a separate `@ConditionalOnMissingBean` provider bean.
Declare only the ones you need to override — everything else keeps its default.

### Available providers

| Provider interface | Default | Override example use-case |
|---|---|---|
| `BotTelegramClientProvider` | `OkHttpTelegramClient` | Custom client implementation |
| `BotExecutorServiceProvider` | `newSingleThreadExecutor()` | Fixed thread pool for update handling |
| `BotOkHttpClientProvider` | `new OkHttpClient()` | Custom timeouts / proxy |
| `BotTelegramUrlProvider` | `TelegramUrl.DEFAULT_URL` | Test / local Telegram mock |
| `BotObjectMapperProvider` | shared Spring `ObjectMapper` | Custom serialisation modules |

### Example: fixed thread pool for concurrent update handling

```java
@Configuration
public class BotConfig {

    @Bean
    public BotExecutorServiceProvider botExecutorServiceProvider() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        return () -> pool;
    }
}
```

Declaring any `@Bean` of a provider type replaces only that provider; all other defaults remain.

## See Also

- [messaging-kafka/README.md](../messaging-kafka/README.md) — publish updates to Kafka
- [messaging-producer/README.md](../messaging-producer/README.md) — smart broker-routing producer
- [samples/README.md](../samples/README.md) — runnable sample apps (including forward-only producer examples)

