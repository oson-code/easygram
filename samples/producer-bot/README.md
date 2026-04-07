# producer-bot

A Docker-ready Telegram bot that forwards every incoming update to **Kafka** or **RabbitMQ**.  
No code required — configure everything via Spring property names in your `.env` file.

Supports all four deployment combinations:

| Update source | Broker |
|---|---|
| Long-polling *(default)* | Kafka *(default)* |
| Long-polling | RabbitMQ |
| Webhook | Kafka |
| Webhook | RabbitMQ |

---

## Quick start

### 1. Prepare your `.env`

```bash
cp .env.example .env
# Edit .env and set your bot token:
# easygram.token=<your_token>
```

### 2. Run — Long-polling → Kafka (default)

```bash
docker compose -f docker-compose.kafka.yml up --build
```

Kafka UI is available at **http://localhost:8090**.

### 3. Run — Long-polling → RabbitMQ

```bash
docker compose -f docker-compose.rabbit.yml up --build
```

RabbitMQ Management UI is available at **http://localhost:15672** (`rabbit` / `rabbit`).

### 4. Run — Webhook → Kafka

Add to `.env`:
```
easygram.update.transport=WEBHOOK
easygram.update.webhook.url=https://your-domain.com/webhook
```

Then:
```bash
docker compose -f docker-compose.kafka.yml up --build
```

Port `8080` is exposed for the webhook endpoint.

### 5. Run — Webhook → RabbitMQ

Add to `.env`:
```
easygram.update.transport=WEBHOOK
easygram.update.webhook.url=https://your-domain.com/webhook
easygram.messaging.producer.type=RABBIT
```

Then:
```bash
docker compose -f docker-compose.rabbit.yml up --build
```

---

## Property reference

All properties are Spring Boot property names — set them directly in `.env` or as container
environment variables. See `.env.example` for the complete list with defaults.

| Property | Default | Description |
|---|---|---|
| `easygram.token` | *(required)* | Bot token from @BotFather |
| `easygram.update.transport` | `LONG_POLLING` | `LONG_POLLING` or `WEBHOOK` |
| `easygram.update.webhook.url` | *(required for WEBHOOK)* | Public HTTPS URL |
| `easygram.update.webhook.path` | `/webhook` | Local endpoint path |
| `easygram.update.webhook.secret-token` | — | Optional HMAC header secret |
| `easygram.messaging.producer.type` | `KAFKA` | `KAFKA` or `RABBIT` |
| `easygram.messaging.forward-only` | `true` | Skip local handlers after publish |
| `easygram.messaging.kafka.topic` | `easygram-updates` | Kafka topic name |
| `easygram.messaging.kafka.create-if-absent` | `true` | Auto-create topic |
| `spring.kafka.bootstrap-servers` | `kafka:9092` | Kafka bootstrap servers |
| `easygram.messaging.rabbit.exchange` | `easygram-exchange` | RabbitMQ exchange |
| `easygram.messaging.rabbit.queue` | `easygram-updates` | RabbitMQ queue |
| `easygram.messaging.rabbit.routing-key` | `easygram.updates` | Routing key |
| `spring.rabbitmq.host` | `rabbitmq` | RabbitMQ hostname |
| `spring.rabbitmq.port` | `5672` | RabbitMQ port |
| `spring.rabbitmq.username` | `rabbit` | RabbitMQ username |
| `spring.rabbitmq.password` | `rabbit` | RabbitMQ password |

---

## Build manually

```bash
# Build the Docker image
docker build -t easygram-producer-bot .

# Run against an existing Kafka
docker run --rm \
  -e "easygram.token=<token>" \
  -e "easygram.messaging.producer.type=KAFKA" \
  -e "spring.kafka.bootstrap-servers=my-kafka:9092" \
  easygram-producer-bot

# Run against an existing RabbitMQ
docker run --rm \
  -e "easygram.token=<token>" \
  -e "easygram.messaging.producer.type=RABBIT" \
  -e "spring.rabbitmq.host=my-rabbit" \
  easygram-producer-bot
```

---

## How it works

1. The bot starts the configured transport (long-polling or webhook).
2. Every incoming Telegram `Update` passes through `BotUpdatePublishingFilter`.
3. Because `easygram.messaging.forward-only=true`, the update is serialised to JSON
   and published to the broker **without** running any local handler.
4. A downstream consumer bot (see `kafka-consumer-bot` / `rabbit-consumer-bot` samples)
   deserialises the update and dispatches it to `@BotController` handlers.
