# Easygram — Configuration Reference

All properties are under the `easygram` prefix.

---

## Core Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.token` | `String` | — **(required)** | Telegram Bot API token from @BotFather |
| `easygram.transport` | `TransportType` enum | `LONG_POLLING` | Active transport. Valid values: `LONG_POLLING`, `WEBHOOK`, `KAFKA_CONSUMER`, `RABBIT_CONSUMER` |

---

## Long-Polling Properties

Active when `easygram.transport=LONG_POLLING`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.long-polling.timeout` | `int` | `30` | Long-polling timeout in seconds |
| `easygram.long-polling.limit` | `int` | `100` | Max updates per poll request |
| `easygram.long-polling.allowed-updates` | `List<String>` | all | Update types to receive (e.g. `message`, `callback_query`) |

---

## Webhook Properties

Active when `easygram.transport=WEBHOOK`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.webhook.url` | `String` | — **(required)** | Public HTTPS URL Telegram will POST updates to |
| `easygram.webhook.path` | `String` | `/telegram/webhook` | URL path on your server that receives updates |
| `easygram.webhook.secret-token` | `String` | — | Optional secret token for request validation |
| `easygram.webhook.max-connections` | `int` | `40` | Max simultaneous HTTPS connections from Telegram |

---

## Kafka Consumer Properties

Active when `easygram.transport=KAFKA_CONSUMER`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.kafka-consumer.topic` | `String` | `easygram-updates` | Kafka topic to consume updates from |
| `easygram.kafka-consumer.group-id` | `String` | `easygram-bot` | Kafka consumer group ID |

Standard Spring Kafka properties (`spring.kafka.*`) also apply.

---

## RabbitMQ Consumer Properties

Active when `easygram.transport=RABBIT_CONSUMER`.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.rabbit-consumer.queue` | `String` | `easygram-updates` | RabbitMQ queue to consume updates from |
| `easygram.rabbit-consumer.exchange` | `String` | `easygram-exchange` | RabbitMQ exchange |
| `easygram.rabbit-consumer.routing-key` | `String` | `easygram-updates` | Routing key |

Standard Spring AMQP properties (`spring.rabbitmq.*`) also apply.

---

## Messaging Publisher Properties

When using `messaging-api` to publish updates from a transport to a broker:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.messaging.producer.type` | `ProducerType` enum | — | Active publisher: `KAFKA` or `RABBIT` |
| `easygram.messaging.consumer.type` | `ConsumerType` enum | — | Active consumer: `KAFKA` or `RABBIT` |

---

## i18n Properties

Active when `core-i18n` is on the classpath.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `easygram.i18n.default-locale` | `String` (language tag) | `en` | Default locale for users without a known locale |
| `easygram.i18n.basename` | `String` | `messages` | Message source basename (Spring `MessageSource` convention) |

---

## Minimal Example

```yaml
easygram:
  token: ${BOT_TOKEN}
  transport: LONG_POLLING
  i18n:
    default-locale: en
```

## Full Example

```yaml
easygram:
  token: ${BOT_TOKEN}
  transport: WEBHOOK

  webhook:
    url: https://my-bot.example.com
    path: /telegram/webhook
    secret-token: ${WEBHOOK_SECRET}

  i18n:
    default-locale: en
    basename: i18n/messages

logging:
  level:
    uz.osoncode.easygram: INFO

management:
  endpoints:
    web:
      exposure:
        include: health, info, telegram-bot
```
