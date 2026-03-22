---
id: observability
title: Observability
---

# Observability

Easygram's `core-observability` module integrates with Micrometer for metrics and distributed
tracing. Combined with Spring Boot Actuator, you get health indicators, Prometheus metrics, and
trace context propagation with minimal configuration.

## Add Dependencies

`spring-boot-starter` includes `core-observability` automatically. For individual modules:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>core-observability</artifactId>
    <version>0.0.1</version>
</dependency>

<!-- Spring Boot Actuator (health, metrics, prometheus endpoints) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Actuator Setup

Expose the endpoints you need:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
```

## Built-in Micrometer Observation

The `BotObservationFilter` (order `BotFilterOrder.OBSERVATION = Integer.MIN_VALUE + 1`) wraps
the entire update processing chain in a Micrometer `Observation`. Every update is automatically
timed and recorded.

### Built-in Metrics

| Metric name | Type | Description |
|---|---|---|
| `bot.update.duration` | Timer | Full processing time per update |
| `bot.update.errors` | Counter | Number of unhandled exceptions |

Metrics include the `application` tag set in `management.metrics.tags`.

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
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: /actuator/prometheus
    scrape_interval: 15s
```

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
      probability: 1.0   # 100% sampling in dev; reduce to 0.1 in production
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

Every update processed through the filter chain gets a trace span automatically.

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

## Custom Health Indicator

Verify the Telegram API connection is reachable:

```java
@Component
public class BotHealthIndicator implements HealthIndicator {

    private final TelegramClient telegramClient;

    public BotHealthIndicator(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public Health health() {
        try {
            User bot = telegramClient.execute(new GetMe());
            return Health.up()
                    .withDetail("botUsername", bot.getUserName())
                    .withDetail("botId", bot.getId())
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
```

Response at `/actuator/health`:

```json
{
  "status": "UP",
  "components": {
    "bot": {
      "status": "UP",
      "details": {
        "botUsername": "my_awesome_bot",
        "botId": 1234567890
      }
    }
  }
}
```

## Prometheus + Grafana with Docker Compose

```yaml
version: '3.8'
services:
  bot:
    image: my-bot:latest
    environment:
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,metrics,prometheus
    ports: ["8080:8080"]

  prometheus:
    image: prom/prometheus:latest
    volumes: [./prometheus.yml:/etc/prometheus/prometheus.yml]
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana:latest
    ports: ["3000:3000"]
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
```

## Pub/Sub Trace Propagation

When using Kafka or RabbitMQ transport (the `messaging-kafka` / `messaging-rabbit` + consumer
modules), Easygram automatically propagates W3C `traceparent` headers through the broker **as
long as Micrometer Tracing is configured** (i.e. an `ObservationRegistry` bean is present).

### How it works

The filter pipeline on the **producer** side executes in this order:

```
CONTEXT_SETTER (MIN_VALUE)
  → OBSERVATION (MIN_VALUE + 1)   ← telegram.bot.update span starts HERE
  → ...
  → PUBLISHING (MIN_VALUE + 1000) ← KafkaTemplate / RabbitTemplate sends the message
```

Because `BotObservabilityFilter` runs _before_ `BotUpdatePublishingFilter`, an active span
already exists when the template publishes. With observation enabled on the template, Spring
automatically injects a `traceparent` header into every outgoing Kafka record / AMQP message.

On the **consumer** side, the listener container (configured by Easygram with
`observationEnabled=true`) extracts the `traceparent` header and creates a linked span before
dispatching the message to the bot.

### Resulting span tree

```
[producer service]
  telegram.bot.update  (BotObservabilityFilter)
    └── spring.kafka.producer  (KafkaTemplate with observationEnabled=true)
             ↓  W3C traceparent header in Kafka record

[consumer service]
  spring.kafka.consumer  (KafkaListenerContainerFactory with observationEnabled=true)
    └── telegram.bot.update  (BotObservabilityFilter — child of kafka.consumer span)
```

The same pattern applies for RabbitMQ (`spring.rabbit.producer` / `spring.rabbit.listener`).

### No extra configuration needed

Easygram registers `botKafkaListenerContainerFactory` and `botRabbitListenerContainerFactory`
automatically, conditioned on `ObservationRegistry` being present:

| Condition | Template observation | Container factory observation |
|---|---|---|
| No `ObservationRegistry` bean | disabled (default) | disabled (default) |
| `ObservationRegistry` present | ✅ enabled automatically | ✅ enabled automatically |

You only need to add the tracing bridge (Brave or OTel) to your application — nothing extra in
the consumer or producer modules:

```xml
<!-- Add to your consumer and producer applications -->
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
      probability: 1.0
```

### Disabling propagation

To opt out of observation on the transport layer while keeping `core-observability` active,
register your own beans:

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

`@ConditionalOnMissingBean(name = "botKafkaListenerContainerFactory")` ensures Easygram's
auto-configured factory is skipped when yours is present.



Enable trace context in log lines (requires Micrometer Tracing configured above):

```yaml
logging:
  level:
    uz.osoncode.easygram: INFO
    uz.example: DEBUG
  pattern:
    console: "%d{HH:mm:ss} %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
```

The `traceId` and `spanId` MDC fields are populated automatically by Micrometer Tracing.

## Feature Summary

| Capability | How to enable |
|---|---|
| Update timing | Automatic via `BotObservationFilter` |
| Prometheus metrics | Add `micrometer-registry-prometheus` |
| Distributed tracing | Add `micrometer-tracing-bridge-brave` + Zipkin |
| Pub/sub trace propagation | Automatic when `ObservationRegistry` bean is present |
| Custom counters/timers | Inject `MeterRegistry` |
| Health endpoint | Implement `HealthIndicator` + register as `@Bean` |
| Structured logging | Configure Logback with MDC trace pattern |

---

See also:
- [Custom Filters](./custom-filters) — add cross-cutting metrics in a filter
- [Architecture](../architecture) — `BotFilterOrder.OBSERVATION` in the pipeline
