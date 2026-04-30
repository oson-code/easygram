---
id: whatsnew-0.0.7
title: What's New in 0.0.7
description: Summary of all new features, reliability improvements, and migration notes for Easygram 0.0.7 — duplicate handler detection, webhook security, startup validation, observability enhancements, and @BotStartTrigger lambda support.
keywords: [easygram 0.0.7, telegram bot java 0.0.7 changelog, duplicate handler detection, webhook security, startup validation, BotStartTrigger lambda, easygram reliability]
---

# What's New in 0.0.7

A summary of every new feature and reliability improvement in Easygram **0.0.7**. There are
**no breaking changes** — all additions are either additive or opt-in. For migration instructions
see the [0.0.6 → 0.0.7 migration guide](./migration/0.0.6-to-0.0.7.md).

---

## 1. Duplicate Handler Condition Detection at Startup

Easygram now detects ambiguous handler registrations at startup, analogous to Spring MVC
throwing `IllegalStateException` when two `@GetMapping` methods share the same path.

If two `@BotController` methods share the same routing condition — same annotation type,
same value, and same effective `@BotChatState` — the application fails to start with a
`BeanCreationException` that names both conflicting methods:

```
Duplicate handler mapping detected for condition [specific:BotCommand:/start:state:]:
  First  : com.example.MyController#onStart
  Second : com.example.OtherController#onStartDuplicate
Remove or rename one of the conflicting handler methods.
```

### What counts as a duplicate

Two handlers are duplicates when they would match the **exact same incoming update** — i.e.,
all three of the following are identical:

| Dimension | Examples |
|---|---|
| Annotation type | Both `@BotCommand`, both `@BotText`, both `@BotDefaultHandler`, etc. |
| Annotation value | Both `"/start"`, both `"hello"`, both `"btn_ok"` |
| Effective `@BotChatState` | Both have no state, or both have the same state(s) |

### What is NOT a duplicate

```java
// ✅ Different @BotChatState — different tiers, no conflict
@BotCommand("/start")
@BotChatState("ONBOARDING")
public String onStartInOnboarding() { ... }

@BotCommand("/start")
@BotChatState("MAIN_MENU")
public String onStartInMainMenu() { ... }

// ✅ Different values — no conflict
@BotCommand("/start")
public String onStart() { ... }

@BotCommand("/help")
public String onHelp() { ... }

// ✅ Multi-value: only overlapping individual values conflict
@BotCommand({"/start", "/begin"})
public String onStart() { ... }
// ❌ Adding @BotCommand("/start") anywhere = conflict on "/start"
```

### Cross-controller detection

Duplicate detection spans **all** `@BotController` beans — placing conflicting methods in
different controllers does not avoid the error.

### Using `@BotOrder` for priority

If you intentionally want two handlers for the same condition with priority-based dispatch,
use distinct `@BotOrder` values. The lower value is checked first:

```java
// This is valid: same condition, different priority
@BotCommand("/admin")
@BotOrder(1)
public String onAdminForSuperUsers(User user) { ... }

@BotCommand("/admin")
@BotOrder(100)
public String onAdminFallback() { ... }
```

:::note
`@BotOrder` is intentionally excluded from the conflict key. Two handlers with different
order values but identical conditions are **not** an error — the lower order wins.
:::

