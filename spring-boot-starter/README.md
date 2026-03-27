# spring-boot-starter

> One-stop dependency for the Easygram framework.

This starter pulls in the core engine, all transport modules, and i18n support with a single Maven or Gradle dependency. It is the recommended starting point for most bots.

---

## Table of Contents

- [Maven Dependency](#maven-dependency)
- [What's Included](#whats-included)
- [Transport Selection](#transport-selection)
- [Minimal Configuration](#minimal-configuration)
- [All Configuration Properties](#all-configuration-properties)
- [Starter vs. Individual Modules](#starter-vs-individual-modules)
- [Module Documentation](#module-documentation)
- [See Also](#see-also)

---

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.2</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("uz.osoncode.easygram:spring-boot-starter:0.0.1")
```

---

## What's Included

The starter pulls in every module transitively — you get all of these with the single dependency above:

| Module | Artifact ID | What it provides |
|---|---|---|
| Core API | `core-api` | Annotations, interfaces, model classes |
| Core Engine | `core` | Dispatching, filter pipeline, argument/return-type handling |
| Chat State | `core-chatstate` | `InMemoryBotChatStateService` |
| i18n | `core-i18n` | `BotMessageSource`, `BotKeyboardFactory`, `Locale` injection |
| Long-Polling | `longpolling` | `getUpdates` polling transport (default) |
| Webhook | `webhook` | Spring MVC endpoint transport |
| Kafka Consumer | `messaging-kafka-consumer` | `@KafkaListener` consumer transport |
| RabbitMQ Consumer | `messaging-rabbit-consumer` | `@RabbitListener` consumer transport |
| Messaging Consumer | `messaging-consumer` | Aggregator for Kafka/Rabbit consumer selection |

**Runtime dependencies brought in transitively:**

| Library | Brought in by | Notes |
|---|---|---|
| `spring-boot-starter-web` | `webhook` | Required for the webhook MVC endpoint |
| `spring-kafka` | `messaging-kafka-consumer` | Kafka client + Spring Kafka |
| `spring-boot-starter-amqp` | `messaging-rabbit-consumer` | RabbitMQ client + Spring AMQP |

> If your bot only uses long-polling and you want a lean classpath (no Spring MVC, no Kafka, no AMQP), use individual modules instead — see [Starter vs. Individual Modules](#starter-vs-individual-modules).

---

## Transport Selection

Only **one** transport is active at a time, controlled by `telegram.bot.transport`:

| Value | Default? | Description |
|---|---|---|
| `LONG_POLLING` | ✅ | Polls `getUpdates` API — no public URL required |
| `WEBHOOK` | | Spring MVC endpoint — requires a public HTTPS URL |
| `KAFKA_CONSUMER` | | Consumes `Update` JSON from a Kafka topic |
| `RABBIT_CONSUMER` | | Consumes `Update` JSON from a RabbitMQ queue |

---

## Minimal Configuration

### Long-polling (default)

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
```

No other properties are required. Long-polling starts automatically.

### Webhook

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: WEBHOOK
    webhook:
      url: https://bot.example.com/webhook
      secret-token: ${WEBHOOK_SECRET}   # strongly recommended
```

### Kafka consumer

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: KAFKA_CONSUMER
    kafka-consumer:
      topic: telegram-updates

spring:
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: my-bot-group
```

### RabbitMQ consumer

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: RABBIT_CONSUMER
    rabbit-consumer:
      queue: telegram-updates

spring:
  rabbitmq:
    host: rabbitmq
    username: guest
    password: guest
```

---

## All Configuration Properties

| Property | Default | Description |
|---|---|---|
| `telegram.bot.token` | — | **Required.** Bot token from @BotFather |
| `telegram.bot.transport` | `LONG_POLLING` | Active transport: `LONG_POLLING`, `WEBHOOK`, `KAFKA_CONSUMER`, `RABBIT_CONSUMER` |
| `telegram.bot.webhook.url` | — | Public HTTPS URL for webhook registration |
| `telegram.bot.webhook.path` | `/webhook` | Local request path Spring MVC listens on |
| `telegram.bot.webhook.secret-token` | — | Header validation secret (recommended) |
| `telegram.bot.webhook.max-connections` | — | Max simultaneous Telegram connections (1–100) |
| `telegram.bot.webhook.drop-pending-updates` | `false` | Drop queued updates on webhook registration |
| `telegram.bot.webhook.unregister-on-shutdown` | `false` | Call `DeleteWebhook` on graceful shutdown |
| `telegram.bot.kafka-consumer.topic` | — | Kafka topic to consume updates from |
| `telegram.bot.kafka-consumer.create-if-absent` | `true` | Auto-create topic on startup |
| `telegram.bot.kafka-consumer.partitions` | `1` | Partitions for auto-created topic |
| `telegram.bot.kafka-consumer.replication-factor` | `1` | Replication factor for auto-created topic |
| `telegram.bot.rabbit-consumer.queue` | — | RabbitMQ queue to consume updates from |
| `telegram.bot.rabbit-consumer.exchange` | `telegram-exchange` | Exchange for auto-created queue binding |
| `telegram.bot.rabbit-consumer.routing-key` | `telegram.updates` | Routing key for auto-created binding |
| `telegram.bot.rabbit-consumer.create-if-absent` | `true` | Auto-create exchange/queue/binding |
| `telegram.bot.i18n.default-locale` | `en` | Fallback locale when user locale cannot be resolved |

---

## Starter vs. Individual Modules

Use individual modules for a **minimal dependency tree**:

```xml
<!-- Long-polling only — no Spring MVC, no Kafka, no AMQP -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>longpolling</artifactId>
    <version>0.0.2</version>
</dependency>
```

The starter is convenient but brings `spring-boot-starter-web`, `spring-kafka`, and `spring-boot-starter-amqp` onto your classpath even if you use only one transport. For production bots where classpath size matters, depend on the specific transport module directly.

| Scenario | Recommended dependency |
|---|---|
| Development / quick start | `spring-boot-starter` (this module) |
| Long-polling only | `longpolling` |
| Webhook only | `webhook` |
| Kafka consumer only | `messaging-kafka-consumer` |
| RabbitMQ consumer only | `messaging-rabbit-consumer` |
| Custom extension module | `core-api` only |

---

## Module Documentation

| Module | Documentation |
|---|---|
| Core API | [core-api/README.md](../core-api/README.md) |
| Core Engine | [core/README.md](../core/README.md) |
| Long-Polling | [longpolling/README.md](../longpolling/README.md) |
| Webhook | [webhook/README.md](../webhook/README.md) |
| Chat State | [core-chatstate/README.md](../core-chatstate/README.md) |
| i18n | [core-i18n/README.md](../core-i18n/README.md) |
| Observability | [core-observability/README.md](../core-observability/README.md) |
| Kafka Consumer | [messaging-kafka-consumer/README.md](../messaging-kafka-consumer/README.md) |
| RabbitMQ Consumer | [messaging-rabbit-consumer/README.md](../messaging-rabbit-consumer/README.md) |
| Samples | [samples/README.md](../samples/README.md) |

---

## See Also

- [Root README](../README.md) — project overview, features, quick start
- [samples/README.md](../samples/README.md) — runnable example applications for each transport

