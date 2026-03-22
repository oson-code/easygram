# messaging-rabbit

> RabbitMQ `BotUpdatePublisher` implementation for the Easygram framework.

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-rabbit</artifactId>
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## What It Does

Serializes each incoming Telegram `Update` to JSON and publishes it to a configured RabbitMQ exchange with a routing key using Spring's `RabbitTemplate`. Integrates with the `BotUpdatePublishingFilter` from the `messaging-api` module to forward every update before any handler logic runs.

## Architecture

```
Telegram API
     │
     ▼
[LongPollingBot / WebhookBot]
     │
     ▼
[BotUpdatePublishingFilter]   ← runs at order MIN_VALUE + 1000
     │
     ▼
[RabbitBotUpdatePublisher]
     │
     ▼ routing-key: telegram.updates
[RabbitMQ Exchange: telegram-exchange]
     │  binding
     ▼
[RabbitMQ Queue: telegram-updates]   ← consumed by messaging-rabbit-consumer
```

## Exchange and Queue Topology

When `create-if-absent: true` (the default), the framework auto-declares on startup:

- A **durable topic exchange** named by `exchange` (default: `telegram-exchange`)
- A **durable queue** named by `queue` (default: `telegram-updates`)
- A **binding** between them using `routing-key` (default: `telegram.updates`)

If these resources already exist in RabbitMQ, the declarations are idempotent — no error is thrown as long as the existing definitions match. Set `create-if-absent: false` to disable all declarations (e.g. when resources are managed externally).

## Configuration

```yaml
telegram:
  bot:
    messaging:
      producer:
        consumer-type: rabbit
      rabbit:
        exchange: telegram-exchange
        routing-key: telegram.updates
        queue: telegram-updates
        create-if-absent: true         # default: true

spring:
  rabbitmq:
    host: localhost
    username: guest
    password: guest
```

## All Properties

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.messaging.producer.consumer-type` | ✅ | — | Must be `rabbit` to activate this publisher |
| `telegram.bot.messaging.rabbit.exchange` | ✅ | — | RabbitMQ exchange to publish to |
| `telegram.bot.messaging.rabbit.routing-key` | ✅ | `telegram.updates` | Routing key for published messages |
| `telegram.bot.messaging.rabbit.queue` | ❌ | `telegram-updates` | Queue bound during auto-creation |
| `telegram.bot.messaging.rabbit.create-if-absent` | ❌ | `true` | Auto-create exchange/queue/binding if absent |
| `spring.rabbitmq.host` | ✅ | `localhost` | Broker host |
| `spring.rabbitmq.port` | ❌ | `5672` | AMQP port |
| `spring.rabbitmq.username` | ❌ | `guest` | Username |
| `spring.rabbitmq.password` | ❌ | `guest` | Password |

## Message Format

| Field | Value |
|---|---|
| **Content-Type** | `application/json` |
| **Body** | JSON-serialised `Update` object |
| **Routing Key** | value of `telegram.bot.messaging.rabbit.routing-key` |

The `ObjectMapper` used for JSON serialization is provided by `BotObjectMapperProvider` and defaults to the shared Spring `ObjectMapper` bean. Override it by declaring your own `BotObjectMapperProvider` bean if you need custom serialization modules.

## Forward-Only Mode

To publish updates to RabbitMQ without processing them locally, set `forward-only: true`:

```yaml
telegram:
  bot:
    messaging:
      forward-only: true
      producer:
        consumer-type: rabbit
      rabbit:
        exchange: telegram-exchange
        routing-key: telegram.updates
```

In forward-only mode the bot receives each update, publishes it to RabbitMQ, and then stops — it does **not** dispatch the update to `@BotController` handlers.

## Error Handling

If the RabbitMQ broker is unreachable at publish time, `RabbitTemplate` throws an `AmqpException`. This exception propagates through the filter chain — your `@BotExceptionHandler` or a custom `BotFilter` can catch it. Spring AMQP's `RetryTemplate` can be configured on the `RabbitTemplate` to add automatic retries before the exception surfaces.

## Customization

You can provide a custom `RabbitTemplate` by registering a `BotRabbitTemplateProvider` bean. This is useful if you need to use a specific `ConnectionFactory` or custom configuration that differs from the default Spring Boot auto-configuration.

```java
@Bean
public BotRabbitTemplateProvider botRabbitTemplateProvider(ConnectionFactory connectionFactory) {
    return () -> new RabbitTemplate(connectionFactory);
}
```

## See Also

- [messaging-rabbit-consumer/README.md](../messaging-rabbit-consumer/README.md) — consume updates from RabbitMQ
- [messaging-producer/README.md](../messaging-producer/README.md) — smart broker routing
- [messaging-api/README.md](../messaging-api/README.md) — SPI and custom publisher interface

