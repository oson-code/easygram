---
id: rabbitmq-consumer-guide
title: RabbitMQ Consumer Transport
description: Run a RabbitMQ consumer bot with Easygram — configure easygram.messaging.consumer.type=RABBIT, exchange, queue, routing-key, spring.rabbitmq, and Docker Compose setup.
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
    <version>0.0.6</version>
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
    <version>0.0.6</version>
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
  messaging:
    type: CONSUMER
    consumer:
      type: RABBIT
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

:::info Consumer transport and `easygram.update.transport`
When `easygram.messaging.type=CONSUMER`, the bot receives updates from RabbitMQ — not from
Telegram directly. The `easygram.update.transport` property (long-polling / webhook) has no
effect and the Telegram update transport is not started.
:::

## Configuration Reference

### `easygram.messaging.*`

| Property | Required | Default | Description |
|---|---|---|---|
| `easygram.messaging.type` | **Yes** | — | Must be `CONSUMER` |
| `easygram.messaging.consumer.type` | **Yes** | — | Must be `RABBIT` |

### `easygram.messaging.rabbit.*`

| Property | Required | Default | Description |
|---|---|---|---|
| `easygram.messaging.rabbit.exchange` | **Yes** | — | RabbitMQ exchange to bind the queue to |
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
      easygram.messaging.type: CONSUMER
      easygram.messaging.consumer.type: RABBIT
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

## Use Cases

| Use case | Pattern |
|---|---|
| Microservice integration | Producer bot forwards; consumer bots specialise |
| Work queue | Multiple consumer instances share the load |
| Dead-letter handling | Route failed messages to a dead-letter exchange for retry |
| Priority queues | Use RabbitMQ priority queues to handle urgent updates first |

---

All transports covered! Next: [Broker Publishing](../advanced/broker-publishing) to set up a producer bot.

