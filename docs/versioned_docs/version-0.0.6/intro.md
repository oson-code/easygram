---
id: intro
title: Introduction to Easygram
description: Easygram is an annotation-driven Spring Boot framework for building Telegram bots. Learn how it simplifies routing, transports, and chat state management with zero boilerplate.
keywords: [telegram bot framework, spring boot telegram, java telegram bot, annotation-driven bot, easygram]
---

# Welcome to Easygram

Easygram is a **Spring Boot framework for building Telegram bots** with annotation-driven routing, pluggable transports, and zero boilerplate. It abstracts away the complexity of Telegram Bot API integration and lets you focus on your bot's business logic.

## What is Easygram?

Easygram provides:

- **Zero-config autoconfiguration** — Add a single dependency and one property to get started
- **Annotation-driven routing** — Use `@BotCommand`, `@BotText`, `@BotCallbackQuery`, and more to define handlers
- **Smart parameter injection** — Inject `Update`, `User`, `Chat`, `TelegramClient`, or annotated scalars automatically
- **Four pluggable transports** — Switch between long-polling (default), webhook, Kafka consumer, or RabbitMQ consumer with one property
- **Composable filter pipeline** — Intercept every update for auth, logging, rate-limiting, etc.
- **Chat state management** — Build multi-step wizards with stateful handlers
- **Handler priority & exception handling** — Fine-grained control with `@BotOrder` and `@BotExceptionHandler`
- **Multiple return types** — Return strings, API methods, collections, or localized replies
- **Broker publishing** — Forward updates to Kafka or RabbitMQ automatically
- **Internationalization (i18n)** — Locale-aware messages and keyboards out-of-the-box
- **Fully customizable** — Every bean uses `@ConditionalOnMissingBean` for easy overrides

## Key Features

### Quick to Start
```java
@BotController
public class MyBot {
    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "!";
    }
}
```

### Pluggable Transports
Switch between update sources without code changes:
- **Long-polling** (default) — Perfect for development and small deployments
- **Webhook** — Scale to production with HTTPS webhooks
- **Kafka Consumer** — Integrate with Kafka pipelines
- **RabbitMQ Consumer** — Use RabbitMQ message queues

### Stateful Workflows
Build registration wizards and multi-step flows:
```java
@BotCommand("/register")
@BotChatState("REGISTRATION")
@BotForwardChatState("REGISTERED")
public String handleRegistration(String input) { ... }
```

### Extensible
- Custom `BotFilter` for middleware (auth, rate-limiting, logging)
- Custom `BotArgumentResolver` for parameter injection
- Custom `BotReturnTypeHandler` for response types
- Replace `BotChatStateService` with Redis or JDBC backends

### Internationalization
```yaml
spring:
  messages:
    basename: messages/bot
easygram:
  i18n:
    default-locale: en
```

## Use Cases

### 1. Simple Echo Bot
Respond to commands and text messages with minimal boilerplate.

### 2. Registration & Onboarding Wizard
Guide users through multi-step flows using chat state management.

### 3. Notification Service
Publish bot updates to Kafka or RabbitMQ for downstream processing.

### 4. Multi-Language Support
Build bots that respond in the user's preferred language.

### 5. Enterprise Integration
Combine with custom filters for authentication, audit logging, and monitoring.

## What's Next?

- **[Quick Start](quick-start.md)** — Build your first bot in 5 minutes
- **[Architecture Overview](architecture.md)** — Understand the framework design
- **[Core Concepts](core-concepts/handlers)** — Master handlers, filters, and state
- **[Transport Guides](transports/long-polling-guide)** — Deploy with long-polling, webhook, Kafka, or RabbitMQ
- **[Examples](examples/echo-bot)** — See runnable examples for every pattern

## Community & Support

- **GitHub** — [github.com/oson-code/easygram](https://github.com/oson-code/easygram)
- **Issues** — Report bugs or request features
- **Discussions** — Ask questions and share ideas
- **License** — MIT

---

Happy bot building!

