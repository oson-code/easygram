# Easygram — Configuration Reference

All properties are under the `easygram` prefix.

---

## Core Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.token` | `String` | — **(required)** | Telegram Bot API token from @BotFather |

---

## Telegram API URL Properties

By default, Easygram sends all API requests to `api.telegram.org`. Use these properties
to redirect to a local or self-hosted [Telegram Bot API server](https://core.telegram.org/bots/api#using-a-local-bot-api-server).

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.telegram-url.host` | `String` | — | Custom hostname; when absent, `api.telegram.org` is used |
| `easygram.telegram-url.port` | `int` | `443` | Port; only used when `host` is set |
| `easygram.telegram-url.schema` | `String` | `https` | Protocol scheme; only used when `host` is set |
| `easygram.telegram-url.test-server` | `boolean` | `false` | Set to `true` to use the Telegram test environment |

**Example — local Bot API server:**

```yaml
easygram:
  token: ${BOT_TOKEN}
  telegram-url:
    host: my-local-bot-api.example.com
    port: 8443
    schema: https
```

For complete programmatic control, declare a `BotTelegramUrlProvider` bean (takes
precedence over these properties):

```java
@Bean
public BotTelegramUrlProvider customUrl() {
    return () -> new TelegramUrl("https", "my-bot-api.example.com", 443);
}
```

*Since 0.0.6*

---

## Update Transport Properties

These properties control how updates arrive **from Telegram** to your application.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.update.transport` | `BotTransportType` | `LONG_POLLING` | Active transport. Valid values: `LONG_POLLING`, `WEBHOOK` |

---

## Long-Polling Properties

Active when `easygram.update.transport=LONG_POLLING` (the default).
Long-polling is also automatically suppressed when `easygram.messaging.type=CONSUMER`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.update.transport` | `BotTransportType` | `LONG_POLLING` | Must be `LONG_POLLING` or absent |

---

## Webhook Properties

Active when `easygram.update.transport=WEBHOOK`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.update.webhook.url` | `String` | — **(required)** | Public HTTPS URL Telegram will POST updates to |
| `easygram.update.webhook.path` | `String` | `/webhook` | URL path on your server that receives updates |
| `easygram.update.webhook.secret-token` | `String` | — | Optional secret token for request validation |
| `easygram.update.webhook.max-connections` | `int` | — | Max simultaneous HTTPS connections from Telegram (Telegram default: 40) |
| `easygram.update.webhook.drop-pending-updates` | `boolean` | `false` | Drop queued updates when registering the webhook |
| `easygram.update.webhook.unregister-on-shutdown` | `boolean` | `false` | Call `deleteWebhook` when the application shuts down |

---

## Messaging Properties

When using `messaging-api` to integrate with a message broker (Kafka or RabbitMQ):

### Role selection

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.messaging.type` | `MessagingType` | — | Application role: `PRODUCER` or `CONSUMER` |
| `easygram.messaging.forward-only` | `boolean` | `false` | `PRODUCER` only: skip local handlers, publish to broker only |

### Producer configuration (`messaging.type=PRODUCER`)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.messaging.producer.type` | `ProducerType` | — | Active publisher: `KAFKA` or `RABBIT` |

### Consumer configuration (`messaging.type=CONSUMER`)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.messaging.consumer.type` | `ConsumerType` | — | Active consumer transport: `KAFKA` or `RABBIT` |

### Kafka properties (shared by producer + consumer)

Active when `messaging.producer.type=KAFKA` or `messaging.consumer.type=KAFKA`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.messaging.kafka.topic` | `String` | `easygram-updates` | Kafka topic |
| `easygram.messaging.kafka.group-id` | `String` | `easygram-bot` | Kafka consumer group ID (consumer only) |
| `easygram.messaging.kafka.create-if-absent` | `boolean` | `true` | Auto-create topic on startup |
| `easygram.messaging.kafka.partitions` | `int` | `1` | Topic partition count (only used when creating) |
| `easygram.messaging.kafka.replication-factor` | `int` | `1` | Topic replication factor (only used when creating) |

Standard Spring Kafka properties (`spring.kafka.*`) also apply.

### RabbitMQ properties (shared by producer + consumer)

Active when `messaging.producer.type=RABBIT` or `messaging.consumer.type=RABBIT`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.messaging.rabbit.exchange` | `String` | `easygram-exchange` | RabbitMQ exchange |
| `easygram.messaging.rabbit.queue` | `String` | `easygram-updates` | RabbitMQ queue |
| `easygram.messaging.rabbit.routing-key` | `String` | `easygram.updates` | Routing key |
| `easygram.messaging.rabbit.create-if-absent` | `boolean` | `true` | Auto-declare exchange + queue on startup |

Standard Spring AMQP properties (`spring.rabbitmq.*`) also apply.

---

## i18n Properties

Active when `core-i18n` is on the classpath.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.i18n.default-locale` | `String` (language tag) | `en` | Default locale for users without a known locale |
| `easygram.i18n.basename` | `String` | `messages` | Message source basename (Spring `MessageSource` convention) |

---

## Minimal Examples

### Simple long-polling bot

```yaml
easygram:
  token: ${BOT_TOKEN}
  # update.transport defaults to LONG_POLLING — nothing else needed
```

### Webhook bot

```yaml
easygram:
  token: ${BOT_TOKEN}
  update:
    transport: WEBHOOK
    webhook:
      url: https://my-bot.example.com
      path: /webhook
      secret-token: ${WEBHOOK_SECRET}
  i18n:
    default-locale: en
```

### Producer bot (webhook → Kafka)

```yaml
easygram:
  token: ${BOT_TOKEN}
  update:
    transport: WEBHOOK
    webhook:
      url: https://my-bot.example.com
  messaging:
    type: PRODUCER
    forward-only: true
    producer:
      type: KAFKA
    kafka:
      topic: my-bot-updates
```

### Consumer bot (Kafka → handlers)

```yaml
easygram:
  token: ${BOT_TOKEN}          # still needed to send replies via Telegram API
  messaging:
    type: CONSUMER
    consumer:
      type: KAFKA
    kafka:
      topic: my-bot-updates
```

## Observability / Actuator

```yaml
logging:
  level:
    uz.osoncode.easygram: INFO

management:
  endpoints:
    web:
      exposure:
        include: health, info, easygram-bot
```
