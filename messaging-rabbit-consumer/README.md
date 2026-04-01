# messaging-rabbit-consumer

> RabbitMQ consumer transport for the Easygram framework.
> Receives Telegram updates from a RabbitMQ queue and feeds them into the bot handler pipeline.

## Maven Dependency

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

## What It Does

Activates when `telegram.bot.transport=RABBIT_CONSUMER`. Reads JSON-serialised `Update` objects from the configured RabbitMQ queue using a `@RabbitListener` and deserialises each message before passing it to `Bot.handleUpdate()`. This lets you decouple update ingestion (handled by a separate producer process) from update processing (your bot logic).

## Architecture

```
[messaging-rabbit (producer side)]
         │
         ▼ JSON Update (routing-key: telegram.updates)
[RabbitMQ Exchange: telegram-exchange]
         │  binding
         ▼
[RabbitMQ Queue: telegram-updates]
         │
         ▼  @RabbitListener
[RabbitBotUpdateListener]
         │ deserialise Update
         ▼
[Bot.handleUpdate()]
         │
         ▼
[BotFilter chain → BotDispatcher → @BotController handlers]
```

## Transport Selection

Set `telegram.bot.transport=RABBIT_CONSUMER` to activate this transport.

## Configuration

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: RABBIT_CONSUMER
    rabbit-consumer:
      queue: telegram-updates
      exchange: telegram-exchange      # default: telegram-exchange
      routing-key: telegram.updates    # default: telegram.updates
      create-if-absent: true           # default: true

spring:
  rabbitmq:
    host: rabbitmq
    username: admin
    password: secret
```

## All Properties

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.token` | ✅ | — | Bot token (shared across transports) |
| `telegram.bot.rabbit-consumer.queue` | ✅ | — | Queue to consume from |
| `telegram.bot.rabbit-consumer.exchange` | ❌ | `telegram-exchange` | Exchange used for auto-creation |
| `telegram.bot.rabbit-consumer.routing-key` | ❌ | `telegram.updates` | Routing key used for auto-creation |
| `telegram.bot.rabbit-consumer.create-if-absent` | ❌ | `true` | Auto-create exchange/queue/binding if absent |
| `spring.rabbitmq.host` | ✅ | `localhost` | Broker host |
| `spring.rabbitmq.port` | ❌ | `5672` | AMQP port |
| `spring.rabbitmq.username` | ❌ | `guest` | Username |
| `spring.rabbitmq.password` | ❌ | `guest` | Password |

## Queue and Exchange Topology

When `create-if-absent: true` (the default), the framework auto-declares on startup:

- A **durable topic exchange** named by `exchange` (default: `telegram-exchange`)
- A **durable queue** named by `queue` (default: `telegram-updates`)
- A **binding** between them using `routing-key` (default: `telegram.updates`)

These declarations are idempotent — if the resources already exist with the same definition, no error is thrown. Set `create-if-absent: false` to skip auto-declaration (e.g. when exchange/queue topology is managed by Terraform or Ansible).

## Concurrency Model

By default, the `@RabbitListener` uses a single consumer thread. Each message is deserialized and handed to the `ExecutorService` provided by `BotExecutorServiceProvider` (default: `newSingleThreadExecutor()`), meaning updates are processed sequentially.

To handle updates concurrently, override `BotExecutorServiceProvider`:

```java
@Bean
public BotExecutorServiceProvider botExecutorServiceProvider() {
    ExecutorService pool = Executors.newFixedThreadPool(4);
    return () -> pool;
}
```

You can also configure the `@RabbitListener` concurrency via Spring AMQP's `concurrency` property to consume multiple messages from the queue in parallel.

## Deserialization Error Handling

If a RabbitMQ message cannot be deserialized to an `Update` (e.g. malformed JSON), the exception is propagated by the `@RabbitListener`. With Spring AMQP's default error handling, the message is nacked and re-queued. Configure a `SimpleRabbitListenerContainerFactory` with a custom `MessageRecoverer` (e.g. `RepublishMessageRecoverer`) to route failed messages to a dead-letter exchange instead of re-queuing them indefinitely.

## Customizing Infrastructure

Every infrastructure concern is exposed as a separate `@ConditionalOnMissingBean` provider bean.
Declare only the ones you need to override — everything else keeps its default.

### Available providers

| Provider interface | Default | Override example use-case |
|---|---|---|
| `BotTelegramClientProvider` | `OkHttpTelegramClient` | Custom client implementation |
| `BotExecutorServiceProvider` | `newSingleThreadExecutor()` | Fixed thread pool for update handling |
| `BotOkHttpClientProvider` | `new OkHttpClient()` | Custom timeouts / proxy |
| `BotTelegramUrlProvider` | `TelegramUrl.DEFAULT_URL` | Test / local Telegram mock |
| `BotObjectMapperProvider` | shared Spring `ObjectMapper` | Custom serialisation modules |

### Example: fixed thread pool for concurrent update handling

```java
@Configuration
public class BotConfig {

    @Bean
    public BotExecutorServiceProvider botExecutorServiceProvider() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        return () -> pool;
    }
}
```

Declaring any `@Bean` of a provider type replaces only that provider; all other defaults remain.

## See Also

- [messaging-rabbit/README.md](../messaging-rabbit/README.md) — publish updates to RabbitMQ
- [messaging-producer/README.md](../messaging-producer/README.md) — smart broker-routing producer
- [samples/README.md](../samples/README.md) — runnable sample apps (including forward-only producer examples)