📖 [Handler Annotations — Duplicate Mapping Detection](./core-concepts/handlers.md#duplicate-mapping-detection)

---

## 2. Webhook Security Enhancements

### `requireSecretToken` fail-fast validation

The webhook transport now enforces that a secret token is actually configured when you
declare it required. Setting `require-secret-token: true` without providing a `secret-token`
value now fails at startup with a `BeanCreationException`:

```yaml
easygram:
  update:
    webhook:
      require-secret-token: true  # ← startup fails if secret-token is absent
      secret-token: ${WEBHOOK_SECRET}
```

This prevents a misconfigured deployment from accepting webhook requests without validation.
Previously, the omission was silently ignored.

### `maxBodyBytes` — body size limit

Set a maximum allowed request body size for webhook endpoints. Requests exceeding the limit
are rejected immediately with **HTTP 413 Request Entity Too Large** before any parsing occurs:

```yaml
easygram:
  update:
    webhook:
      max-body-bytes: 5242880  # 5 MB (default: unlimited)
```

This protects against oversized payloads that could exhaust heap memory or cause excessive
GC pressure on high-traffic bots.

📖 [Webhook Guide — Security](./transports/webhook-guide.md#security)

---

## 3. Startup Fail-Fast Validation for Annotation Values

Three annotation value validations were added to `BotHandlerLoader`. Blank values that would
cause silent runtime failures now throw `BeanCreationException` immediately at startup:

| Annotation | Invalid example | Error |
|---|---|---|
| `@BotMarkup` | `@BotMarkup("")` | Blank markup name — provide the markup identifier |
| `@BotReplyMarkup` | `@BotReplyMarkup("")` | Blank markup reference — provide the registered markup name |
| `@BotForwardChatState` | `@BotForwardChatState("")` | Blank state name — provide a non-blank state identifier |

```java
// ❌ Fails at startup — blank @BotForwardChatState
@BotText("register")
@BotForwardChatState("")      // BeanCreationException!
public String onRegister() { ... }

// ✅ Correct
@BotText("register")
@BotForwardChatState("REGISTERED")
public String onRegister() { ... }
```

---

## 4. Observability — Error Counter with `exception` Tag

The `easygram.update.error_total` counter now carries an **`exception`** tag containing the
simple class name of the exception that caused the error. This enables alert rules and
dashboards to distinguish error types:

**Before (0.0.6):** `easygram.update.error_total` — single counter, no breakdown.

**After (0.0.7):** `easygram.update.error_total{exception="IllegalStateException"}` — one time series per exception type.

```promql
# Alert if any error type exceeds 10/min
rate(easygram_update_error_total[1m]) > 10

# Show top error types over the last hour
topk(5, sum(increase(easygram_update_error_total[1h])) by (exception))
```

**Graceful no-op without Micrometer:** The `core-observability` module now starts and registers
its beans even when `MeterRegistry` is not on the classpath. Metrics are simply no-ops — you
no longer need to exclude the module when Micrometer is absent.

📖 [Observability — Error Counter](./advanced/observability.md#error-counter)

---

## 5. `@BotStartTrigger` as `@FunctionalInterface`

`BotStartTrigger` is now annotated with `@FunctionalInterface`, enabling lambda-style
registration in `@BotConfiguration` classes:

```java
// Before (0.0.6) — required anonymous class or separate bean
@Bean
public BotStartTrigger warmupTrigger() {
    return new BotStartTrigger() {
        @Override
        public void onStart(TelegramClient client) {
            client.execute(new SetMyCommands(...));
        }
    };
}

// After (0.0.7) — concise lambda
@Bean
public BotStartTrigger warmupTrigger() {
    return client -> client.execute(new SetMyCommands(...));
}
```

---

## 6. Reliability Improvements

These are internal changes with no API impact. They improve correctness and performance under
concurrent load:

| Improvement | Detail |
|---|---|
| **Thread-safe handler registry** | `BotHandlerRegistry` lists are now `CopyOnWriteArrayList`, preventing `ConcurrentModificationException` if handlers are iterated concurrently during hot reload |
| **Bounded regex cache** | `@BotTextPattern` compiled patterns are cached in an LRU map capped at 512 entries, preventing unbounded heap growth when many unique patterns are dynamically generated |
| **Stable exception handler tiebreak** | When multiple `@BotExceptionHandler` methods have equal `@BotOrder`, the winner is now deterministic (sorted by canonical class name) — no more non-deterministic handler selection across restarts |
| **Error propagation from sender** | `BotApiMethodsSenderFilter` no longer silently swallows Telegram API send failures — exceptions now reach your `@BotExceptionHandler` methods |

### Error propagation: what to do

The sender-filter change means Telegram send failures (e.g., rate limits, chat not found) are
now visible. If you do not have an appropriate exception handler, these will surface as
`IllegalStateException` from the dispatcher. Add a handler for `TelegramApiException`:

```java
@BotExceptionHandler(TelegramApiException.class)
public void onTelegramError(TelegramApiException e) {
    log.error("Telegram API error: {}", e.getMessage());
    // optionally retry, or alert
}
```

---

## Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.7</version>
</dependency>
```
