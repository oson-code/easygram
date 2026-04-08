---
id: long-polling-guide
title: Long-Polling Transport
description: Configure Telegram long-polling transport in Easygram — required dependency, easygram.update.transport property, polling interval tuning, and Docker deployment with environment variables.
keywords: [telegram long polling spring boot, easygram long polling, telegram bot polling java, spring boot bot transport, easygram.update.transport]
---

# Long-Polling Transport Guide

The long-polling transport is the **default** and simplest way to run your bot. It continuously polls the Telegram API for new updates using the `getUpdates` endpoint — no public HTTPS URL required.

## How It Works

1. Your bot connects to the Telegram API
2. Calls `getUpdates` with a long-timeout (the bot library handles retries automatically)
3. Receives a batch of updates
4. Processes each update through your handler pipeline
5. Calls `getUpdates` again immediately

## Configuration

Long-polling is the **default transport** — omit the `update` block entirely and your bot uses long-polling automatically.

**application.yml (minimal — recommended):**
```yaml
easygram:
  token: ${BOT_TOKEN}
```

**application.yml (explicit):**
```yaml
easygram:
  token: ${BOT_TOKEN}
  update:
    transport: LONG_POLLING
```

**application.properties:**
```properties
easygram.token=${BOT_TOKEN}
# easygram.update.transport=LONG_POLLING  # optional; this is the default
```

## Configuration Options

| Property | Required | Default | Description |
|---|---|---|---|
| `easygram.token` | **Yes** | — | Bot API token from @BotFather |
| `easygram.update.transport` | No | `LONG_POLLING` | Update delivery mechanism |

Long-polling has no additional Easygram-specific properties. The underlying `telegrambots` library manages polling timeout and retry logic.

## Advantages

- ✅ Zero infrastructure — no HTTPS certificate, no domain, no firewall rules
- ✅ Works locally and behind NAT
- ✅ Ideal for development and testing
- ✅ Simplest setup — just provide a token

## Disadvantages

- ⚠️ Higher latency than webhook (one polling cycle between user message and dispatch)
- ⚠️ More Telegram API requests per unit time
- ⚠️ Less suitable for very high-traffic bots

## When to Use

| Scenario | Recommendation |
|---|---|
| Local development | ✅ Long-polling |
| Single-instance bots | ✅ Long-polling |
| Behind NAT / corporate firewall | ✅ Long-polling |
| Prototyping / MVP | ✅ Long-polling |
| Production high-traffic | ⚠️ Consider webhook |
| Multi-instance scaling | ⚠️ Consider Kafka/RabbitMQ consumer |

## Example: Echo Bot

```java
@SpringBootApplication
public class LongPollingBotApp {
    public static void main(String[] args) {
        SpringApplication.run(LongPollingBotApp.class, args);
    }
}

@BotController
public class EchoBot {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "! Send me any message.";
    }

    @BotDefaultHandler
    public String echo(@BotTextValue String text) {
        return "You said: " + text;
    }
}
```

**application.yml:**
```yaml
spring:
  application:
    name: echo-bot

easygram:
  token: ${BOT_TOKEN}

logging:
  level:
    uz.osoncode.easygram: INFO
```

**Run:**
```bash
export BOT_TOKEN="your_bot_token"
mvn spring-boot:run
```

## Docker Deployment

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q install -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S bot && adduser -S bot -G bot
USER bot
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

**docker-compose.yml:**
```yaml
services:
  bot:
    build: .
    environment:
      easygram.token: ${BOT_TOKEN}
    restart: unless-stopped
```

:::tip Spring property names as Docker env vars
Spring Boot reads `easygram.token`, `easygram.update.transport`, etc. directly from container
environment variables — no uppercase conversion needed. Pass property names exactly as they
appear in `application.yml`.
:::

**Run:**
```bash
BOT_TOKEN="your_token" docker compose up
```

## Production Considerations

1. **State persistence** — Use Redis-backed `BotChatStateService` so state survives restarts
2. **Health checks** — Expose Spring Actuator (`/actuator/health`) for orchestration platforms
3. **Log correlation** — MDC keys `bot.update.id`, `bot.chat.id`, `bot.user.id` are set automatically; configure your Logback pattern to include them
4. **Graceful shutdown** — Spring Boot handles `SIGTERM`; the bot library drains in-flight updates before stopping
5. **Rate limits** — Telegram's `getUpdates` has generous limits; single-instance bots rarely hit them

---

Next: Learn about [Webhook Transport](webhook-guide) for lower-latency production deployments.

