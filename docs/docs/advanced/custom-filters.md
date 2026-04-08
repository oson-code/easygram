---
id: custom-filters
title: Custom Filters
description: Intercept every Telegram update with a custom BotFilter — implement doFilter(), set execution priority with getOrder(), and use BotFilterOrder constants for precise pipeline ordering.
keywords: [BotFilter custom, telegram bot interceptor java, spring boot bot filter chain, BotFilterOrder, easygram custom filter, update interceptor]
---

# Custom Filters

Filters are middleware that intercepts **every update** before it reaches a handler. Use them
for cross-cutting concerns: authentication, rate-limiting, audit logging, tenant resolution, or
any logic that must run regardless of which handler matches.

## How the Filter Chain Works

Every incoming update passes through an ordered chain of `BotFilter` implementations.
Each filter can:

- Short-circuit the chain (e.g., reject unauthorized requests)
- Transform `BotRequest` or `BotResponse`
- Add data to the context map for downstream filters and handlers
- Call `chain.doFilter(request, response)` to continue to the next filter

```
Update
  → BotContextSetterFilter (sets User + Chat)
  → BotObservationFilter (Micrometer span)
  → BotApiMethodsSenderFilter(sends queued responses)
  → BotUpdatePublishingFilter (optional: broker forwarding)
  → [your custom filters]
  → BotDispatcher (routes to handler)
```

The chain is sorted by `getOrder()` — lower value = runs earlier.

## BotFilter Interface

```java
public interface BotFilter extends Ordered {

    void doFilter(BotRequest request, BotResponse response, BotFilterChain chain)
            throws Exception;

    @Override
    default int getOrder() {
        return Integer.MAX_VALUE; // Run last by default
    }
}
```

Register any `BotFilter` as a Spring `@Bean` — the framework auto-collects and sorts them.

## Built-in Filter Order Constants

Use `BotFilterOrder` constants to position your filter relative to built-in ones:

| Constant | Value | Built-in filter |
|---|---|---|
| `CONTEXT_SETTER` | `Integer.MIN_VALUE` | Resolves `User` and `Chat` from the update |
| `OBSERVATION` | `MIN_VALUE + 1` | Wraps the chain in a Micrometer `Observation` |
| `API_SENDER` | `MIN_VALUE + 2` | Executes queued `BotApiMethod` calls after chain |
| `PUBLISHING` | `MIN_VALUE + 1000` | Forwards the update to Kafka or RabbitMQ |
| *(your filter)* | `MAX_VALUE` | Default — runs after all built-in filters |

:::tip
Insert your filter after `CONTEXT_SETTER` so `request.getUser()` and `request.getChat()` are
already resolved. The default `Integer.MAX_VALUE` ordering is always safe.
:::

## Example: Authentication Filter

Reject updates from users not in a whitelist before any handler runs:

```java
@Component
public class AuthFilter implements BotFilter {

    private final Set<Long> allowedUserIds = Set.of(123456789L, 987654321L);

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain)
            throws Exception {
        User user = request.getUser();
        if (user == null || !allowedUserIds.contains(user.getId())) {
            response.addBotApiMethod(SendMessage.builder()
                    .chatId(request.getChat().getId())
                    .text(" You are not authorized to use this bot.")
                    .build());
            return; // Stop — do NOT call chain.doFilter
        }
        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return BotFilterOrder.CONTEXT_SETTER + 10;
    }
}
```

## Example: Rate-Limiting Filter

Allow each user a maximum of 5 messages per 10 seconds:

```java
@Component
public class RateLimitFilter implements BotFilter {

    private final Map<Long, Deque<Long>> timestamps = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 10_000L;

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain)
            throws Exception {
        User user = request.getUser();
        if (user != null && isRateLimited(user.getId())) {
            response.addBotApiMethod(SendMessage.builder()
                    .chatId(request.getChat().getId())
                    .text(" Too many requests — please slow down.")
                    .build());
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(long userId) {
        long now = System.currentTimeMillis();
        Deque<Long> window = timestamps.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MS) {
                window.pollFirst();
            }
            if (window.size() >= MAX_REQUESTS) return true;
            window.addLast(now);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return BotFilterOrder.CONTEXT_SETTER + 100;
    }
}
```

## Example: Audit Logging Filter

Log every update with the user, chat, and elapsed processing time:

```java
@Component
public class AuditFilter implements BotFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditFilter.class);

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain)
            throws Exception {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            User user = request.getUser();
            log.info("update={} user={} chat={} elapsed={}ms responses={}",
                    request.getUpdate().getUpdateId(),
                    user != null ? user.getUserName() : "unknown",
                    request.getChat() != null ? request.getChat().getId() : "unknown",
                    elapsed,
                    response.getQueue().size());
        }
    }

    @Override
    public int getOrder() {
        return BotFilterOrder.CONTEXT_SETTER + 50;
    }
}
```

## Sharing Data Between Filters and Handlers

`BotRequest` carries `update`, `telegramClient`, `user`, `chat`, `throwable`, and `botMetadata`
— there is no built-in context map. To share computed values from a filter into handlers, use a
**request-scoped Spring component** or a `ThreadLocal` in a shared service:

```java
// Shared service — holds per-thread data for the duration of one update
@Component
public class TenantContext {

    private final ThreadLocal<String> tenantId = new ThreadLocal<>();

    public void set(String id) { tenantId.set(id); }
    public String get() { return tenantId.get(); }
    public void clear() { tenantId.remove(); }
}

// Filter — resolves and stores tenant ID before the chain runs
@Component
public class TenantResolutionFilter implements BotFilter {

    private final TenantContext tenantContext;

    public TenantResolutionFilter(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) {
        tenantContext.set(resolveTenantId(request));
        try {
            chain.doFilter(request, response);
        } finally {
            tenantContext.clear(); // Always clean up after the chain
        }
    }

    @Override
    public int getOrder() { return BotFilterOrder.CONTEXT_SETTER + 50; }
}

// Handler — injects TenantContext as a Spring bean
@BotController
public class InfoController {

    @Autowired
    private TenantContext tenantContext;

    @BotCommand("/info")
    public String onInfo() {
        return "Tenant: " + tenantContext.get();
    }
}
```

## Enqueuing Responses from Filters

Any filter may add `BotApiMethod` calls to `response`. They are executed after the full chain
completes by `BotApiMethodsSenderFilter`:

```java
response.addBotApiMethod(SendMessage.builder()
        .chatId(request.getChat().getId())
        .text(" Your request is being processed...")
        .build());
```

## Handler-Level Pipeline: BotHandlerInvocationFilter

For control over **handler invocation** itself — argument resolution, return-type dispatch, or
chat-state updates — implement `BotHandlerInvocationFilter` rather than `BotFilter`. These
filters run after the dispatcher has chosen a handler. See [Architecture](../architecture).

## Configuration Reference

| Filter position | Order value | Typical use case |
|---|---|---|
| After context | `CONTEXT_SETTER + 10` | Auth, permission checks |
| After context | `CONTEXT_SETTER + 20` | Tenant / locale resolution |
| After context | `CONTEXT_SETTER + 50` | Audit logging |
| After context | `CONTEXT_SETTER + 100` | Rate limiting |
| After observation | `OBSERVATION + 1` | Custom Micrometer counters |

---

See also:
- [Filters (Core Concept)](../core-concepts/filters) — filter pipeline overview
- [Observability](./observability) — built-in Micrometer observation
- [Architecture](../architecture) — full update processing pipeline
