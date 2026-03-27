---
id: architecture
title: System Architecture
---

# Easygram Architecture

## System Overview

Easygram is built on a **modular, multi-module Maven architecture** where each module has a specific responsibility. This design ensures flexibility, testability, and easy customization.

```

  Your Bot (@BotController with handler methods)

                   Update (from any transport)

  Telegram Update → Filter Chain (ordered by priority)
   BotContextSetterFilter (resolve User + Chat)
   BotUpdatePublishingFilter (forward to broker, optional)
   BotApiMethodsSenderFilter (execute response API calls)

  BotDispatcher (route to matching handler)
   Tier 1: State Handlers (@BotChatState with values)
   Tier 2: Spec Handlers (no state restriction)
   Tier 3: Default Handlers (@BotDefaultHandler)

  Handler Method Invocation
   Argument Resolution (inject User, Chat, params, etc)
   Method Execution
   Return-Type Handling (convert to BotApiMethod calls)

  Response to Telegram API

```

## Module Structure

### core-api
**Pure contracts—no Spring configuration, no business logic.**

Contains:
- All custom annotations (`@BotCommand`, `@BotText`, etc.)
- Handler interfaces (`BotFilter`, `BotArgumentResolver`, `BotReturnTypeHandler`)
- Model classes (`BotRequest`, `BotResponse`, `Update`, `User`, `Chat`)
- Configuration interfaces (`BotConfigurer`)

Every module depends on `core-api`.

### core
**The processing engine.**

Contains:
- `Bot` abstract base class
- `BotDispatcher` (routes updates to handlers)
- Filter chain implementation
- Built-in `BotArgumentResolver` implementations
- Built-in `BotReturnTypeHandler` implementations
- Handler scanning and registration (`BotHandlerLoader`, `BotHandlerRegistry`)
- Spring auto-configuration (`CoreAutoConfiguration`)

Depends on: `core-api`, `core-chatstate`

### core-chatstate
**Optional chat state service (in-memory default).**

Contains:
- `InMemoryBotChatStateService` (thread-safe, for development/single-instance)

Replaceable with custom `BotChatStateService` implementations (Redis, JDBC, etc.) by providing your own `@Bean`.

Depends on: `core-api`

### core-i18n
**Internationalization support.**

Contains:
- `BotMessageSource` (locale-aware message lookup)
- `BotKeyboardFactory` (localized keyboard creation)
- `LocalizedReply` return type
- Locale argument resolver

Depends on: `core-api`

### core-observability
**Observability hooks.**

Contains:
- Metrics collection interfaces
- Tracing integration points

Depends on: `core-api`

### Transports
- **longpolling** — Polling updates from Telegram API (default)
- **webhook** — Receiving updates via HTTPS webhooks (Spring MVC)
- **messaging-kafka-consumer** — Consuming updates from Kafka
- **messaging-rabbit-consumer** — Consuming updates from RabbitMQ

### Brokers
- **messaging-api** — `BotUpdatePublisher` SPI
- **messaging-kafka** — Kafka publisher using `KafkaTemplate`
- **messaging-rabbit** — RabbitMQ publisher using `RabbitTemplate`
- **messaging-producer** — Smart routing (publish to Kafka OR RabbitMQ based on property)

### spring-boot-starter
**One-stop dependency.**

Pulls in:
- `core`, `core-chatstate`, `core-i18n`, `core-observability`
- All transports (`longpolling`, `webhook`, `messaging-kafka-consumer`, `messaging-rabbit-consumer`)
- All brokers

### samples
**Runnable Spring Boot applications** demonstrating each transport and pattern.

## Update Processing Pipeline

### 1. Transport-Specific Bot (e.g., LongPollingBot)
Receives updates from Telegram and calls `Bot.handleUpdate(Update)`.

### 2. Filter Chain
Each update passes through an ordered chain of `BotFilter` implementations:

