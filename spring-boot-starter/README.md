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
    <version>0.0.5</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("uz.osoncode.easygram:spring-boot-starter:0.0.5")
```

---

## What's Included

The starter pulls in every module transitively — you get all of these with the single dependency above:

| Module | Artifact ID | What it provides |
|---|---|---|
| Core API | `core-api` | Annotations, interfaces, model classes |
| Core Engine | `core` | Dispatching, filter pipeline, MDC tracing, argument/return-type handling |
| Chat State | `core-chatstate` | `InMemoryBotChatStateService` |
| i18n | `core-i18n` | `BotMessageSource`, `BotKeyboardFactory`, `Locale` injection |
| Long-Polling | `longpolling` | `getUpdates` polling transport (default) |
| Webhook | `webhook` | Spring MVC endpoint transport |
| Messaging | `messaging-api` | Kafka + RabbitMQ publisher and consumer transports |

**Optional runtime dependencies** (not pulled transitively — add only what you use):

| Library | Required when | Notes |
|---|---|---|
| `uz.osoncode.easygram:core-observability` | Actuator health/info or Micrometer metrics | Health indicator, info contributor, `BotObservabilityFilter` |
| `spring-boot-starter-web` | `webhook` transport | Required for the webhook MVC endpoint |
| `spring-kafka` | `KAFKA_CONSUMER` transport or Kafka publisher | Kafka client + Spring Kafka |
| `spring-boot-starter-amqp` | `RABBIT_CONSUMER` transport or RabbitMQ publisher | RabbitMQ client + Spring AMQP |

> For a lean classpath (e.g., long-polling only — no Spring MVC, no Kafka, no AMQP), depend on the specific transport module directly — see [Starter vs. Individual Modules](#starter-vs-individual-modules).

---

## Transport Selection

Only **one** transport is active at a time, controlled by `easygram.transport`:

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
easygram:
  token: ${BOT_TOKEN}
```

No other properties are required. Long-polling starts automatically.

### Webhook

```yaml
easygram:
  token: ${BOT_TOKEN}
  transport: WEBHOOK
  webhook:
    url: https://bot.example.com/webhook
    secret-token: ${WEBHOOK_SECRET}   # strongly recommended
```

### Kafka consumer

```yaml
easygram:
  token: ${BOT_TOKEN}
  transport: KAFKA_CONSUMER
  kafka-consumer:
    topic: easygram-updates

spring:
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: my-bot-group
```

### RabbitMQ consumer

```yaml
easygram:
  token: ${BOT_TOKEN}
  transport: RABBIT_CONSUMER
  rabbit-consumer:
    queue: easygram-updates

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
| `easygram.token` | — | **Required.** Bot token from @BotFather |
| `easygram.transport` | `LONG_POLLING` | Active transport: `LONG_POLLING`, `WEBHOOK`, `KAFKA_CONSUMER`, `RABBIT_CONSUMER` |
| `easygram.webhook.url` | — | Public HTTPS URL for webhook registration |
| `easygram.webhook.path` | `/webhook` | Local request path Spring MVC listens on |
| `easygram.webhook.secret-token` | — | Header validation secret (recommended) |
| `easygram.webhook.max-connections` | — | Max simultaneous Telegram connections (1–100) |
| `easygram.webhook.drop-pending-updates` | `false` | Drop queued updates on webhook registration |
| `easygram.webhook.unregister-on-shutdown` | `false` | Call `DeleteWebhook` on graceful shutdown |
| `easygram.kafka-consumer.topic` | — | Kafka topic to consume updates from |
| `easygram.kafka-consumer.create-if-absent` | `true` | Auto-create topic on startup |
| `easygram.kafka-consumer.partitions` | `1` | Partitions for auto-created topic |
| `easygram.kafka-consumer.replication-factor` | `1` | Replication factor for auto-created topic |
| `easygram.rabbit-consumer.queue` | — | RabbitMQ queue to consume updates from |
| `easygram.rabbit-consumer.exchange` | `easygram-exchange` | Exchange for auto-created queue binding |
| `easygram.rabbit-consumer.routing-key` | `easygram.updates` | Routing key for auto-created binding |
| `easygram.rabbit-consumer.create-if-absent` | `true` | Auto-create exchange/queue/binding |
| `easygram.i18n.default-locale` | `en` | Fallback locale when user locale cannot be resolved |

---

## Starter vs. Individual Modules

Use individual modules for a **minimal dependency tree**:

```xml
<!-- Long-polling only — no Spring MVC, no Kafka, no AMQP -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>longpolling</artifactId>
    <version>0.0.5</version>
</dependency>
```

The starter is convenient but brings `spring-boot-starter-web`, `spring-kafka`, and `spring-boot-starter-amqp` onto your classpath even if you use only one transport. For production bots where classpath size matters, depend on the specific transport module directly.

| Scenario | Recommended dependency |
|---|---|
| Development / quick start | `spring-boot-starter` (this module) |
| Long-polling only | `longpolling` |
| Webhook only | `webhook` |
| Kafka consumer only | `messaging-api` + `spring-kafka` |
| RabbitMQ consumer only | `messaging-api` + `spring-boot-starter-amqp` |
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
| Messaging (Kafka + RabbitMQ) | [messaging-api/README.md](../messaging-api/README.md) |
| Samples | [samples/README.md](../samples/README.md) |

---

## See Also

- [Root README](../README.md) — project overview, features, quick start
- [samples/README.md](../samples/README.md) — runnable example applications for each transport

