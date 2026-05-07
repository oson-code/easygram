---
id: rabbitmq-consumer-guide
title: RabbitMQ Consumer Transport
description: Run a RabbitMQ consumer bot with Easygram — configure easygram.update.transport=RABBIT_CONSUMER, exchange, queue, routing-key, spring.rabbitmq, and Docker Compose setup.
keywords: [telegram bot rabbitmq consumer, easygram rabbitmq transport, spring boot telegram rabbitmq, AMQP telegram bot java, easygram rabbit consumer]
---

# RabbitMQ Consumer Transport

The RabbitMQ consumer transport lets your bot receive Telegram updates from a RabbitMQ queue instead of polling Telegram directly. A separate **producer bot** publishes updates to the exchange; this bot only processes them.

## Architecture

```
Telegram API
    ↓  (long-polling or webhook)
Producer Bot  →  [RabbitMQ Exchange]  →  [Queue: easygram-updates]
                                                 ↓
                                         Consumer Bot instance(s)
```

## Add Dependencies

`messaging-api` includes RabbitMQ consumer support, but `spring-boot-starter-amqp` is **optional** in `messaging-api` to avoid unwanted broker connections. Declare it explicitly:

```xml
<!-- Easygram starter (already includes messaging-api) -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.7</version>
</dependency>

<!-- Required: spring-amqp is marked optional in messaging-api -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

Or without the starter:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-api</artifactId>
    <version>0.0.7</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## Configuration

**application.yml:**
```yaml
easygram:
  token: ${BOT_TOKEN}
  update:
    transport: RABBIT_CONSUMER
  messaging:
    rabbit:
      exchange: easygram-exchange
      queue: easygram-updates
      routing-key: easygram.updates
      create-if-absent: true

spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
```

:::info Consumer transport
When `easygram.update.transport=RABBIT_CONSUMER`, the bot receives updates from RabbitMQ — not from
Telegram directly. Long-polling and webhook transports are not started.
:::

## Configuration Reference

### `easygram.update.*`

| Property | Required | Default | Description |
|---|---|---|---|
| `easygram.update.transport` | **Yes** | — | Must be `RABBIT_CONSUMER` |

### `easygram.messaging.rabbit.*`

| Property | Required | Default | Description |
|---|---|---|---|
| `easygram.messaging.rabbit.exchange` | No | `easygram-exchange` | RabbitMQ exchange to bind the queue to |
| `easygram.messaging.rabbit.queue` | No | `easygram-updates` | Queue name for consuming updates |
| `easygram.messaging.rabbit.routing-key` | No | `easygram.updates` | Routing key for the exchange binding |
| `easygram.messaging.rabbit.create-if-absent` | No | `true` | Auto-create exchange, queue, and binding on startup |

### Standard Spring RabbitMQ properties

| Property | Required | Description |
|---|---|---|
| `spring.rabbitmq.host` | **Yes** | RabbitMQ server hostname |
| `spring.rabbitmq.port` | No | Port (default: 5672) |
| `spring.rabbitmq.username` | No | Username (default: `guest`) |
| `spring.rabbitmq.password` | No | Password (default: `guest`) |
| `spring.rabbitmq.virtual-host` | No | Virtual host (default: `/`) |

## Example Bot

```java
@SpringBootApplication
public class RabbitConsumerBotApp {
    public static void main(String[] args) {
        SpringApplication.run(RabbitConsumerBotApp.class, args);
    }
}

@BotController
public class RabbitConsumerBot {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello " + user.getFirstName() + " — update received from RabbitMQ!";
    }

    @BotDefaultHandler
    public String onAny(@BotTextValue String text) {
        return "Processed via RabbitMQ: " + text;
    }
}
```

## Docker Deployment

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"   # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: rabbit
      RABBITMQ_DEFAULT_PASS: rabbit
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  bot:
    build: .
    environment:
      easygram.token: ${BOT_TOKEN}
      easygram.update.transport: RABBIT_CONSUMER
      easygram.messaging.rabbit.exchange: easygram-exchange
      easygram.messaging.rabbit.queue: easygram-updates
      easygram.messaging.rabbit.routing-key: easygram.updates
      spring.rabbitmq.host: rabbitmq
      spring.rabbitmq.username: rabbit
      spring.rabbitmq.password: rabbit
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped
```

