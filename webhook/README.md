# webhook

> Webhook transport for the Easygram framework.
> Registers a Spring MVC endpoint and receives updates pushed by Telegram over HTTPS.

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>webhook</artifactId>
    <version>0.0.4</version>
</dependency>
```

> Webhook requires `spring-boot-starter-web` on the classpath and a public HTTPS URL.

## What It Does

The `webhook` module autoconfigures a Spring MVC controller that listens at a configurable path for incoming `Update` POSTs from Telegram. On startup it registers the webhook URL with Telegram (and optionally validates requests via a secret token). Each received `Update` is dispatched into the bot's filter and handler pipeline.

## How It Works

1. **Startup:** `WebhookBot.afterPropertiesSet()` calls Telegram's `setWebhook` method, registering `webhook.url` as the endpoint Telegram should call for every new update
2. **Receiving updates:** Telegram POSTs a JSON `Update` to `webhook.path` (default `/webhook`); Spring MVC deserializes it and hands it to the handler pipeline
3. **Shutdown:** If `unregister-on-shutdown: true`, `WebhookBot` calls `deleteWebhook` on graceful shutdown, so Telegram stops sending updates to this URL

## Security: Why `secret-token` Is Important

Without a secret token, any party who guesses your webhook URL can send arbitrary JSON payloads to your bot. The `secret-token` option instructs Telegram to include an `X-Telegram-Bot-Api-Secret-Token` header on every request it sends. The framework validates this header and rejects requests that don't match, ensuring only Telegram can trigger your bot.

```yaml
telegram:
  bot:
    webhook:
      secret-token: ${WEBHOOK_SECRET}   # generate a long random string
```

Use a cryptographically random value (e.g. `openssl rand -hex 32`) and store it as an environment variable or secret.

## HTTPS Requirement

Telegram requires that webhook URLs use HTTPS with a valid TLS certificate. Options:

- **Public domain with a CA certificate** — standard production setup
- **Self-signed certificate** — supported by Telegram if you upload the certificate via the `certificate` parameter of `setWebhook`
- **Reverse proxy** — run Spring Boot on HTTP internally, terminate TLS at nginx/Caddy/AWS ALB
- **Local development** — use [ngrok](https://ngrok.com/) or [localtunnel](https://localtunnel.me/) to expose your local server (see example below)

## Local Development with ngrok

```bash
# Start your bot locally on port 8080
./mvnw spring-boot:run

# In another terminal, expose it via ngrok
ngrok http 8080
```

Set the forwarding URL as your webhook URL:

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: WEBHOOK
    webhook:
      url: https://abc123.ngrok.io/webhook
      path: /webhook
```

> ngrok generates a new URL on each restart — update `webhook.url` accordingly or use a paid ngrok plan with a fixed subdomain.

## Minimal Configuration

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: WEBHOOK
    webhook:
      url: https://bot.example.com/webhook
```

## All Properties

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.token` | ✅ | — | Bot token (shared across transports) |
| `telegram.bot.webhook.url` | ✅ | — | Public HTTPS URL Telegram calls |
| `telegram.bot.webhook.path` | ❌ | `/webhook` | Local request path Spring MVC listens on |
| `telegram.bot.webhook.secret-token` | ❌ | — | Header validation secret (strongly recommended) |
| `telegram.bot.webhook.max-connections` | ❌ | — | 1–100 simultaneous connections (Telegram default is 40) |
| `telegram.bot.webhook.drop-pending-updates` | ❌ | `false` | Drop queued updates on registration |
| `telegram.bot.webhook.unregister-on-shutdown` | ❌ | `false` | Delete webhook on shutdown |

### Full Example

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
    transport: webhook
    webhook:
      url: https://bot.example.com/webhook
      path: /webhook
      secret-token: ${WEBHOOK_SECRET}           # optional but recommended
      max-connections: 40
      drop-pending-updates: false
      unregister-on-shutdown: false
```

## Customizing Infrastructure

Every infrastructure concern is exposed as a separate `@ConditionalOnMissingBean` provider bean.
Declare only the ones you need to override — everything else keeps its default.

### Available providers

| Provider interface | Default | Override example use-case |
|---|---|---|
| `BotTelegramClientProvider` | `OkHttpTelegramClient` | Custom client implementation |
| `BotExecutorServiceProvider` | `newSingleThreadExecutor()` | Fixed thread pool |
| `BotOkHttpClientProvider` | `new OkHttpClient()` | Custom timeouts / proxy |
| `BotTelegramUrlProvider` | `TelegramUrl.DEFAULT_URL` | Test / local Telegram mock |
| `BotObjectMapperProvider` | shared Spring `ObjectMapper` | Custom serialisation modules |

### Example: custom executor

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

### Example: point to a local Telegram mock

```java
@Bean
public BotTelegramUrlProvider botTelegramUrlProvider() {
    return () -> new TelegramUrl("http", "localhost", 8080);
}
```

### Example: startup hook

```java
@Component
public class MyStartTrigger implements BotStartTrigger {

    @Override
    public void onStart(User botUser, TelegramClient client) {
        log.info("Bot @{} started", botUser.getUserName());
    }
}
```

Declaring any of these `@Bean` methods replaces only that provider; all other defaults remain active.

## Webhook vs. Long-Polling

| | Webhook | Long-Polling |
|---|---|---|
| Infrastructure | Public HTTPS URL required | None (just a token) |
| Latency | Near real-time | ~50 ms + network |
| Updates missed on restart | Lost unless Telegram retries | Re-delivered by Telegram (< 24 h) |
| Best for | Production, serverless, high-throughput | Development, simple deployments |

## See Also

- [core/README.md](../core/README.md) — handler annotations and processing pipeline
- [longpolling/README.md](../longpolling/README.md) — alternative long-polling transport

