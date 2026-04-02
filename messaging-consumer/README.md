# messaging-consumer

> Aggregator module that activates **either a Kafka or a RabbitMQ consumer** for your Telegram bot with a single dependency and one configuration property.

---

## Table of Contents

- [What It Does](#what-it-does)
- [When to Use This vs Standalone Modules](#when-to-use-this-vs-standalone-modules)
- [Module Diagram](#module-diagram)
- [Maven Dependency](#maven-dependency)
- [Quick Start](#quick-start)
  - [Kafka](#kafka-consumer)
  - [RabbitMQ](#rabbitmq-consumer)
- [Configuration Reference](#configuration-reference)
- [Auto-Created Resources](#auto-created-resources)
- [How Transport Is Auto-Configured](#how-transport-is-auto-configured)
- [Overriding Defaults](#overriding-defaults)

---

## What It Does

`messaging-consumer` bundles both [`messaging-kafka-consumer`](../messaging-kafka-consumer/README.md) and [`messaging-rabbit-consumer`](../messaging-rabbit-consumer/README.md) and routes to the correct one at startup based on `telegram.bot.messaging.consumer.consumer-type`.

- **One dependency** — no need to pick and add the individual broker module
- **One property** — switch between Kafka and RabbitMQ without changing code
- **Auto-transport** — sets `telegram.bot.transport` automatically; you don't need to set it yourself
- **Auto-resources** — creates Kafka topic / RabbitMQ exchange + queue + binding on startup (opt-out with `create-if-absent: false`)

---

## When to Use This vs Standalone Modules

| | `messaging-consumer` | `kafka-consumer` / `rabbit-consumer` |
|-|----------------------|--------------------------------------|
| **Use case** | You want a single dep and plan to switch brokers via config | You know exactly which broker you'll always use |
| **Dependencies** | Both Kafka + Rabbit client jars on classpath | Only the chosen broker's jar |
| **Transport config** | Auto-set by `consumer-type` | Must set `telegram.bot.transport` yourself |
| **Flexibility** | Switch broker with one property change | Hardcoded at dependency level |

---

## Module Diagram

```
messaging-consumer
├── depends on ──► messaging-kafka-consumer
│                    └── activates KafkaConsumerBot + KafkaBotUpdateListener
│                        when consumer-type=kafka
│
└── depends on ──► messaging-rabbit-consumer
                     └── activates RabbitConsumerBot + RabbitBotUpdateListener
                         when consumer-type=rabbit
```

**Relationship to other modules:**

```
[Telegram Bot API]
        │
        ▼
[messaging-producer]  ──publishes──►  [Kafka / RabbitMQ broker]
                                               │
                                               ▼
                                    [messaging-consumer]  ──routes to──►  [BotFilterChain]
                                               │                                  │
                                  ┌────────────┴────────────┐                     ▼
                                  ▼                         ▼              [BotDispatcher]
                           [kafka-consumer]         [rabbit-consumer]             │
                                                                                  ▼
                                                                          [@BotController handlers]
```

---

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-consumer</artifactId>
    <version>0.0.4</version>
</dependency>
```

> **Note:** You do **not** need to add `kafka-consumer` or `rabbit-consumer` separately — `messaging-consumer` pulls both in. Only the broker you configure in `consumer-type` will have its beans activated.

---

## Quick Start

### Kafka Consumer

**1. Add dependency**

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-consumer</artifactId>
    <version>0.0.4</version>
</dependency>
```

**2. Configure `application.yml`**

```yaml
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN}
    messaging:
      consumer:
        consumer-type: kafka           # ← activates Kafka consumer
    kafka-consumer:
      topic: telegram-updates          # ← Kafka topic to listen on
      create-if-absent: true           # auto-create topic (default: true)
      partitions: 1
      replication-factor: 1

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: my-bot-group
      auto-offset-reset: earliest
```

**3. Write your bot controller** (same as any other transport)

```java
@BotController
public class EchoBotController {

    @BotTextDefault
    public SendMessage onMessage(BotRequest request) {
        return SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("Echo: " + request.getUpdate().getMessage().getText())
                .build();
    }
}
```

That's it. The bot listens on the Kafka topic, deserializes `Update` objects, and routes them through the filter chain to your handlers.

---

### RabbitMQ Consumer

**1. Same dependency as above**

**2. Configure `application.yml`**

```yaml
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN}
    messaging:
      consumer:
        consumer-type: rabbit          # ← activates RabbitMQ consumer
    rabbit-consumer:
      queue: telegram-updates          # ← queue to listen on
      exchange: telegram-exchange      # exchange (default: telegram-exchange)
      routing-key: telegram.updates    # routing key (default: telegram.updates)
      create-if-absent: true           # auto-create exchange/queue/binding (default: true)

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

**3. Write your bot controller** — identical to Kafka example above.

---

## Configuration Reference

### `telegram.bot.messaging.consumer.*`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `consumer-type` | `kafka` \| `rabbit` | ✅ | Selects which broker consumer to activate |

### `telegram.bot.kafka-consumer.*` (when `consumer-type=kafka`)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `topic` | `String` | — | ✅ Kafka topic to consume updates from |
| `create-if-absent` | `boolean` | `true` | Auto-create topic on startup if it doesn't exist |
| `partitions` | `int` | `1` | Number of partitions for auto-created topic |
| `replication-factor` | `short` | `1` | Replication factor for auto-created topic |

Also requires standard Spring Kafka configuration under `spring.kafka.*`.

### `telegram.bot.rabbit-consumer.*` (when `consumer-type=rabbit`)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `queue` | `String` | — | ✅ RabbitMQ queue to consume updates from |
| `exchange` | `String` | `telegram-exchange` | Exchange to bind the queue to |
| `routing-key` | `String` | `telegram.updates` | Routing key for the binding |
| `create-if-absent` | `boolean` | `true` | Auto-create exchange, queue and binding on startup |

Also requires standard Spring RabbitMQ configuration under `spring.rabbitmq.*`.

---

## Auto-Created Resources

When `create-if-absent=true` (the default), the framework declares the broker resources as Spring beans so the underlying infrastructure (Kafka AdminClient / RabbitMQ AMQP Admin) creates them on startup if they don't already exist.

**Kafka:** Creates a topic with the configured `partitions` and `replication-factor`.

**RabbitMQ:** Creates:
- A **durable topic exchange** (`exchange` property)
- A **durable queue** (`queue` property)
- A **binding** between them using `routing-key`

Set `create-if-absent: false` to disable auto-creation (e.g. when resources are managed externally via Terraform / Ansible).

---

## How Transport Is Auto-Configured

Normally you would need to set:

```yaml
telegram:
  bot:
    transport: KAFKA_CONSUMER   # or RABBIT_CONSUMER
```

`messaging-consumer` does this automatically. When `consumer-type=kafka` it registers a `BotConfigurer` bean with `BotTransportType.KAFKA_CONSUMER`; when `consumer-type=rabbit` it registers one with `BotTransportType.RABBIT_CONSUMER`. This runs before `CoreAutoConfiguration` so the core picks it up correctly.

---

## Overriding Defaults

Every bean registered by `messaging-consumer` is guarded by `@ConditionalOnMissingBean`. To override, declare your own bean of the same type:

```java
@Bean
public KafkaConsumerBot customKafkaConsumerBot(...) {
    // custom configuration
}
```

The auto-configuration will back off and use your bean instead.

---

## Related Modules

| Module | Description |
|--------|-------------|
| [`messaging-kafka-consumer`](../messaging-kafka-consumer/README.md) | Standalone Kafka consumer (no aggregation) |
| [`messaging-rabbit-consumer`](../messaging-rabbit-consumer/README.md) | Standalone RabbitMQ consumer (no aggregation) |
| [`messaging-producer`](../messaging-producer/README.md) | Publish every update to Kafka or RabbitMQ |
| [`messaging-kafka`](../messaging-kafka/README.md) | Low-level Kafka publisher infrastructure |
| [`messaging-rabbit`](../messaging-rabbit/README.md) | Low-level RabbitMQ publisher infrastructure |
| [`core-observability`](../core-observability/README.md) | Metrics + traces + health indicators |
