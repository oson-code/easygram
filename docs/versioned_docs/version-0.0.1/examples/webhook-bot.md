---
id: webhook-bot
title: Webhook Bot Example
---

# Webhook Bot Example

An echo bot using the **webhook transport** instead of long-polling. Telegram pushes updates
to your HTTPS endpoint as HTTP POST requests. This is the recommended transport for production
deployments.

**Full source:** [`samples/webhook-bot`](https://github.com/oson-code/easygram/tree/main/samples/webhook-bot)

## What We Build

Same echo functionality as the [Echo Bot Example](./echo-bot), delivered via webhook:

| User sends | Bot replies |
|---|---|
| `/start` | Greeting with the user's first name |
| `/help` | List of available commands |
| Any text | Echoes the text back |
| Anything else | "I don't know how to handle that" |

## Prerequisites

Webhook requires a **publicly accessible HTTPS URL**:

| Scenario | Tool |
|---|---|
| Local development | [ngrok](https://ngrok.com/), [localtunnel](https://localtunnel.me/), [jprq](https://jprq.io/) |
| Production | Reverse proxy (Nginx, Traefik) with a valid TLS certificate |
| Self-signed certificate | Supported — pass the cert to Telegram during `setWebhook` |

## Project Setup

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.2</version>
</dependency>
```

### application.yml

```yaml
easygram:
  token: "${BOT_TOKEN}"
  transport: WEBHOOK

  webhook:
    # Public HTTPS URL Telegram will POST updates to
    url: "https://my-bot.example.com/webhook"

    # Local path that accepts incoming requests (default: /webhook)
    path: /webhook

    # Recommended: validate X-Telegram-Bot-Api-Secret-Token header
    # secret-token: "${WEBHOOK_SECRET}"

    # Max simultaneous connections from Telegram (1–100, default: 40)
    max-connections: 40

    # Drop queued updates when registering (default: false)
    drop-pending-updates: false

    # Call deleteWebhook on application shutdown (default: false)
    unregister-on-shutdown: true

server:
  port: 8080

spring:
  application:
    name: webhook-bot

logging:
  level:
    uz.osoncode: DEBUG
```

## Application Class

```java
package uz.example.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramUrl;
import uz.osoncode.easygram.core.provider.BotTelegramUrlProvider;

@SpringBootApplication
public class WebhookBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookBotApplication.class, args);
    }

    /**
     * Optional: override the Telegram API base URL.
     * Useful for pointing at a local Bot API server or a custom proxy.
     * Remove this bean entirely to use the default api.telegram.org endpoint.
     */
    @Bean
    public BotTelegramUrlProvider botTelegramUrlProvider() {
        return () -> TelegramUrl.builder()
                .schema("https")
                .host("api.telegram.org")
                .port(443)
                .build();
    }
}
```

## Bot Controller

The controller is **identical** to the long-polling example — transport is transparent:

```java
package uz.example.webhook;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.bind.annotation.*;
import uz.osoncode.easygram.core.model.BotRequest;

@BotController
public class EchoBotController {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName()
               + "! I am an echo bot using webhook. "
               + "Send me any message and I will repeat it.";
    }

    @BotCommand("/help")
    public String onHelp(@BotCommandValue String command) {
        return "Available commands:\n" +
               "/start — welcome message\n" +
               "/help  — this help text\n\n" +
               "Send any text message and I will echo it back.";
    }

    @BotTextDefault
    public String onText(@BotTextValue String text, User user) {
        return user.getFirstName() + " said: " + text;
    }

    @BotDefaultHandler
    public SendMessage onUnknown(BotRequest request) {
        return SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("I don't know how to handle that. Try /help.")
                .build();
    }
}
```

:::tip
The handler code is **exactly the same** as in the long-polling example. Switching transports
requires only a property change, never a code change.
:::

## Lifecycle

On startup, the framework automatically calls Telegram's `setWebhook`:

```
Application start
  → POST https://api.telegram.org/bot<TOKEN>/setWebhook
  → { "url": "https://my-bot.example.com/webhook", ... }
  → 200 OK — Telegram starts pushing updates to your endpoint

Application shutdown (if unregister-on-shutdown: true)
  → POST .../deleteWebhook
  → Telegram stops pushing updates
```

## Security: Secret Token

Prevent spoofed webhook requests by configuring a secret token:

```yaml
easygram:
  webhook:
    secret-token: "${WEBHOOK_SECRET}"
```

Telegram sends the token in the `X-Telegram-Bot-Api-Secret-Token` header with every POST.
The framework validates it automatically and returns HTTP 403 for requests that fail validation.

## Local Development with ngrok

```bash
# 1. Start your bot
export BOT_TOKEN=your_token
mvn spring-boot:run

# 2. In another terminal, expose port 8080
ngrok http 8080

# 3. Update application.yml with the ngrok URL
#    easygram.webhook.url: https://abc123.ngrok-free.app/webhook

# 4. Restart the bot — it registers the new URL automatically
```

## Local Development with jprq

```bash
# 1. Expose port 8080
jprq http 8080

# 2. Copy the *.jprq.live URL and update application.yml
#    easygram.webhook.url: https://my-session.jprq.live/webhook

# 3. Restart the bot
```

## Webhook vs. Long-Polling

| | Long-Polling | Webhook |
|---|---|---|
| **Setup** | Minimal | Requires HTTPS URL |
| **Latency** | ~50 ms (poll timeout) | Near real-time |
| **Internet** | Outbound only | Inbound HTTPS required |
| **Best for** | Development, small bots | Production, high traffic |
| **Horizontal scaling** | Limited | Yes (multiple instances) |
| **Property** | *(default)* | `transport: WEBHOOK` |

## Running in Production with Docker

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/webhook-bot.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
services:
  bot:
    build: .
    environment:
      BOT_TOKEN: "${BOT_TOKEN}"
      WEBHOOK_SECRET: "${WEBHOOK_SECRET}"
      TELEGRAM_BOT_WEBHOOK_URL: "https://my-bot.example.com/webhook"
    ports: ["8080:8080"]
```

Pair with a reverse proxy (Nginx, Traefik, Caddy) to handle TLS termination.

---

See also:
- [Webhook Transport Guide](../transports/webhook-guide) — full configuration reference
- [Echo Bot Example](./echo-bot) — same bot with long-polling transport
- [Quick Start](../quick-start) — first-bot guide
