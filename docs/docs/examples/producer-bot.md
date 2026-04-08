---
id: producer-bot
title: Docker Producer Bot
description: Docker-ready Easygram producer bot image — run with environment variables to forward Telegram updates to Kafka or RabbitMQ. Long-polling and webhook options with Docker Compose.
keywords: [easygram docker producer bot, telegram bot docker kafka, telegram bot docker rabbitmq, easygram docker image, telegram bot container spring boot]
---

# Docker-Ready Producer Bot

The `samples/producer-bot` module provides a **zero-code**, Docker-ready Telegram bot that
forwards every incoming update to Apache Kafka or RabbitMQ. It ships with two
`docker-compose` files — one for each broker — so the only thing you need to provide is
a Telegram bot token.

**Full source:** [`samples/producer-bot`](https://github.com/oson-code/easygram/tree/main/samples/producer-bot)

## What It Does

- Receives updates from Telegram via **long-polling** (default) or webhook
- Publishes each update as JSON to a Kafka topic or RabbitMQ exchange
- Local `@BotController` handlers are **never called** (`forward-only: true`)
- Works as a pure ingest gateway; downstream services handle all bot logic

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- A Telegram bot token from [@BotFather](https://t.me/BotFather)

### 1. Clone and enter the module

```bash
git clone https://github.com/oson-code/easygram.git
cd easygram/samples/producer-bot
```

### 2. Create your `.env` file

```bash
cp .env.example .env
# Edit .env and set your bot token:
# easygram.token=1234567890:AAFxxxxxxxxxxxxxx
```

### 3. Start with Kafka

```bash
docker compose -f docker-compose.kafka.yml up
```

This starts:
- **Kafka** (Bitnami KRaft — no ZooKeeper) on port 9092
- **Kafka UI** at [http://localhost:8090](http://localhost:8090)
- **Bot** — long-polling, forwarding updates to topic `easygram-updates`

### 4. Start with RabbitMQ

```bash
docker compose -f docker-compose.rabbit.yml up
```

This starts:
- **RabbitMQ** with management UI at [http://localhost:15672](http://localhost:15672) (user: `rabbit`, pass: `rabbit`)
- **Bot** — long-polling, forwarding updates to exchange `easygram-exchange`

## Configuration

All configuration is passed as Spring property names in the compose file's `environment`
block. Override any value in your `.env` file.

### Kafka Defaults

| Property | Default | Description |
|---|---|---|
| `easygram.token` | *(required)* | Bot API token from @BotFather |
| `easygram.update.transport` | `LONG_POLLING` | Update source: `LONG_POLLING` or `WEBHOOK` |
| `easygram.messaging.type` | `PRODUCER` | Broker role |
| `easygram.messaging.forward-only` | `true` | Skip local handlers |
| `easygram.messaging.producer.type` | `KAFKA` | Broker backend |
| `easygram.messaging.kafka.topic` | `easygram-updates` | Target Kafka topic |
| `spring.kafka.bootstrap-servers` | `kafka:9092` | Kafka broker address (Docker service name) |

### RabbitMQ Defaults

| Property | Default | Description |
|---|---|---|
| `easygram.token` | *(required)* | Bot API token from @BotFather |
| `easygram.update.transport` | `LONG_POLLING` | Update source |
| `easygram.messaging.type` | `PRODUCER` | Broker role |
| `easygram.messaging.forward-only` | `true` | Skip local handlers |
| `easygram.messaging.producer.type` | `RABBIT` | Broker backend |
| `easygram.messaging.rabbit.exchange` | `easygram-exchange` | Target exchange |
| `easygram.messaging.rabbit.queue` | `easygram-updates` | Queue bound to exchange |
| `easygram.messaging.rabbit.routing-key` | `easygram.updates` | Routing key |
| `spring.rabbitmq.host` | `rabbitmq` | RabbitMQ host (Docker service name) |
| `spring.rabbitmq.username` | `rabbit` | RabbitMQ username |
| `spring.rabbitmq.password` | `rabbit` | RabbitMQ password |

## Advanced: Webhook as Update Source

To receive updates via webhook instead of long-polling, add these lines to your `.env`:

```properties
easygram.update.transport=WEBHOOK
easygram.update.webhook.url=https://your-domain.com/webhook
easygram.update.webhook.secret-token=your-webhook-secret
```

Then expose port 8080 in the compose file and configure your reverse proxy or TLS terminator.

## Application Code

The application class contains no controllers — framework defaults handle everything:

```java
@SpringBootApplication
public class ProducerBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProducerBotApplication.class, args);
    }
}
```

## MDC Correlation Logging

The bot uses `logback-spring.xml` with MDC-aware patterns. Every log line for a given update
automatically includes:

```
[upd:12345678] [chat:987654321] [uid:111222333] [transport:LONG_POLLING]
```

This makes it easy to trace a specific update through your logs when debugging.

## Consuming the Updates

Use a Kafka or RabbitMQ consumer bot to process the forwarded updates. See:

- [Kafka Consumer Transport](../transports/kafka-consumer-guide)
- [RabbitMQ Consumer Transport](../transports/rabbitmq-consumer-guide)

---

See also:
- [Broker Publishing](../advanced/broker-publishing) — producer configuration reference
- [Kafka Producer Bot Example](./kafka-producer-bot) — code-level walkthrough
