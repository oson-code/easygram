# longpolling

> Long-polling transport for the Easygram framework.
> Connects to the Telegram `getUpdates` API and feeds updates into the bot processing pipeline.

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>longpolling</artifactId>
    <version>0.0.2</version>
</dependency>
```

## What It Does

The `longpolling` module registers an autoconfigured background worker that repeatedly calls Telegram's `getUpdates` API using an `OkHttpClient`. Each received `Update` is dispatched into the bot's filter and handler pipeline. It is the default transport and requires no additional infrastructure beyond a bot token.

## How It Works

The polling loop is managed by a `ScheduledExecutorService` (one thread by default). Each polling cycle:

1. Issues a `getUpdates` request with the current `offset`, `limit` (default: 100), and `timeout` (default: 50 seconds — the long-poll window)
2. If updates arrive before the timeout, they are returned immediately
3. If no updates arrive within the timeout window, Telegram responds with an empty list after ~50 seconds — the scheduler then issues the next request immediately
4. After processing, the offset is advanced to `lastUpdateId + 1` to acknowledge receipt and avoid re-delivering the same updates

> **Restart behavior:** If the process restarts, the offset resets to 0 and Telegram re-delivers all unacknowledged updates from the last 24 hours. This is safe — updates will be delivered at most once per `GetUpdates` call because Telegram only re-delivers updates that were never confirmed.

## Back-Off on Errors

When a polling request fails (network error, Telegram API error, etc.), the scheduler applies an `ExponentialBackOff` strategy before retrying:

- **Initial interval:** configurable (default follows Spring's `ExponentialBackOff` defaults)
- **Multiplier:** each successive failure doubles the wait time
- **Max attempts:** unlimited by default

Override back-off behavior by registering a custom `BotBackOffProvider` bean:

```java
@Bean
public BotBackOffProvider botBackOffProvider() {
    ExponentialBackOff backOff = new ExponentialBackOff();
    backOff.setInitialInterval(1_000);    // 1 second
    backOff.setMultiplier(2.0);
    backOff.setMaxInterval(30_000);       // cap at 30 seconds
    return () -> backOff;
}
```

## Minimal Configuration

```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}              # required
    transport: LONG_POLLING          # optional — this is the default
```

## All Properties

| Property | Required | Default | Description |
|---|---|---|---|
| `telegram.bot.token` | ✅ | — | Bot token from @BotFather (shared across transports) |

## Customizing Infrastructure

Every infrastructure concern is exposed as a separate `@ConditionalOnMissingBean` provider bean.
Declare only the ones you need to override — everything else keeps its default.

### Available providers

| Provider interface | Default | Override example use-case |
|---|---|---|
| `BotOkHttpClientProvider` | `new OkHttpClient()` | Custom timeouts / proxy |
| `BotExecutorServiceProvider` | `newSingleThreadExecutor()` | Fixed thread pool |
| `BotTelegramUrlProvider` | `TelegramUrl.DEFAULT_URL` | Test / local Telegram mock |
| `BotObjectMapperProvider` | shared Spring `ObjectMapper` | Custom serialisation modules |
| `BotTelegramClientProvider` | `OkHttpTelegramClient` | Custom client implementation |
| `BotScheduledExecutorServiceProvider` | `newSingleThreadScheduledExecutor()` | Custom polling scheduler |
| `BotBackOffProvider` | `new ExponentialBackOff()` | Custom retry strategy |
| `BotGetUpdatesGeneratorProvider` | limit=100, timeout=50 | Custom `GetUpdates` parameters |

### Example: tune HTTP timeouts only

```java
@Configuration
public class BotConfig {

    @Bean
    public BotOkHttpClientProvider botOkHttpClientProvider() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        return () -> client;
    }
}
```

### Example: fixed thread pool + custom GetUpdates

```java
@Configuration
public class BotConfig {

    @Bean
    public BotExecutorServiceProvider botExecutorServiceProvider() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        return () -> pool;
    }

    @Bean
    public BotGetUpdatesGeneratorProvider botGetUpdatesGeneratorProvider() {
        return () -> offset ->
                GetUpdates.builder()
                        .offset(offset + 1)
                        .limit(10)
                        .timeout(100)
                        .build();
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

Declaring any of these `@Bean` methods replaces only that provider; all other defaults remain active.

## Long-Polling vs. Webhook

| | Long-Polling | Webhook |
|---|---|---|
| Infrastructure | None (just a token) | Public HTTPS URL required |
| Latency | ~50 ms + network | Near real-time |
| Updates missed on restart | Re-delivered by Telegram (< 24 h) | Lost unless Telegram retries |
| Best for | Development, single-server deployments | Production, serverless, high-throughput |

## See Also

- [core/README.md](../core/README.md) — handler annotations and processing pipeline
- [webhook/README.md](../webhook/README.md) — alternative webhook transport

