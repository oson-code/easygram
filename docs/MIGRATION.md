# Easygram — Migration Guide

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
