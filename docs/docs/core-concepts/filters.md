---
id: filters
title: Filter Pipeline
---

# Filter Pipeline

The filter pipeline is the middleware layer of Easygram. Every update passes through an ordered chain of filters before reaching your handlers.

## BotFilter Interface

```java
public interface BotFilter extends Comparable<BotFilter> {

    /** Core method — process the update and optionally delegate to chain. */
    void doFilter(BotRequest request, BotResponse response, BotFilterChain chain);

    /** Priority order — lower value runs first. Default: Integer.MAX_VALUE. */
    default int getOrder() { return Integer.MAX_VALUE; }

    /**
     * Optional opt-out hook. Return false to skip this filter for a given request
     * without stopping the chain. Default: always runs.
     *
     * Example: skip an expensive auth check for updates that have no user (inline queries).
     */
    default boolean shouldFilter(BotRequest request, BotResponse response) { return true; }
}
```

Register any `BotFilter` as a Spring `@Bean`. The framework collects all filters, sorts by
`getOrder()`, and executes them in ascending order for every incoming update.

**shouldFilter() example — skip filter for bot-own messages:**

```java
@Component
public class AuthFilter implements BotFilter {

    @Override
    public boolean shouldFilter(BotRequest request, BotResponse response) {
        // Skip auth check if there is no user (e.g. channel posts)
        return request.getUser() != null;
    }

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) {
        if (!isAllowed(request.getUser().getId())) {
            response.addBotApiMethod(SendMessage.builder()
                .chatId(request.getChat().getId())
                .text(" Not authorized.")
                .build());
            return; // stop chain — do NOT call chain.doFilter
        }
        chain.doFilter(request, response);
    }
}
```

## Built-In Filters

Easygram includes these filters, executed in this order:

| Filter | Order constant | Value | Purpose |
|---|---|---|---|
| `BotMdcFilter` | `MDC_CONTEXT` | `Integer.MIN_VALUE` | Sets MDC keys `bot.update.id` and `bot.transport` for correlated logging |
| `BotContextSetterFilter` | `CONTEXT_SETTER` | `MIN_VALUE + 1` | Resolves `User` and `Chat` from the update; enriches MDC with `bot.chat.id` / `bot.user.id` |
| `BotObservabilityFilter` | `OBSERVATION` | `MIN_VALUE + 2` | Wraps the chain in a Micrometer `Observation` (timer + distributed trace span) |
| `BotApiMethodsSenderFilter` | `API_SENDER` | `MIN_VALUE + 3` | Calls the downstream chain, then executes all queued `BotApiMethod` calls via `TelegramClient` |
| `BotUpdatePublishingFilter` | `PUBLISHING` | `MIN_VALUE + 1000` | Forwards the raw update to Kafka / RabbitMQ (active only when `messaging-api` is configured) |
| *(your custom filters)* | — | `MAX_VALUE` (default) | Runs after all built-in filters |

:::tip
Custom filters default to `Integer.MAX_VALUE` — they run after all built-in filters.
Use `BotFilterOrder.CONTEXT_SETTER + N` to insert your filter just after `User`/`Chat` are resolved.
Use `BotFilterOrder.OBSERVATION - 1` to wrap your logic inside the Micrometer trace span.
:::

## Creating Custom Filters

```java
@Component
public class LoggingFilter implements BotFilter {
    
    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) throws Exception {
        long start = System.currentTimeMillis();
        
        // Pre-processing
        log.info("Update from user {}: {}", request.getUser().getId(), request.getUpdate().getUpdateId());
        
        // Continue chain
        chain.doFilter(request, response);
        
        // Post-processing
        long duration = System.currentTimeMillis() - start;
        log.info("Processed in {} ms", duration);
    }
    
    @Override
    public int getOrder() {
        return -500; // High priority (before dispatcher)
    }
}
```

## Common Use Cases

### 1. Authentication Filter

```java
@Component
public class AuthenticationFilter implements BotFilter {
    
    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) throws Exception {
        User user = request.getUser();
        
        if (!isUserAllowed(user.getId())) {
            response.addBotApiMethod(SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("You are not authorized!")
                .build());
            return; // Stop chain
        }
        
        chain.doFilter(request, response); // Allow to continue
    }
    
    @Override
    public int getOrder() {
        return -2000; // Very high priority
    }
}
```

### 2. Rate-Limiting Filter

