---
id: rabbitmq-consumer-guide
title: RabbitMQ Consumer Transport
---

# RabbitMQ Consumer Transport

Consume Telegram updates from a RabbitMQ queue.

## Configuration

**application.yml:**
```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: RABBIT_CONSUMER
    rabbit-consumer:
      queue: telegram-updates
      routing-key: updates.*

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

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
