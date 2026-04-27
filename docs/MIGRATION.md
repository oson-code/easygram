# Easygram — Migration Guide

## 0.0.5 → 0.0.6

Feature release — **no breaking changes**. All existing 0.0.5 code and configuration works unchanged.

### New: `@BotParseMode` annotation

Attach `@BotParseMode("HTML")`, `@BotParseMode("MarkdownV2")`, or `@BotParseMode("Markdown")`
to any handler method. Works with all return types: `String`, `PlainReply`,
`PlainTextTemplate`, `LocalizedReply`, `LocalizedTemplate`.

```java
@BotParseMode("HTML")
@BotCommand("/start")
public String start(User user) {
    return "<b>Hello, " + user.getFirstName() + "!</b>";
}
```

All `MarkupAware` reply types also gained `.withParseMode(String)` for runtime control:

```java
return PlainReply.of("<b>text</b>").withParseMode("HTML");
```

### New: Configurable Telegram API URL (`easygram.telegram-url.*`)

Use a local or self-hosted Bot API server:

```yaml
easygram:
  telegram-url:
    host: my-local-bot-api.example.com
    port: 8443
    schema: https
    test-server: false
```

All fields are optional. When `host` is absent, the standard `api.telegram.org` is used.

### New: Messaging factory provider SPI

Register a bean of any of the three new interfaces to supply a custom factory:

| Interface | Replaces |
|---|---|
| `BotKafkaProducerFactoryProvider` | Kafka `ProducerFactory` |
| `BotKafkaConsumerFactoryProvider` | Kafka `ConsumerFactory` |
| `BotRabbitConnectionFactoryProvider` | RabbitMQ `ConnectionFactory` |

All three are `@ConditionalOnMissingBean` — declare only the ones you need.

Topic and exchange properties are now fully optional (defaults: `easygram-updates`,
`easygram-exchange`).

**Full details:** [Migrating from 0.0.5 to 0.0.6](../docs/migration/0.0.5-to-0.0.6)

---

## 0.0.4 → 0.0.5

### Property namespace rename (`telegram.bot` → `easygram`)

All configuration properties have been renamed. Find and replace in your configuration files:

```
telegram.bot.token          → easygram.token
telegram.bot.transport      → easygram.transport
telegram.bot.webhook.*      → easygram.webhook.*
telegram.bot.i18n.*         → easygram.i18n.*
telegram.bot.messaging.*    → easygram.messaging.*
telegram.bot.kafka-consumer.* → easygram.kafka-consumer.*
telegram.bot.rabbit-consumer.* → easygram.rabbit-consumer.*
```

Default values also renamed: `telegram-updates` → `easygram-updates`, `telegram-exchange` → `easygram-exchange`, `telegram.updates` → `easygram.updates`.

Observability metric/span renamed: `telegram.bot.update` → `easygram.update`.

### Messaging module consolidation

Six fragmented messaging modules have been merged into the single `messaging-api` module.

#### Old dependencies (remove all of these)

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-kafka</artifactId>
</dependency>
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-rabbit</artifactId>
</dependency>
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-producer</artifactId>
</dependency>
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-consumer</artifactId>
</dependency>
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-kafka-consumer</artifactId>
</dependency>
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-rabbit-consumer</artifactId>
</dependency>
```

#### New dependency (single replacement)

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-api</artifactId>
    <version>0.0.5</version>
</dependency>
```

