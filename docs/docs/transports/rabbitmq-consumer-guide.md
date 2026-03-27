---
id: rabbitmq-consumer-guide
title: RabbitMQ Consumer Transport
---

# RabbitMQ Consumer Transport

Consume Telegram updates from a RabbitMQ queue.

## Add Dependencies

`messaging-rabbit-consumer` is included in `spring-boot-starter`, but `spring-boot-starter-amqp`
is **not** pulled in transitively (it is marked optional to avoid unwanted broker connections
when you are using a different transport). You must add it explicitly:

```xml
<!-- spring-boot-starter already includes messaging-rabbit-consumer -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.2</version>
</dependency>

<!-- Required: provides ConnectionFactory + RabbitTemplate for RABBIT_CONSUMER transport -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

If you are NOT using `spring-boot-starter`, add `messaging-rabbit-consumer` directly:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-rabbit-consumer</artifactId>
    <version>0.0.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## Configuration

**application.yml:**
```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: RABBIT_CONSUMER
    rabbit-consumer:
      queue: telegram-updates
      exchange: telegram-exchange
      routing-key: telegram.updates
      create-if-absent: true

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

## Configuration Options

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.rabbit-consumer.queue` | Yes | — | RabbitMQ queue to consume from |
| `telegram.bot.rabbit-consumer.exchange` | No | `telegram-exchange` | Exchange to bind queue to |
| `telegram.bot.rabbit-consumer.routing-key` | No | `telegram.updates` | Routing key for the binding |
| `telegram.bot.rabbit-consumer.create-if-absent` | No | `true` | Auto-create exchange, queue, and binding |

## Docker Deployment

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"  # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  bot:
    build: .
    environment:
      TELEGRAM_BOT_TRANSPORT: RABBIT_CONSUMER
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      - rabbitmq
    restart: unless-stopped
```

## Example

```java
@BotController
public class RabbitBotHandler {
    @BotCommand("/start")
    public String onStart() {
        return "Bot connected to RabbitMQ!";
    }
}
```

See [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html) for advanced queue setup.

---

All transports configured! Next: [Advanced Features](../advanced/custom-filters)
