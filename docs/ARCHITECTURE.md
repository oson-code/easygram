# Easygram — Architecture

## Overview

Easygram is a Spring Boot framework for building Telegram bots. It provides an
annotation-driven programming model (`@BotController`, `@BotCommand`, `@BotText`, …)
over a fully pluggable SPI so every internal component can be replaced with a custom
`@Bean`.

---

## Module Structure

```
core-api            ← SPI contracts (no Spring, no broker deps)
core                ← Engine (dispatcher, filter chain, argument resolvers, autoconfiguration)
core-chatstate      ← Optional in-memory BotChatStateService
core-i18n           ← i18n support (BotMessageSource, LocalizedReply, Locale injection)
core-observability  ← Micrometer metrics + Spring Boot Actuator endpoint
longpolling         ← Long-polling transport
webhook             ← Webhook transport (Spring MVC)
messaging-api       ← Broker integration (Kafka + RabbitMQ publishers and consumers)
spring-boot-starter ← Aggregator POM (all modules)
```

### Dependency direction

```
core-api
  └── core
        ├── core-chatstate
        ├── core-i18n
        ├── core-observability
        ├── longpolling
        ├── webhook
        └── messaging-api
              └── spring-boot-starter (POM aggregator)
```

`core-api` has **zero Spring or broker dependencies** — it is the stable SPI boundary.

---

## Request Processing Pipeline

Every incoming Telegram `Update` is processed through the following pipeline:

```
Transport (longpolling / webhook / Kafka consumer / Rabbit consumer)
  │
  ▼
BotFilterChain (DefaultBotFilterChain — ordered list of BotFilter beans)
  │
  ├── BotMdcFilter              order = MIN_VALUE      ← sets MDC: update.id, transport
  ├── BotContextSetterFilter    order = MIN_VALUE+1    ← extracts Chat, User, message text
  ├── BotObservabilityFilter    order = MIN_VALUE+2    ← Micrometer spans / metrics
  ├── BotApiMethodsSenderFilter order = MIN_VALUE+3    ← executes queued Telegram API calls
  └── (custom BotFilter beans)  any order
  │
  ▼
BotDispatcher
  │  Three-tier handler lookup (in priority order):
  │  1. State handlers   — matched by chat state AND update type/content
  │  2. Specific handlers — matched by update type/content (no state requirement)
  │  3. Default handlers  — catch-all fallback
  │
  ▼
BotMethodHandler.handle()
  │
  ▼
BotHandlerInvocationChain (ordered BotHandlerInvocationFilter list)
  │
  ├── MethodInvocationFilter      ← resolves args, validates (Bean Validation), invokes method
  ├── MarkupApplicationFilter     ← attaches keyboard to return value
  ├── ReturnTypeDispatchFilter    ← routes return value to BotReturnTypeHandler
  └── ChatStateUpdateFilter       ← applies @BotForwardChatState / @BotClearChatState
```

### Filter vs Invocation Filter

| Concept | Interface | Scope |
|---------|-----------|-------|
| `BotFilter` | Pre/post for every update | Runs once per Update |
| `BotHandlerInvocationFilter` | Around the controller method call | Runs once per matched handler |

---

## Three-Tier Handler Registry

`BotHandlerRegistry` maintains three independent lists:

| Tier | Key | Matches when |
|------|-----|-------------|
| **State handlers** | `(state, handlerKey)` | Chat is in a specific state AND update matches |
| **Specific handlers** | `handlerKey` | Update matches, no state requirement |
| **Default handlers** | — (ordered list) | Fallback when tiers 1 and 2 yield no match |

`BotDispatcher` checks tiers in order: state → specific → default. A `WARN` log is
emitted and `IllegalStateException` thrown if no handler matches any tier.

---

## Argument Resolution

`BotArgumentResolverFactory` iterates over handler method parameters and finds the
first `BotArgumentResolver` that `supportsParameter()` returns `true` for.

Built-in resolvers cover all common types:
- Telegram types: `Update`, `Message`, `User`, `Chat`, `CallbackQuery`, …
- Framework types: `BotRequest`, `BotResponse`
- Annotation-bound: `@BotCommandParam`, `@BotCallbackData`, `@BotChatStateParam`, …
- Locale injection: `java.util.Locale` (via `core-i18n`)

If **no resolver** is found for a parameter, `null` is injected and a `WARN` log is
emitted.

---

## Return Type Handling

`BotReturnTypeHandlerFactory` selects the appropriate `BotReturnTypeHandler`:

| Return type | Handler |
|-------------|---------|
| `String` | `BotStringReturnTypeHandler` — sends plain text message |
| `PlainReply` | `BotPlainReplyReturnTypeHandler` — sends with optional keyboard |
| `LocalizedReply` | `BotLocalizedReplyReturnTypeHandler` — i18n-aware send |
| `void` / `null` | `BotVoidReturnTypeHandler` — no-op |
| Custom | Register a `BotReturnTypeHandler` `@Bean` |

---

## Autoconfiguration

Each module registers its `@AutoConfiguration` class in:
```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Spring Boot 3.x discovers these automatically. Every framework bean is annotated
`@ConditionalOnMissingBean` so users can replace any default.

---

## Chat State Machine

`BotChatStateService` stores a per-chat `String` state. The default implementation
(`InMemoryBotChatStateService`) uses a `ConcurrentHashMap`.

State transitions are declared on handler methods:

```java
@BotText("register")
@BotChatState("IDLE")                  // fires only when chat is in IDLE state
@BotForwardChatState("WAITING_NAME")   // transitions to WAITING_NAME on success
public String onRegister() { … }
```

Replace the default with a Redis- or database-backed implementation by declaring your
own `BotChatStateService` `@Bean`.

---

## MDC Tracing Keys

`BotMdcFilter` (first in the filter chain) sets these MDC keys for every Update:

| Key | Value | Set when |
|-----|-------|----------|
| `bot.update.id` | `update.getUpdateId()` | Always (before chain) |
| `bot.transport` | Transport type name | Always (before chain) |
| `bot.user.id` | `user.getId()` | After context setter resolves user |
| `bot.chat.id` | `chat.getId()` | After context setter resolves chat |

All keys are cleared in `finally` at the end of filter chain execution.

Include these fields in your Logback pattern for correlated per-update logs:
```xml
<pattern>%d{HH:mm:ss} [%X{bot.update.id}] [%X{bot.chat.id}] %-5level %logger{36} - %msg%n</pattern>
```