Or use the starter which includes everything:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.5</version>
</dependency>
```

#### Package names — no change

All class package names remain unchanged. No import statements need updating:

```
uz.osoncode.easygram.messaging.*                  ← unchanged
uz.osoncode.easygram.messaging.kafka.*            ← unchanged
uz.osoncode.easygram.messaging.rabbit.*           ← unchanged
uz.osoncode.easygram.messaging.producer.*         ← unchanged
uz.osoncode.easygram.messaging.consumer.*         ← unchanged
```

---

### BotFilterOrder constant values changed

`BotFilterOrder` constants were shifted to make room for the new `MDC_CONTEXT` filter.

| Constant | 0.0.4 value | 0.0.5 value |
|----------|-------------|-------------|
| `MDC_CONTEXT` | *(did not exist)* | `Integer.MIN_VALUE` |
| `CONTEXT_SETTER` | `Integer.MIN_VALUE` | `Integer.MIN_VALUE + 1` |
| `OBSERVATION` | `Integer.MIN_VALUE + 1` | `Integer.MIN_VALUE + 2` |
| `API_SENDER` | `Integer.MIN_VALUE + 2` | `Integer.MIN_VALUE + 3` |

**Recommendation:** Always reference constants by name (`BotFilterOrder.CONTEXT_SETTER`)
rather than hardcoding integer values.

---

### New MDC keys in every log line

`BotMdcFilter` is now automatically registered at the start of the filter chain. Custom
`BotFilter` beans automatically have `bot.update.id`, `bot.transport`, `bot.user.id`, and
`bot.chat.id` in the MDC — no code change required.

To surface these in log output, update your Logback pattern:

```xml
<pattern>%d [upd:%X{bot.update.id}] [chat:%X{bot.chat.id}] %-5level %logger{36} - %msg%n</pattern>
```

---

### Configuration API redesign (transport + messaging)

The configuration API has been redesigned to separate **update transport** (how updates
arrive from Telegram) from **broker integration** (publishing/consuming via Kafka or RabbitMQ).

#### Property rename table

| Old (0.0.5-pre) | New |
|---|---|
| `easygram.transport=LONG_POLLING` | `easygram.update.transport=LONG_POLLING` (or omit — it is the default) |
| `easygram.transport=WEBHOOK` | `easygram.update.transport=WEBHOOK` |
| `easygram.transport=KAFKA_CONSUMER` | `easygram.messaging.type=CONSUMER` + `easygram.messaging.consumer.type=KAFKA` |
| `easygram.transport=RABBIT_CONSUMER` | `easygram.messaging.type=CONSUMER` + `easygram.messaging.consumer.type=RABBIT` |
| `easygram.webhook.url` | `easygram.update.webhook.url` |
| `easygram.webhook.path` | `easygram.update.webhook.path` |
| `easygram.webhook.secret-token` | `easygram.update.webhook.secret-token` |
| `easygram.webhook.max-connections` | `easygram.update.webhook.max-connections` |
| `easygram.webhook.drop-pending-updates` | `easygram.update.webhook.drop-pending-updates` |
| `easygram.webhook.unregister-on-shutdown` | `easygram.update.webhook.unregister-on-shutdown` |
| `easygram.messaging.producer.producer-type=kafka` | `easygram.messaging.type=PRODUCER` + `easygram.messaging.producer.type=KAFKA` |
| `easygram.messaging.producer.producer-type=rabbit` | `easygram.messaging.type=PRODUCER` + `easygram.messaging.producer.type=RABBIT` |
| `easygram.kafka-consumer.*` | `easygram.messaging.kafka.*` |
| `easygram.rabbit-consumer.*` | `easygram.messaging.rabbit.*` |
| `easygram.messaging.kafka.*` | `easygram.messaging.kafka.*` *(unchanged — shared now)* |
| `easygram.messaging.rabbit.*` | `easygram.messaging.rabbit.*` *(unchanged — shared now)* |

#### `BotTransportType` enum changes

`KAFKA_CONSUMER` and `RABBIT_CONSUMER` have been **removed** from `BotTransportType`.
Consumer bots no longer set `update.transport`; instead they set `messaging.type=CONSUMER`.

If you referenced these enum values directly in code, replace them:

```java
// Old
BotTransportType.KAFKA_CONSUMER

// New — there is no enum constant; check via messaging properties instead
// easygram.messaging.type=CONSUMER + easygram.messaging.consumer.type=KAFKA
```

#### Consumer bot example (before / after)

```yaml
# ── Before ──────────────────────────────────────
easygram:
  transport: KAFKA_CONSUMER
  kafka-consumer:
    topic: my-updates
    group-id: my-group

# ── After ───────────────────────────────────────
easygram:
  messaging:
    type: CONSUMER
    consumer:
      type: KAFKA
    kafka:
      topic: my-updates
      group-id: my-group
```

#### Producer bot example (before / after)

```yaml
# ── Before ──────────────────────────────────────
easygram:
  transport: WEBHOOK
  webhook:
    url: https://example.com/bot
  messaging:
    producer:
      producer-type: kafka
    kafka:
      topic: my-updates

# ── After ───────────────────────────────────────
easygram:
  update:
    transport: WEBHOOK
    webhook:
      url: https://example.com/bot
  messaging:
    type: PRODUCER
    producer:
      type: KAFKA
    kafka:
      topic: my-updates
```

---

## 0.0.3 → 0.0.4

Version 0.0.4 introduced **Dynamic Callback Queries** (`@BotDynamicCallbackQuery`,
`BotDynamicCallbackData`, `BotDynamicCallbackQueryService`). No breaking changes — all
existing code compiles without modification. The only action required is updating custom
`BotKeyboardFactory` beans to accept `BotDynamicCallbackQueryService` if you use the
new `dynamicRow` builder method.

See the full details in the [Docusaurus migration page](../docs/migration/0.0.3-to-0.0.4).

---

## Earlier versions

No prior migration guides are available.
