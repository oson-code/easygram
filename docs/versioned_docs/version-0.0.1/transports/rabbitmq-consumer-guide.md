---
id: rabbitmq-consumer-guide
title: RabbitMQ Consumer Transport
---

# RabbitMQ Consumer Transport

Consume Telegram updates from a RabbitMQ queue.

## Configuration

**application.yml:**
```yaml
easygram:
  token: ${BOT_TOKEN}
  transport: RABBIT_CONSUMER
  rabbit-consumer:
    queue: easygram-updates
    exchange: easygram-exchange
    routing-key: easygram.updates
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
| `easygram.rabbit-consumer.queue` | Yes | — | RabbitMQ queue to consume from |
| `easygram.rabbit-consumer.exchange` | No | `easygram-exchange` | Exchange to bind queue to |
| `easygram.rabbit-consumer.routing-key` | No | `easygram.updates` | Routing key for the binding |
| `easygram.rabbit-consumer.create-if-absent` | No | `true` | Auto-create exchange, queue, and binding |

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
