# messaging-api

> SPI module providing the `BotUpdatePublisher` interface and `BotUpdatePublishingFilter` for the Easygram framework.

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-api</artifactId>
    <version>0.0.2</version>
</dependency>
```

## What It Does

Defines the publisher contract used by all broker implementations. The `BotUpdatePublishingFilter` runs at `Integer.MIN_VALUE + 1000` (very early in the filter chain) to ensure every incoming `Update` is captured and forwarded before any business logic executes. Broker-specific implementations (Kafka, RabbitMQ) live in separate modules.

## Custom Publisher

Add only the `messaging-api` module (without Kafka/RabbitMQ dependencies) when implementing your own publisher:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-api</artifactId>
    <version>0.0.2</version>
</dependency>
```

Then implement and register your publisher:

```java
@Component
public class MyCustomPublisher implements BotUpdatePublisher {

    @Override
    public void publish(Update update) {
        // publish to any system — SNS, SQS, Redis Pub/Sub, NATS, …
    }
}
```

## Forward-Only Mode

By default, updates are both published to the broker **and** processed locally by bot handlers. Set `forward-only: true` to publish only, skipping all local bot handlers entirely:

```yaml
telegram:
  bot:
    messaging:
      forward-only: false           # false = publish AND process locally (default)
                                    # true  = publish ONLY, skip bot handlers
      producer:
        producer-type: kafka        # kafka | rabbit
```

This is useful when running a dedicated forwarding instance (see the producer examples in [samples/README.md](../samples/README.md)) that should not handle updates itself.

## See Also

- [messaging-kafka/README.md](../messaging-kafka/README.md) — Kafka publisher implementation
- [messaging-rabbit/README.md](../messaging-rabbit/README.md) — RabbitMQ publisher implementation
- [messaging-producer/README.md](../messaging-producer/README.md) — smart broker routing
