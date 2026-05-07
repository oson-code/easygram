# Easygram — Migration Guide

## 0.0.6 → 0.0.7

### BREAKING: `core-observability` no longer pulled transitively by `spring-boot-starter`

`core-observability` is now declared with `<optional>true</optional>` in
`spring-boot-starter/pom.xml`. Projects that relied on it being pulled in automatically
will no longer have `BotHealthIndicator`, `BotInfoContributor`, or `BotObservabilityFilter`
without an explicit dependency.

**Affected if**: you see `BotHealthIndicator` contributions missing from `/actuator/health`,
or if your application previously failed to start without `micrometer-core` on the classpath.

**Migration**: Add the module explicitly to restore the previous behavior:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>core-observability</artifactId>
    <version>0.0.7</version>
</dependency>
```

You will also need `spring-boot-actuator` (usually via `spring-boot-starter-actuator`) and
`micrometer-core` (usually pulled by a registry implementation like `micrometer-registry-prometheus`).

### New (non-breaking): `BotChatStateMetrics` SPI

A new `BotChatStateMetrics` public interface has been added to `core-chatstate`. It
decouples the in-memory chat state service from any specific metrics library. When
`micrometer-core` is on the classpath, `MicrometerBotChatStateMetrics` is wired
automatically and emits the following counters:

| Metric | Tags | Description |
|---|---|---|
| `easygram.chatstate.get` | `result=hit\|miss` | State look-up results |
| `easygram.chatstate.set` | — | State writes |
| `easygram.chatstate.clear` | — | State removals |

No action required — this is purely additive.

---

## 0.0.5 → 0.0.6

### BREAKING: `PlainTextTemplate` removed

`PlainTextTemplate` and its handler `BotPlainTextTemplateReturnTypeHandler` have been
removed. Migrate to `PlainReply` with built-in `MessageFormat` arg support:

| Before (0.0.5) | After (0.0.6) |
|---|---|
| `PlainTextTemplate.of("Hello, #{0}!", name)` | `PlainReply.of("Hello, {0}!", name)` |
| `PlainTextTemplate.of("#{0} msgs", count)` | `PlainReply.of("{0} msgs", count)` |

Token notation changes: **`#{n}` → `{n}`** (standard Java `MessageFormat` syntax).

```java
// Before
return PlainTextTemplate.of("Welcome, #{0}! You have #{1} messages.", name, count);

// After
return PlainReply.of("Welcome, {0}! You have {1} messages.", name, count);
```

The builder and wither are analogous:

```java
PlainReply.builder().text("Hi, {0}!").args(name).build();
PlainReply.of("Hi, {0}!").withArgs(name);
```

### BREAKING: `LocalizedTemplate` removed

`LocalizedTemplate` and its handler `BotLocalizedTemplateReturnTypeHandler` have been
removed. Message bundle files must also be updated to use standard `MessageFormat` `{n}`
placeholders instead of the old `#{n}` notation.

**Option 1 — Use `LocalizedReply` with args** (single key, positional args):

```java
// messages/bot_en.properties: register.complete=Registration complete. City: {0}

// Before
return LocalizedTemplate.of("${register.complete} #{0}", city);

// After (single-key with arg)
return LocalizedReply.of("register.complete", city);
```

**Option 2 — Concatenate via `BotMessageSource`** (multi-key):

```java
// Before
return LocalizedTemplate.of("${welcome.title}\n\n${welcome.body}\n\nHello, #{0}!", name);

// After — inject BotMessageSource, resolve each key, combine
@BotController
@RequiredArgsConstructor
public class WelcomeController {
    private final BotMessageSource messageSource;

    @BotCommand("/start")
    public String onStart(User user, BotRequest request) {
        return messageSource.getMessage("welcome.title", request) + "\n\n"
             + messageSource.getMessage("welcome.body", request) + "\n\n"
             + "Hello, " + user.getFirstName() + "!";
    }
}
```

**Bundle file migration** — change `#{n}` → `{n}` in all `.properties` files:

```properties
# Before (0.0.5)
greeting=Hello, #{0}!
register.complete=Complete! City: #{0}

# After (0.0.6)
greeting=Hello, {0}!
register.complete=Complete! City: {0}
```

### New: sendMessage delivery options on `PlainReply` and `LocalizedReply`

Five new optional fields added (additive, fully backward-compatible):

| Field | Method | Purpose |
|---|---|---|
| `disableNotification` | `.withDisableNotification(bool)` | Send silently |
| `protectContent` | `.withProtectContent(bool)` | Disable forwarding/saving |
| `messageThreadId` | `.withMessageThreadId(id)` | Forum topic thread |
| `replyParameters` | `.withReplyParameters(rp)` | Reply to specific message |
| `linkPreviewOptions` | `.withLinkPreviewOptions(lp)` | Link preview control |

```java
return PlainReply.of("Quiet update.").withDisableNotification(true);
return LocalizedReply.of("welcome").withProtectContent(true);
```

### New: `@BotParseMode` annotation

Attach `@BotParseMode("HTML")`, `@BotParseMode("MarkdownV2")`, or `@BotParseMode("Markdown")`
to any handler method. Works with all return types: `String`, `PlainReply`, `LocalizedReply`.

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
| `EasygramKafkaProducerFactoryProvider` | Kafka `ProducerFactory` |
| `EasygramKafkaConsumerFactoryProvider` | Kafka `ConsumerFactory` |
| `EasygramRabbitConnectionFactoryProvider` | RabbitMQ `ConnectionFactory` |

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
| `easygram.transport=KAFKA_CONSUMER` | `easygram.update.transport=KAFKA_CONSUMER` *(prefix changed only)* |
| `easygram.transport=RABBIT_CONSUMER` | `easygram.update.transport=RABBIT_CONSUMER` *(prefix changed only)* |
| `easygram.webhook.url` | `easygram.update.webhook.url` |
| `easygram.webhook.path` | `easygram.update.webhook.path` |
| `easygram.webhook.secret-token` | `easygram.update.webhook.secret-token` |
| `easygram.webhook.max-connections` | `easygram.update.webhook.max-connections` |
| `easygram.webhook.drop-pending-updates` | `easygram.update.webhook.drop-pending-updates` |
| `easygram.webhook.unregister-on-shutdown` | `easygram.update.webhook.unregister-on-shutdown` |
| `easygram.messaging.producer.producer-type=kafka` | `easygram.messaging.producer.type=KAFKA` |
| `easygram.messaging.producer.producer-type=rabbit` | `easygram.messaging.producer.type=RABBIT` |
| `easygram.kafka-consumer.*` | `easygram.messaging.kafka.*` |
| `easygram.rabbit-consumer.*` | `easygram.messaging.rabbit.*` |
| `easygram.messaging.kafka.*` | `easygram.messaging.kafka.*` *(unchanged — shared now)* |
| `easygram.messaging.rabbit.*` | `easygram.messaging.rabbit.*` *(unchanged — shared now)* |

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
  update:
    transport: KAFKA_CONSUMER
  messaging:
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