```java
public interface BotFilter {
    void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) throws Exception;
    int getOrder(); // Lower value = higher priority
}
```

Built-in filters (in order):
1. **BotContextSetterFilter** — Resolve `User` and `Chat` from update
2. **BotUpdatePublishingFilter** — Publish update to broker (if enabled)
3. **BotDispatcher** — Route to handler
4. **BotApiMethodsSenderFilter** — Execute queued API calls

Custom filters can be inserted at any priority level.

### 3. Handler Dispatch (Three Tiers)

The dispatcher searches in order, stopping at first match:

**Tier 1: State Handlers**
- Methods with `@BotChatState` where at least one state value matches user's current state
- Always executed first (highest priority)
- Ensures stateful handlers take precedence

**Tier 2: Bot Handlers**
- Spec-type handlers (`@BotCommand`, `@BotText`, `@BotCallbackQuery`, etc.)
- No state restriction
- Executed only if no Tier 1 match

**Tier 3: Default Handlers**
- `@BotDefaultHandler`, `@BotTextDefault`, `@BotDefaultCallbackQuery`, etc.
- No state restriction
- Executed only if no Tier 1 or Tier 2 match
- Fallback for unmatched updates

### 4. Handler Method Invocation

#### Argument Resolution
The framework resolves method parameters automatically:

```java
@BotCommand("/start")
public String handleStart(
    User user, // Resolved from Update
    @BotCommandValue String command, // The matched "/start"
    @BotCommandQueryParam Integer code // Query param (e.g., /start 42)
) { ... }
```

All resolvable parameters:
- `Update` — Raw Telegram update
- `User` — Sender
- `Chat` — Chat context
- `BotRequest` — Internal request (filter context); also carries `setAttribute`/`getAttribute` for cross-cutting data
- `BotResponse` — Internal response accumulator; also carries `setAttribute`/`getAttribute`
- `TelegramClient` — API client
- `BotMetadata` — Bot's own info (id, username)
- `Throwable` — Exception being handled (in `@BotExceptionHandler` only)
- `Contact` — Shared contact (in `@BotContact` handlers only)
- `Location` — Shared location (in `@BotLocation` handlers only)
- `BotMarkupContext` — Markup factory params (in `@BotMarkup` factory methods only)
- `Locale` — User's locale (requires `core-i18n`)
- Annotated scalars (`@BotCommandValue`, `@BotTextValue`, `@BotCallbackQueryData`, `@BotCommandQueryParam`)
- Custom types via `BotArgumentResolver`

#### Method Execution
Handler method is invoked with resolved arguments. Any exceptions are caught and routed to `@BotExceptionHandler` methods if defined.

#### Return-Type Handling
The return value is converted to `BotApiMethod` calls:

```java
// Return type → Handler behavior
String → SendMessage to current chat
PlainReply → SendMessage + optional keyboard
PlainTextTemplate → SendMessage with #{index} token substitution
BotApiMethod<?> → Enqueued and executed directly
Collection<BotApiMethod<?>> → All executed in insertion order
Collection<Object> → Per-element dispatch to matching handler
void / null → No response sent
LocalizedReply → MessageSource key lookup + SendMessage (core-i18n)
LocalizedTemplate → Mixed ${key}/#{index} template + SendMessage (core-i18n)
```

### 5. Response Execution
Queued `BotApiMethod` calls are executed by `BotApiMethodsSenderFilter`.

## Handler Dispatch Tiers (Detailed Example)

Suppose user is in `"REGISTRATION"` state and sends text "John":

```java
@BotController
public class RegistrationBot {
    // Tier 1: State handler — MATCHES (state is "REGISTRATION")
    @BotText("John")
    @BotChatState("REGISTRATION")
    public String handleRegistrationText() {
        return "Name saved!";
    }
    
    // Would not be reached (Tier 1 matched first)
    @BotText("John")
    public String handleGeneralText() {
        return "Generic response";
    }
    
    // Would not be reached (Tier 1 matched first)
    @BotTextDefault
    public String handleDefault() {
        return "Default";
    }
}
```

