# Easygram — Observability

## MDC Correlation Context

`BotMdcFilter` (order `Integer.MIN_VALUE`, first in the filter chain) automatically
populates SLF4J MDC for every incoming `Update`. All subsequent log statements —
including those in custom `BotFilter` beans, argument resolvers, and handler methods —
carry these keys automatically.

### MDC Keys

| Key | Type | Description |
|-----|------|-------------|
| `bot.update.id` | String (integer) | Telegram update ID |
| `bot.transport` | String (enum name) | Active transport: `LONG_POLLING`, `WEBHOOK`, `KAFKA_CONSUMER`, `RABBIT_CONSUMER` |
| `bot.user.id` | String (long) | Telegram user ID (set after `BotContextSetterFilter`) |
| `bot.chat.id` | String (long) | Telegram chat ID (set after `BotContextSetterFilter`) |

Keys are always cleared in `finally` at the end of filter chain execution.

### Using MDC keys in Logback

```xml
<!-- logback-spring.xml -->
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>
        %d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [upd:%X{bot.update.id}] [chat:%X{bot.chat.id}] [user:%X{bot.user.id}] [%X{bot.transport}] %cyan(%logger{36}) - %msg%n
      </pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="STDOUT"/>
  </root>

  <!-- Enable TRACE logging for argument resolution and method invocation -->
  <logger name="uz.osoncode.easygram" level="DEBUG"/>
</configuration>
```

### Log level guide

| Level | What you see |
|-------|-------------|
| `ERROR` | Processing failures, publish failures, unhandled exceptions |
| `WARN` | No handler matched for update; no argument resolver found for a parameter |
| `INFO` | Bot startup: handler count, markup count, transport type |
| `DEBUG` | Handler matched per update; chat state transitions; markup applied; per-controller registration |
| `TRACE` | Per-parameter argument resolution; method invocation entry/exit; return type dispatch |

### Recommended production configuration

```yaml
logging:
  level:
    root: WARN
    uz.osoncode.easygram: INFO   # startup events, no per-request noise
```

### Recommended debugging configuration

```yaml
logging:
  level:
    uz.osoncode.easygram: DEBUG  # shows handler matching and state transitions
    # uz.osoncode.easygram: TRACE  # adds argument resolution detail
```

---

## Micrometer Metrics

`core-observability` provides a `BotObservabilityFilter` that records metrics via
Micrometer for every processed update. The following metrics are emitted:

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `easygram.update.processed` | Counter | `transport`, `update_type` | Total updates processed |
| `easygram.update.errors` | Counter | `transport`, `exception` | Total processing errors |
| `easygram.update.duration` | Timer | `transport`, `update_type` | Processing time per update |

### Chat State Metrics

`core-chatstate` emits per-operation counters automatically whenever `micrometer-core` and
a `MeterRegistry` bean are present. No additional dependency is required — the instrumented
service is wired by `ChatStateAutoConfiguration` only when Micrometer is on the classpath.

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `easygram.chatstate.get` | Counter | `result=hit` | State look-ups that returned a value |
| `easygram.chatstate.get` | Counter | `result=miss` | State look-ups with no stored value |
| `easygram.chatstate.set` | Counter | — | State writes |
| `easygram.chatstate.clear` | Counter | — | State removals |

These counters are useful for tracking conversation funnel depth and identifying stuck
chat sessions. Use them in dashboards alongside the per-update `easygram.update.duration`
timer to correlate processing time with state-machine activity.

### Spring Boot Actuator endpoint

A custom `/actuator/telegram-bot` endpoint is registered when `core-observability` is
on the classpath. It reports the transport type, handler count, and basic health status.

Enable it in `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, telegram-bot
```

---

## Accessing MDC Keys in Custom Code

The MDC key name constants are public on `BotMdcFilter`:

```java
import uz.osoncode.easygram.core.filter.BotMdcFilter;

// In a custom BotFilter:
String updateId = MDC.get(BotMdcFilter.MDC_UPDATE_ID);
String chatId   = MDC.get(BotMdcFilter.MDC_CHAT_ID);
String userId   = MDC.get(BotMdcFilter.MDC_USER_ID);
String transport = MDC.get(BotMdcFilter.MDC_TRANSPORT);
```

---

## Replacing the MDC Filter

Override the default by declaring your own bean:

```java
@Configuration
public class MyObservabilityConfig {

    @Bean
    public BotMdcFilter botMdcFilter(BotConfigurer botConfigurer) {
        return new MyCustomMdcFilter(botConfigurer); // your subclass
    }
}
```
