---
id: chat-state-backends
title: Chat State Backends
---

# Chat State Backends

By default, Easygram stores chat state in memory using `InMemoryBotChatStateService`. This works
for development and single-instance deployments, but production and clustered environments
require a persistent backend.

## The BotChatStateService Interface

```java
public interface BotChatStateService {

    /** Returns the current state string for the given chatId, or null if none. */
    String getState(Long chatId);

    /** Sets the state for the given chatId. */
    void setState(Long chatId, String state);

    /** Removes the state for the given chatId. */
    void clearState(Long chatId);

    // Enum-aware overloads (default implementations provided)
    default <T extends Enum<T>> void setState(Long chatId, T state) { ... }
    default <T extends Enum<T>> T getStateAs(Long chatId, Class<T> type) { ... }
}
```

Override this bean by declaring your own `@Bean` or `@Component` of type `BotChatStateService`.
The default `InMemoryBotChatStateService` is annotated `@ConditionalOnMissingBean`, so it is
only created when no other implementation is present.

## Default: InMemoryBotChatStateService

```
Backed by:  ConcurrentHashMap<Long, String>
Thread-safe: yes
Persistent:  NO — state is lost on restart
Suitable for: development, testing, single-instance deployments
```

## Override with Redis

### 1. Add the dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. Configure Redis

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2s
```

### 3. Implement the service

```java
@Component
public class RedisChatStateService implements BotChatStateService {

    private static final String KEY_PREFIX = "bot:state:";
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redis;

    public RedisChatStateService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String getState(Long chatId) {
        return redis.opsForValue().get(KEY_PREFIX + chatId);
    }

    @Override
    public void setState(Long chatId, String state) {
        redis.opsForValue().set(KEY_PREFIX + chatId, state, TTL);
    }

    @Override
    public void clearState(Long chatId) {
        redis.delete(KEY_PREFIX + chatId);
    }
}
```

Declaring this `@Component` automatically replaces `InMemoryBotChatStateService` — no extra
configuration needed.

## Override with JDBC

### 1. Add the dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

### 2. Create the table

```sql
CREATE TABLE bot_chat_state (
    chat_id    BIGINT      NOT NULL PRIMARY KEY,
    state      VARCHAR(255),
    updated_at TIMESTAMP   NOT NULL DEFAULT now()
);
```

### 3. Implement the service

```java
@Component
public class JdbcChatStateService implements BotChatStateService {

    private final JdbcTemplate jdbc;

    public JdbcChatStateService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String getState(Long chatId) {
        try {
            return jdbc.queryForObject(
                    "SELECT state FROM bot_chat_state WHERE chat_id = ?",
                    String.class, chatId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Override
    public void setState(Long chatId, String state) {
        jdbc.update(
            "INSERT INTO bot_chat_state (chat_id, state, updated_at) VALUES (?, ?, now()) " +
            "ON CONFLICT (chat_id) DO UPDATE SET state = EXCLUDED.state, updated_at = now()",
            chatId, state);
    }

    @Override
    public void clearState(Long chatId) {
        jdbc.update("DELETE FROM bot_chat_state WHERE chat_id = ?", chatId);
    }
}
```

### 4. Configure the datasource

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

## Using Enum States (Recommended)

Enums provide compile-time safety and IDE autocompletion. The service has built-in overloads:

```java
// Define states as an enum
public enum RegistrationState { AWAITING_NAME, AWAITING_AGE, AWAITING_CITY }

// Set with an enum constant
chatStateService.setState(userId, RegistrationState.AWAITING_NAME);

// Read back as enum
RegistrationState state = chatStateService.getStateAs(userId, RegistrationState.class);
```

In handler annotations use the enum constant's `name()`:

```java
@BotTextDefault
@BotChatState("AWAITING_NAME")
@BotForwardChatState("AWAITING_AGE")
public String collectName(@BotTextValue String name) { ... }
```

## Injecting BotChatStateService in Handlers

Inject the service to check or set state programmatically when annotations are not flexible enough:

```java
@BotController
@RequiredArgsConstructor
public class ProfileController {

    private final BotChatStateService chatStateService;

    @BotCommand("/status")
    public String onStatus(User user) {
        String state = chatStateService.getState(user.getId());
        return state == null
               ? "No active session. Use /start to begin."
               : "Current step: " + state;
    }

    @BotCommand("/reset")
    @BotClearChatState
    public String onReset() {
        return "Session cleared. Use /start to begin again.";
    }
}
```

## Backend Comparison

| Backend | Persistent | Multi-instance | Performance | Use case |
|---|---|---|---|---|
| In-memory | No | No | Fastest | Dev, single instance |
| Redis | Yes | Yes | Very fast | Production, multi-instance |
| JDBC / JPA | Yes | Yes | Fast | When DB is already in use |
| Custom | Varies | Varies | Varies | Specific storage |

## Testing with a Custom Backend

Override `BotChatStateService` in tests with a simple in-memory lambda:

```java
@TestConfiguration
public class TestChatStateConfig {

    @Bean
    @Primary
    public BotChatStateService testStateService() {
        Map<Long, String> store = new ConcurrentHashMap<>();
        return new BotChatStateService() {
            public String getState(Long id)         { return store.get(id); }
            public void setState(Long id, String s) { store.put(id, s); }
            public void clearState(Long id)         { store.remove(id); }
        };
    }
}
```

---

See also:
- [Chat State (Core Concept)](../core-concepts/chat-state) — annotations and flow
- [Registration Wizard Example](../examples/registration-wizard) — full multi-step example
