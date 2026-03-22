---
id: long-polling-guide
title: Long-Polling Transport
---

# Long-Polling Transport Guide

The long-polling transport is the **default** and simplest way to run your bot. It continuously polls the Telegram API for new updates.

## How It Works

1. Your bot connects to Telegram API
2. Polls `getUpdates` endpoint every N seconds
3. Receives updates in batches
4. Processes each update through your handlers
5. Repeats

## Configuration

**application.yml:**
```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: LONG_POLLING  # Default, can be omitted
    long-polling:
      allow-users-init-updates: true
      polling-timeout: 30
      max-poll-size: 100
```

**application.properties:**
```properties
telegram.bot.token=${BOT_TOKEN}
telegram.bot.transport=LONG_POLLING
telegram.bot.long-polling.polling-timeout=30
telegram.bot.long-polling.max-poll-size=100
```

## Configuration Options

| Property | Default | Description |
|---|---|---|
| `polling-timeout` | 30 | Timeout in seconds for long-polling |
| `max-poll-size` | 100 | Max updates per poll |
| `allow-users-init-updates` | true | Process initial updates after bot startup |

## Advantages

✅ Simple to setup (no HTTPS, firewall, DNS required)
✅ Works in development and behind NAT
✅ No external infrastructure needed
✅ Good for testing and prototyping

## Disadvantages

❌ Polling adds latency (~N seconds) before updates are received
❌ Not efficient for large-scale production (many API calls)
❌ Heavier load on Telegram API

## When to Use

- **Development** — Perfect for testing locally
- **Single-instance bots** — Low to medium traffic
- **Behind NAT/Firewall** — When webhook is not possible
- **Prototyping** — Quick MVP development

## Production Considerations

For production with long-polling:

1. Use a container/orchestration platform (Kubernetes, Docker Swarm)
2. Set reasonable polling timeout (30-60 seconds)
3. Monitor API rate limits
4. Use persistent chat state (Redis) for multi-instance
5. Consider switching to webhook for high traffic

## Example: Long-Polling Bot

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
    public String onStart() {
        return "Echo bot started! Send me any message and I'll echo it back.";
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

telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: LONG_POLLING
    long-polling:
      polling-timeout: 30
      max-poll-size: 100
```

**Run:**
```bash
export BOT_TOKEN="your_bot_token"
mvn spring-boot:run
```

## Docker Deployment

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:17-jdk
COPY target/bot.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  bot:
    build: .
    environment:
      BOT_TOKEN: ${BOT_TOKEN}
      TELEGRAM_BOT_TRANSPORT: LONG_POLLING
    restart: unless-stopped
```

**Run:**
```bash
BOT_TOKEN="your_token" docker-compose up
```

---

Next: Learn about [Webhook Transport](webhook-guide) for production deployments.