**Run:**
```bash
BOT_TOKEN="your_token" docker compose up
```

Open the RabbitMQ Management UI at [http://localhost:15672](http://localhost:15672) (user: `rabbit`, password: `rabbit`) to inspect exchanges, queues, and message rates.

## Error Handling & Reliability {#error-handling--reliability}

### ACK-always policy (no infinite requeue loops)

`RabbitBotUpdateListener` always **ACKs** every AMQP message, regardless of whether
processing succeeded or failed. This is intentional:

- If a Telegram API call fails with a **4xx error** (e.g. `400 Bad Request: can't parse
  entities`), resending the same message payload will never succeed. Re-throwing the
  exception would cause the AMQP container to NACK and requeue the same broken message
  indefinitely — a poison-message loop.
- If a message has **malformed JSON** or causes any other exception during deserialization or
  dispatch, the same infinite-requeue problem applies.

The failed message is logged at `ERROR` level with the full stack trace and then dropped:

```
ERROR [botRabbitListenerContainer-1] — [messageId=263056385] Failed to process RabbitMQ message
  — ACKing to prevent requeue loop; check the error above
```

### Dead-Letter Exchange (DLX) for failed-message routing

If you need to inspect or replay failed messages instead of dropping them, configure a
Dead-Letter Exchange on the RabbitMQ broker. When the consumer ACKs a message after a
processing error, you cannot retroactively route it to a DLX — but you can set up DLX
**before** the consumer starts:

```yaml
# Declare DLX bindings via RabbitMQ Management or at application startup
# Example: route failed messages from easygram-updates to easygram-dlx
easygram:
  messaging:
    rabbit:
      exchange: easygram-exchange
      queue: easygram-updates       # set x-dead-letter-exchange on this queue in broker config
      routing-key: easygram.updates
      create-if-absent: true
```

Configure the DLX on the broker:
```bash
# Using rabbitmqadmin or management UI:
rabbitmqadmin declare queue name=easygram-updates \
  arguments='{"x-dead-letter-exchange":"easygram-dlx","x-dead-letter-routing-key":"failed"}'
```

:::info Why ACK-always instead of DLX-on-nack
For Telegram bots, the most common failures are permanent (bad parse mode, forbidden,
chat not found). Automatic requeue would cause the same message to be retried thousands of
times per second. ACK-always with an explicit DLX is the correct pattern: you get visibility
into failed messages without the poison-message loop.
:::

### 4xx Telegram errors: suppressed at the sender layer

`BotApiMethodsSenderFilter` (the built-in filter that calls the Telegram API) classifies
Telegram errors before they can reach the AMQP layer:

| Error type | Behavior |
|---|---|
| **4xx (400–499)** | Logged as `ERROR`, **suppressed** — not propagated to exception handlers |
| **5xx / network errors** | Logged as `ERROR`, **re-thrown** — reaches your `@BotExceptionHandler` |

This means a `400 Bad Request: can't parse entities` error is silently dropped (after
logging) and the message is ACKed normally. Only transient 5xx or network errors are
surfaced to your exception handlers:

```java
// Receives 5xx and network errors only — 4xx are suppressed before this runs
@BotExceptionHandler(TelegramApiException.class)
public void onTelegramError(TelegramApiException e) {
    log.error("Transient Telegram error: {}", e.getMessage());
}
```

---

## Use Cases

| Use case | Pattern |
|---|---|
| Microservice integration | Producer bot forwards; consumer bots specialise |
| Work queue | Multiple consumer instances share the load |
| Dead-letter handling | Route failed messages to a dead-letter exchange for retry |
| Priority queues | Use RabbitMQ priority queues to handle urgent updates first |

---

All transports covered! Next: [Broker Publishing](../advanced/broker-publishing) to set up a producer bot.

