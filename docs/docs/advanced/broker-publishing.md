---
id: broker-publishing
title: Broker Publishing
description: Publish Telegram bot updates to Apache Kafka or RabbitMQ using Easygram's messaging API — BotUpdatePublisher, producer configuration, forward-only mode, and multi-transport setup.
keywords: [telegram bot kafka rabbitmq, spring boot telegram message broker, BotUpdatePublisher, easygram messaging, forward telegram updates broker]
---

# Broker Publishing

Easygram can forward every incoming Telegram update to a message broker (Apache Kafka or
RabbitMQ) before — or instead of — running local handlers. This enables event-driven
architectures where multiple downstream services consume the same Telegram updates.

## Architecture

```
Telegram
  ↓ transport (long-polling or webhook)
BotMdcFilter           ← sets MDC: bot.update.id, bot.transport
BotContextSetterFilter ← extracts Chat + User
  ↓
BotUpdatePublishingFilter ← messaging-api (producer auto-config)
   KafkaBotUpdatePublisher  (messaging-api, requires spring-kafka)
   RabbitBotUpdatePublisher (messaging-api, requires spring-amqp)
  ↓
  forward-only: true → STOP (local handlers skipped)
  forward-only: false → continue to BotDispatcher
```

## Dependencies

`spring-boot-starter` includes `messaging-api` automatically. For targeted setups:

```xml
<!-- All broker functionality: Kafka + RabbitMQ publishing and consuming -->
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>messaging-api</artifactId>
    <version>0.0.7</version>
</dependency>

<!-- Required for Kafka: spring-kafka is optional in messaging-api -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Required for RabbitMQ: spring-amqp is optional in messaging-api -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## Kafka Producer Configuration

```yaml
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: PRODUCER
    forward-only: true    # Skip local handlers; updates go to Kafka only
    producer:
      type: KAFKA
    kafka:
      topic: easygram-updates
      create-if-absent: true
      partitions: 1
      replication-factor: 1

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

## RabbitMQ Producer Configuration

```yaml
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: PRODUCER
    forward-only: true
    producer:
      type: RABBIT
    rabbit:
      exchange: easygram-exchange
      routing-key: easygram.updates
      queue: easygram-updates
      create-if-absent: true

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

## Factory Provider SPI

Use the factory provider SPI to inject a fully configured Kafka or RabbitMQ client factory,
replacing the Spring Boot auto-configured defaults. This is useful when you need custom SSL,
SASL, a separate cluster, or specific serializer configuration.

All three providers are `@ConditionalOnMissingBean` — declare only the ones you need.

### `EasygramKafkaProducerFactoryProvider`

Replaces the `ProducerFactory` used to build the internal Kafka template:

```java
@Bean
public EasygramKafkaProducerFactoryProvider easygramKafkaProducerFactoryProvider() {
    return () -> {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-cluster:9093");
        props.put(ProducerConfig.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        // ... additional SASL / SSL settings
        return new DefaultKafkaProducerFactory<>(props,
            new StringSerializer(), new StringSerializer());
    };
}
```

### `EasygramKafkaConsumerFactoryProvider`

Replaces the `ConsumerFactory` used to build the Kafka listener container:

```java
@Bean
public EasygramKafkaConsumerFactoryProvider easygramKafkaConsumerFactoryProvider() {
    return () -> {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-cluster:9093");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-custom-group");
        return new DefaultKafkaConsumerFactory<>(props,
            new StringDeserializer(), new StringDeserializer());
    };
}
```

### `EasygramRabbitConnectionFactoryProvider`

Replaces the RabbitMQ `ConnectionFactory` for both publishing and consuming:

```java
@Bean
public EasygramRabbitConnectionFactoryProvider easygramRabbitConnectionFactoryProvider() {
    return () -> {
        CachingConnectionFactory factory = new CachingConnectionFactory("rabbit-cluster");
        factory.setVirtualHost("/my-vhost");
        factory.setUsername("bot-user");
        factory.setPassword("secret");
        return factory;
    };
}
```

*Since 0.0.6*

## Consuming Updates from the Broker

Use the consumer transport to process updates through the full bot pipeline. A consumer bot
does **not** poll Telegram directly — it reads from the broker topic/queue:

```yaml
# Consumer application.yml (Kafka)
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: CONSUMER
    consumer:
      type: KAFKA
    kafka:
      topic: easygram-updates
      group-id: my-bot-consumer-group

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

```yaml
# Consumer application.yml (RabbitMQ)
easygram:
  token: ${BOT_TOKEN}
  messaging:
    type: CONSUMER
    consumer:
      type: RABBIT
    rabbit:
      exchange: easygram-exchange
      queue: easygram-updates

spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
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
