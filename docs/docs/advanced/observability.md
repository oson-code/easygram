---
id: observability
title: Observability
---

# Observability

Easygram's `core-observability` module integrates with Micrometer for metrics and distributed
tracing. It ships three auto-configured components: a **`BotObservabilityFilter`** that wraps
every update in a Micrometer `Observation`, a **`BotHealthIndicator`** that reports the bot's
health at `/actuator/health`, and a **`BotInfoContributor`** that exposes bot metadata at
`/actuator/info`.

## Add Dependencies

`spring-boot-starter` includes `core-observability` automatically. For individual modules:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>core-observability</artifactId>
    <version>0.0.2</version>
</dependency>

<!-- Spring Boot Actuator — health, info, prometheus endpoints -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Actuator Setup

Expose the endpoints you need and enable percentile histograms for accurate P95/P99 latency:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always      # shows bot id, username, transport
  metrics:
    distribution:
      # Enable histogram buckets so Grafana can compute histogram_quantile()
      percentiles-histogram:
        telegram.bot.update: true
```

---

## Built-in Components

### BotHealthIndicator

Auto-registered bean that reports bot health at `/actuator/health` once the bot has
authenticated with the Telegram Bot API (i.e. `GetMe` completed).

```json
{
  "status": "UP",
  "components": {
    "bot": {
      "status": "UP",
      "details": {
        "id": 123456789,
        "username": "my_awesome_bot",
        "firstName": "MyBot",
        "transport": "LONG_POLLING"
      }
    }
  }
}
```

Reports `UNKNOWN` while the bot is still initializing (metadata not yet populated).

### BotInfoContributor

Auto-registered bean that adds a `telegram-bot` section to `/actuator/info`:

```json
{
  "telegram-bot": {
    "id": 123456789,
    "username": "my_awesome_bot",
    "firstName": "MyBot",
    "transport": "LONG_POLLING"
  }
}
```

Both components are skipped if the bot has not finished its `GetMe` call.
Override them with your own `@Bean` of the same type if you need custom logic.

---

## Built-in Micrometer Observation

`BotObservabilityFilter` (order `BotFilterOrder.OBSERVATION`) wraps the entire update
processing chain in a Micrometer `Observation`. Every update is automatically timed and, when a
tracing bridge is on the classpath, traced.

### Metric name

| Micrometer name | Prometheus series |
|---|---|
| `telegram.bot.update` | `telegram_bot_update_seconds_count` |
| | `telegram_bot_update_seconds_sum` |
| | `telegram_bot_update_seconds_max` |
| | `telegram_bot_update_seconds_bucket` (when histogram enabled) |

### Tags

**Low-cardinality** (present on both metrics and spans):

| Tag | Values | Description |
|---|---|---|
| `update_type` | `message`, `callback_query`, `inline_query`, `edited_message`, `channel_post`, `poll`, `poll_answer`, `my_chat_member`, `chat_member`, `chat_join_request`, `business_connection`, `business_message`, `edited_business_message`, `deleted_business_message`, `paid_media_purchased`, … | Type of the incoming Telegram Update |
| `transport_type` | `LONG_POLLING`, `WEBHOOK`, `KAFKA_CONSUMER`, `RABBIT_CONSUMER` | Active transport |

**High-cardinality** (present in spans/traces only — not in Prometheus labels):

| Tag | Description |
|---|---|
| `user_id` | Telegram user ID (when resolvable) |
| `chat_id` | Telegram chat ID (when resolvable) |

---

## Prometheus Integration

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Metrics are exposed at `/actuator/prometheus`. Prometheus scrape config:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: telegram-bot
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['localhost:8080']
    scrape_interval: 15s
```

### Example Prometheus queries

```promql
# Update throughput (req/s, last 5 minutes)
sum(rate(telegram_bot_update_seconds_count[5m]))

# Throughput by update type
sum by (update_type) (rate(telegram_bot_update_seconds_count[5m]))

# Average processing time
rate(telegram_bot_update_seconds_sum[5m]) / rate(telegram_bot_update_seconds_count[5m])

# P95 latency (requires percentiles-histogram: true)
histogram_quantile(0.95, sum by (le) (rate(telegram_bot_update_seconds_bucket[5m])))

# Error rate (updates that threw an exception)
sum(rate(telegram_bot_update_seconds_count{error!="none",error!=""}[5m]))
```

---

## Prometheus + Grafana Quick Start

The `samples/i18n-registration-bot` sample includes a ready-to-use observability stack with
a pre-built Grafana dashboard. Use it as a reference or copy it into your own project.