```java
@Component
public class RateLimitingFilter implements BotFilter {
    
    private final Map<Long, LocalDateTime> lastMessageTime = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MILLIS = 1000; // 1 message per second
    
    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) throws Exception {
        Long userId = request.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastMessageTime.getOrDefault(userId, now.minusSeconds(10));
        
        if (Duration.between(lastTime, now).toMillis() < RATE_LIMIT_MILLIS) {
            response.addBotApiMethod(SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("Please wait before sending another message!")
                .build());
            return;
        }
        
        lastMessageTime.put(userId, now);
        chain.doFilter(request, response);
    }
    
    @Override
    public int getOrder() {
        return -1500;
    }
}
```

### 3. Audit Logging Filter

```java
@Component
public class AuditFilter implements BotFilter {
    
    @Autowired
    private AuditRepository auditRepository;
    
    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) throws Exception {
        User user = request.getUser();
        Update update = request.getUpdate();
        
        // Log incoming message
        auditRepository.log(AuditEvent.builder()
            .userId(user.getId())
            .action("MESSAGE_RECEIVED")
            .details(update.getMessage().getText())
            .timestamp(LocalDateTime.now())
            .build());
        
        chain.doFilter(request, response);
        
        // Log response sent
        if (!response.getQueue().isEmpty()) {
            auditRepository.log(AuditEvent.builder()
                .userId(user.getId())
                .action("RESPONSE_SENT")
                .count(response.getQueue().size())
                .timestamp(LocalDateTime.now())
                .build());
        }
    }
    
    @Override
    public int getOrder() {
        return -1200;
    }
}
```

## Filter Chain Usage Example

Given three filters with orders: 100, 10, 50

Execution order:
1. Filter with order 10 (highest priority)
2. Filter with order 50
3. Filter with order 100 (lowest priority)

```
→ Filter(10).doFilter()
  → Filter(50).doFilter()
    → Filter(100).doFilter()
      → [Handler executed]
    ← Filter(100) [post-processing]
  ← Filter(50) [post-processing]
← Filter(10) [post-processing]
```

## Stopping the Chain

Return without calling `chain.doFilter()` to prevent subsequent filters and handler execution:

```java
@Override
public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) throws Exception {
    if (shouldBlock()) {
        response.addBotApiMethod(SendMessage.builder()...build());
        return; // Chain stops here
    }
    
    chain.doFilter(request, response); // Continue
}
```

## Accessing Request/Response Data

**BotRequest fields:**
- `User getUser()` — resolved sender (set by `BotContextSetterFilter`)
- `Chat getChat()` — resolved chat
- `Update getUpdate()` — raw Telegram update
- `TelegramClient getTelegramClient()` — API client
- `BotMetadata getBotMetadata()` — bot's own id, username, token
- `Throwable getThrowable()` — populated when routing to an exception handler

**BotResponse methods:**
- `void addBotApiMethod(BotApiMethod<?> method)` — enqueue a single API call
- `void addBotApiMethods(Collection<BotApiMethod<?>> methods)` — enqueue multiple calls
- `Collection<BotApiMethod<?>> getBotApiMethods()` — inspect the current queue

## Exception Handling in Filters

If a `doFilter()` implementation throws an unchecked exception, it propagates up the filter chain
and is ultimately caught by the framework. No response is sent to the user for unhandled
filter exceptions. **Always handle exceptions internally** or re-throw only if you want the
update to be silently dropped:

```java
@Override
public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) {
    try {
        chain.doFilter(request, response);
    } catch (Exception ex) {
        log.error("Filter error for update {}", request.getUpdate().getUpdateId(), ex);
        response.addBotApiMethod(SendMessage.builder()
            .chatId(request.getChat().getId())
            .text("Something went wrong. Please try again.")
            .build());
    }
}
```

## Thread Safety

Filter beans are Spring singletons and are **shared across concurrent update processing threads**.
Design your filters to be **stateless** (no instance fields that store per-request data).
If you must maintain state (e.g., rate-limit counters), use thread-safe data structures:

```java
// Thread-safe: ConcurrentHashMap is safe for concurrent access
private final Map<Long, Deque<Long>> timestamps = new ConcurrentHashMap<>();

// Not thread-safe: plain HashMap with unsynchronized per-user deque
private final Map<Long, Deque<Long>> timestamps = new HashMap<>();
```

---

Next: Learn about [parameter injection](parameter-injection) to access request data in your handlers.
