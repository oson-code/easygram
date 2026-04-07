---
id: broker-publishing
title: Broker Publishing
---

# Broker Publishing

Easygram can forward every incoming Telegram update to a message broker (Apache Kafka or
RabbitMQ) before — or instead of — running local handlers. This enables event-driven
architectures where multiple downstream services consume the same Telegram updates.

## Architecture

```
Telegram
  ↓ transport (long-polling or webhook)
BotContextSetterFilter
  ↓
BotUpdatePublishingFilter ← messaging-producer
   KafkaBotUpdatePublisher (messaging-kafka)
   RabbitBotUpdatePublisher (messaging-rabbit)
  ↓
  forward-only: true → STOP (local handlers skipped)
  forward-only: false → continue to BotDispatcher
```

## Dependencies

`spring-boot-starter` includes all messaging modules. For targeted setups:

```xml
<!-- Kafka publishing -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-kafka</artifactId>
    <version>0.0.3</version>
</dependency>

<!-- RabbitMQ publishing -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-rabbit</artifactId>
    <version>0.0.3</version>
</dependency>

<!-- Smart routing: Kafka OR RabbitMQ based on a single property -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-producer</artifactId>
    <version>0.0.3</version>
</dependency>
```

## Kafka Producer Configuration

```yaml
easygram:
  messaging:
    forward-only: true # Skip local handlers; updates go to Kafka only
    producer:
      producer-type: kafka
    kafka:
      topic: easygram-updates
      create-if-absent: true # Auto-create topic on startup
      partitions: 1
      replication-factor: 1

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
```

## RabbitMQ Producer Configuration

```yaml
easygram:
  messaging:
    forward-only: true
    producer:
      producer-type: rabbit
    rabbit:
      exchange: easygram-exchange
      routing-key: easygram.updates
      queue: easygram-updates
      create-if-absent: true # Auto-create exchange, queue, and binding

spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
```

## forward-only Mode

| `forward-only` | Effect |
|---|---|
| `true` | Updates are published to the broker. The `BotDispatcher` (and all local `@BotController` handlers) is **never called**. The bot acts as a pure ingest gateway. |
| `false` | Updates are published to the broker **and** the local handler pipeline also runs. Useful for hybrid setups (quick local reply + broker for analytics). |

**Pure ingest gateway** — all processing in downstream services:
```yaml
easygram:
  messaging:
    forward-only: true
```

**Hybrid** — local quick-reply + broker for analytics / audit:
```yaml
easygram:
  messaging:
    forward-only: false
```

## Message Format

Updates are serialized as JSON using the configured `ObjectMapper`. Example Kafka message:

```json
{
  "update_id": 123456789,
  "message": {
    "message_id": 42,
    "from": { "id": 12345, "first_name": "Alice", "language_code": "en" },
    "chat": { "id": 12345, "type": "private" },
    "date": 1718000000,
    "text": "/start"
  }
}
```

- **Kafka key**: `update_id` as a string
- **RabbitMQ routing key**: configured via `rabbit.routing-key`

## Custom BotUpdatePublisher

To publish to a different broker (Google Pub/Sub, Amazon SNS, NATS, etc.), implement
`BotUpdatePublisher`:

```java
public interface BotUpdatePublisher {
    void publish(BotRequest request) throws Exception;
}
```

Register as a bean:

```java
@Bean
@ConditionalOnMissingBean(BotUpdatePublisher.class)
public BotUpdatePublisher pubSubPublisher(PubSubTemplate pubSub,
                                          ObjectMapper objectMapper) {
    return request -> {
        String json = objectMapper.writeValueAsString(request.getUpdate());
        pubSub.publish("easygram-updates", json).get();
    };
}
```

## Consuming Updates from the Broker

Use the consumer transport modules to process updates through the full bot pipeline:

```yaml
# Consumer application.yml
easygram:
  transport: KAFKA_CONSUMER # or RABBIT_CONSUMER
  messaging:
    kafka:
      topic: easygram-updates
      group-id: my-bot-consumer-group

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
```

See the [Kafka Consumer Guide](../transports/kafka-consumer-guide) and
[RabbitMQ Consumer Guide](../transports/rabbitmq-consumer-guide) for complete configuration.

## Error Handling

- **Kafka**: Spring Kafka retries according to `spring.kafka.producer.retries`. On exhaustion
  the exception is logged and the update is dropped.
- **RabbitMQ**: Spring AMQP retries according to `spring.rabbitmq.template.retry`.

:::warning
Both built-in publishers are fire-and-forget. Configure a Dead-Letter Topic/Queue for
workloads where update loss is unacceptable.
:::

## Full Example

See [Kafka Producer Bot Example](../examples/kafka-producer-bot) for a complete runnable sample.

---

See also:
- [Kafka Consumer Guide](../transports/kafka-consumer-guide)
- [RabbitMQ Consumer Guide](../transports/rabbitmq-consumer-guide)
- [Kafka Producer Bot Example](../examples/kafka-producer-bot)