Result: "Name saved!" is returned (Tier 1 handler executed).

## Exception / Error Flow

When a handler method throws an exception, the framework follows this path:

```
Handler method throws Exception
  
  
BotDispatcher catches the exception
  
   @BotExceptionHandler found in same @BotController?
        YES → invoke exception handler method → return response
  
   @BotExceptionHandler found in any @BotControllerAdvice?
        YES → invoke advice exception handler → return response
  
   NO handler found → exception propagates up to the BotFilter chain
        Unhandled exception is logged; update processing ends silently
            (no message sent to user)
```

:::tip Best Practice
Always define a catch-all `@BotExceptionHandler(Exception.class)` in a `@BotControllerAdvice`
so users receive a friendly error message instead of silence.
:::

```java
@BotControllerAdvice
public class GlobalErrorHandler {

    @BotExceptionHandler(Exception.class)
    public String onAnyError(Exception ex, User user) {
        log.error("Unhandled error for user {}", user.getId(), ex);
        return " Something went wrong. Please try again later.";
    }
}
```

## Extension Points

### 1. Custom BotFilter
Intercept all updates for middleware logic (auth, logging, rate-limiting).

### 2. Custom BotArgumentResolver
Inject custom types into handler methods.

### 3. Custom BotReturnTypeHandler
Support new return types beyond built-in ones.

### 4. Custom Transport
Subclass `Bot` for a new update source.

### 5. Custom BotChatStateService
Replace in-memory implementation with Redis, JDBC, etc.

## Configuration

### Required Property
```yaml
telegram:
  bot:
    token: YOUR_BOT_TOKEN
```

### Optional Properties
```yaml
telegram:
  bot:
    transport: LONG_POLLING # LONG_POLLING, WEBHOOK, KAFKA_CONSUMER, RABBIT_CONSUMER
    i18n:
      default-locale: en

# Transport-specific (see module READMEs)
# Long-polling has no additional configurable properties — it uses the defaults from
# the underlying telegrambots library. Use spring.kafka.* / spring.rabbitmq.* for broker transports.
```

## Request/Response Model

### BotRequest
```java
Update update // Raw Telegram update
TelegramClient telegramClient // Telegram API client
User user // Resolved sender (set by BotContextSetterFilter)
Chat chat // Resolved chat (set by BotContextSetterFilter)
Throwable throwable // Populated when routing to @BotExceptionHandler
BotMetadata botMetadata // Bot's own info (token, id, username)

// Request-scoped attribute store — share data between filters, resolvers, and handlers:
void setAttribute(String key, Object value) // null removes the key
<T> T getAttribute(String key)
Map<String,Object> getAttributes() // unmodifiable view
```

### BotResponse
```java
// Accumulates API calls to execute; methods:
void addBotApiMethod(BotApiMethod<?> method)
void addBotApiMethods(Collection<BotApiMethod<?>> methods)
Collection<BotApiMethod<?>> getBotApiMethods()

// Response-scoped attribute store (shared with filters and return-type handlers):
void setAttribute(String key, Object value) // null removes the key
<T> T getAttribute(String key)
Map<String,Object> getAttributes() // unmodifiable
```

## Concurrency Model

- **Thread-safe** — All framework components are thread-safe
- **Filter chain** — Executed serially per update
- **Bot handler registry** — Shared and immutable after startup
- **Chat state service** — Pluggable (in-memory default is thread-safe)
- **Argument resolvers** — Must be thread-safe
- **ExecutorService** — Optional batch processing for updates

## Dependency Injection

All framework components use Spring dependency injection:
- Auto-wired via `@Autowired` or constructor injection
- `@ConditionalOnMissingBean` allows easy bean overrides
- Custom filters, resolvers, and handlers are auto-scanned and registered

