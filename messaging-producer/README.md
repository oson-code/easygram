# messaging-producer

> Smart broker-routing module for the Easygram framework.
> Selects Kafka or RabbitMQ as the publish target based on a single configuration property.

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-producer</artifactId>
    <version>0.0.4</version>
</dependency>
```

## What It Does

Wires the correct `BotUpdatePublisher` (Kafka or RabbitMQ) based on `telegram.bot.messaging.producer.producer-type`. Optionally auto-creates broker resources (Kafka topic, RabbitMQ exchange/queue/binding) when `create-if-absent=true` (default). A `BotUpdatePublishingFilter` runs at `Integer.MIN_VALUE + 1000` (very early in the filter chain) to ensure every update is captured before any business logic.

## Kafka Setup

Add the Kafka runtime dependency alongside `messaging-producer`:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-producer</artifactId>
    <version>0.0.4</version>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

```yaml
telegram:
  bot:
    messaging:
      producer:
        producer-type: kafka
      kafka:
        topic: telegram-updates
        create-if-absent: true
        partitions: 1
        replication-factor: 1

spring:
  kafka:
    bootstrap-servers: localhost:9092
```

**Message format:**
- **Key**: `updateId` (String)
- **Value**: JSON-serialised `Update`

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.messaging.producer.producer-type` | ✅ | — | Must be `kafka` |
| `telegram.bot.messaging.kafka.topic` | ✅ | — | Topic to publish to |
| `telegram.bot.messaging.kafka.create-if-absent` | ❌ | `true` | Auto-create the topic if absent |
| `telegram.bot.messaging.kafka.partitions` | ❌ | `1` | Partitions for auto-created topic |
| `telegram.bot.messaging.kafka.replication-factor` | ❌ | `1` | Replication factor for auto-created topic |
| `spring.kafka.bootstrap-servers` | ✅ | — | Kafka broker addresses |

## RabbitMQ Setup

Add the AMQP runtime dependency alongside `messaging-producer`:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-producer</artifactId>
    <version>0.0.4</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```yaml
telegram:
  bot:
    messaging:
      producer:
        producer-type: rabbit
      rabbit:
        exchange: telegram-exchange
        routing-key: telegram.updates
        queue: telegram-updates
        create-if-absent: true

spring:
  rabbitmq:
    host: localhost
    username: guest
    password: guest
```

**Message format:**
- **Content-Type**: `application/json`
- **Body**: JSON-serialised `Update`

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.messaging.producer.producer-type` | ✅ | — | Must be `rabbit` |
| `telegram.bot.messaging.rabbit.exchange` | ✅ | — | Exchange to publish to |
| `telegram.bot.messaging.rabbit.routing-key` | ✅ | `telegram.updates` | Routing key |
| `telegram.bot.messaging.rabbit.queue` | ❌ | `telegram-updates` | Queue bound during auto-creation |
| `telegram.bot.messaging.rabbit.create-if-absent` | ❌ | `true` | Auto-create exchange/queue/binding if absent |
| `spring.rabbitmq.host` | ✅ | `localhost` | Broker host |
| `spring.rabbitmq.port` | ❌ | `5672` | AMQP port |
| `spring.rabbitmq.username` | ❌ | `guest` | Username |
| `spring.rabbitmq.password` | ❌ | `guest` | Password |

## Forward-Only Mode

```yaml
telegram:
  bot:
    messaging:
      forward-only: false   # false = publish AND process locally (default)
                            # true  = publish ONLY, skip bot handlers
```

Set `forward-only: true` when the application's sole purpose is forwarding updates to a broker (see the producer examples in [samples/README.md](../samples/README.md)). Set it to `false` (the default) when you want to both publish updates and process them locally in the same application.

## Custom Publisher

To publish to a custom target (SNS, SQS, Redis Pub/Sub, etc.), implement `BotUpdatePublisher` and register it as a `@Bean`. Use the `messaging` module instead to get the SPI without the Kafka/RabbitMQ dependencies:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-api</artifactId>
    <version>0.0.4</version>
</dependency>
```

```java
@Component
public class MyCustomPublisher implements BotUpdatePublisher {

    @Override
    public void publish(Update update) {
        // publish to any system — SNS, SQS, Redis Pub/Sub, NATS, …
    }
}
```

## See Also

- [messaging-kafka/README.md](../messaging-kafka/README.md) — Kafka publisher module
- [messaging-rabbit/README.md](../messaging-rabbit/README.md) — RabbitMQ publisher module
- [samples/README.md](../samples/README.md) — runnable sample apps (including forward-only producer examples)