```
samples/i18n-registration-bot/
├── docker-compose.yml                              # bot + Prometheus + Grafana
├── prometheus.yml                                  # scrape config
└── grafana/
    ├── provisioning/
    │   ├── datasources/prometheus.yml              # auto-provision Prometheus datasource
    │   └── dashboards/dashboard.yml                # auto-provision dashboards directory
    └── dashboards/
        └── easygram-bot.json                       # 8-panel Grafana dashboard
```

### Dashboard panels

| Panel | Query |
|---|---|
| Total updates | `sum(telegram_bot_update_seconds_count)` |
| Update rate | `rate(telegram_bot_update_seconds_count[5m])` |
| Average processing time | `rate(sum) / rate(count)` |
| Bot health | `up{job="…"}` |
| Update rate by type | grouped by `update_type` |
| P50/P95/P99 latency | `histogram_quantile(0.50/0.95/0.99, …)` |
| Error rate | `update_type` with `error` tag set |
| Max latency by type | `telegram_bot_update_seconds_max` |

To spin up the full stack:

```bash
cd samples/i18n-registration-bot
TELEGRAM_BOT_TOKEN=xxx docker compose up
# Grafana: http://localhost:3000  (admin / admin)
# Prometheus: http://localhost:9090
```

---

## Distributed Tracing

Add Micrometer Tracing with Brave/Zipkin:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # 100% in dev; reduce to 0.1 in production
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

Every update processed through the filter chain gets a `telegram.bot.update` span automatically.

---

## Adding Custom Metrics

Inject `MeterRegistry` into any filter or handler:

```java
@Component
public class CommandMetricsFilter implements BotFilter {

    private final MeterRegistry registry;

    public CommandMetricsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain)
            throws Exception {
        Update update = request.getUpdate();
        if (update.hasMessage() && update.getMessage().isCommand()) {
            String command = update.getMessage().getText().split(" ")[0];
            registry.counter("bot.commands", "command", command).increment();
        }
        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return BotFilterOrder.CONTEXT_SETTER + 5;
    }
}
```

---

## Pub/Sub Trace Propagation

When using Kafka or RabbitMQ transport, Easygram automatically propagates W3C `traceparent`
headers through the broker when a Micrometer Tracing bridge is configured.

### Span tree

```
[producer service]
  telegram.bot.update  (BotObservabilityFilter)
    spring.kafka.producer  (KafkaTemplate — observationEnabled=true)
           ↓ W3C traceparent in Kafka record

[consumer service]
  spring.kafka.consumer  (listener container — observationEnabled=true)
    telegram.bot.update  (BotObservabilityFilter — child span)
```

The same pattern applies for RabbitMQ (`spring.rabbit.producer` / `spring.rabbit.listener`).

Easygram automatically enables `observationEnabled=true` on listener container factories when
`ObservationRegistry` is present — no extra configuration needed.

### Disabling propagation

Register your own factory bean to opt out:

```java
@Bean(name = "botKafkaListenerContainerFactory")
public ConcurrentKafkaListenerContainerFactory<Object, Object> customKafkaFactory(
        ConsumerFactory<Object, Object> cf) {
    var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
    factory.setConsumerFactory(cf);
    // observation intentionally disabled
    return factory;
}
```

---

## Structured Log Correlation

Enable trace/span IDs in log lines (requires Micrometer Tracing configured):

```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss} %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
```

---

## Feature Summary

| Capability | How to enable |
|---|---|
| Update timing + error rate | Automatic via `BotObservabilityFilter` |
| Health endpoint | Automatic via `BotHealthIndicator` (UP/UNKNOWN) |
| Info endpoint | Automatic via `BotInfoContributor` |
| Prometheus metrics | Add `micrometer-registry-prometheus` |
| P95/P99 latency | Add `percentiles-histogram.telegram.bot.update: true` |
| Grafana dashboard | Copy from `samples/i18n-registration-bot/grafana/` |
| Distributed tracing | Add `micrometer-tracing-bridge-brave` + Zipkin |
| Pub/sub trace propagation | Automatic when `ObservationRegistry` bean is present |
| Custom counters/timers | Inject `MeterRegistry` |
| Structured logging | Configure Logback with MDC trace pattern |

---

See also:
- [Custom Filters](./custom-filters) — add cross-cutting metrics in a filter
- [Architecture](../architecture) — `BotFilterOrder.OBSERVATION` in the pipeline
- [RabbitMQ Consumer](../transports/rabbitmq-consumer-guide) — trace propagation details
- [Kafka Consumer](../transports/kafka-consumer-guide) — trace propagation details
