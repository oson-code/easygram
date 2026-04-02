# core-api

> Pure contracts module — all annotations, interfaces, and model classes for the
> Easygram framework. No Spring autoconfiguration, no business logic.
> Every other module depends on `core-api`.

---

## Table of Contents

- [Maven Dependency](#maven-dependency)
- [Controller Annotations](#controller-annotations)
- [Handler Annotations](#handler-annotations)
- [Parameter Injection Annotations](#parameter-injection-annotations)
- [Chat State](#chat-state)
- [Filter Pipeline](#filter-pipeline)
- [Request & Response Models](#request--response-models)
- [Exception Handling](#exception-handling)
- [Infrastructure Providers](#infrastructure-providers)
- [Extension Points](#extension-points)
- [Package Map](#package-map)

---

## Maven Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>core-api</artifactId>
    <version>0.0.4</version>
</dependency>
```

> **Note:** `core-api` is pulled in transitively by every other module. You only need
> this explicit dependency when writing a custom extension (e.g. a custom transport or
> argument resolver) without depending on the full engine.

---

## Controller Annotations

### `@BotController`

Marks a class as a Telegram bot controller. Doubles as a Spring `@Component`, so it is
picked up automatically by component scanning.

```java
@BotController
public class MyHandler {
    // handler methods
}
```

### `@BotControllerAdvice`

Marks a class as a **global** exception handler advice, applied across all `@BotController`
beans — analogous to Spring Web's `@ControllerAdvice`.

Optionally restricts which controllers it applies to via `basePackages`, `assignableTypes`,
or `annotations`:

```java
@BotControllerAdvice                          // global
public class GlobalExceptionAdvice { ... }

@BotControllerAdvice(basePackages = "com.example.payment")  // scoped
public class PaymentExceptionAdvice { ... }
```

### `@BotOrder`

Controls execution priority when multiple handlers match the same update. **Lower value = higher priority.** Default: `Integer.MAX_VALUE`.

```java
@BotCommand("/start")
@BotOrder(1)          // runs before any other /start handler
public void vipStart() { ... }

@BotCommand("/start")
@BotOrder(100)
public void defaultStart() { ... }
```

---

## Handler Annotations

All of these annotations are placed on **methods** inside a `@BotController`.

| Annotation | Matches | Example |
|---|---|---|
| `@BotCommand` | Bot command messages | `@BotCommand("/start")` |
| `@BotDefaultCommand` | Any command not matched by `@BotCommand` | `@BotDefaultCommand` |
| `@BotText` | Exact plain-text message | `@BotText("hello")` |
| `@BotTextDefault` | Any text not matched by `@BotText` or `@BotReplyButton` | `@BotTextDefault` |
| `@BotCallbackQuery` | Callback query by `data` value | `@BotCallbackQuery("btn_ok")` |
| `@BotDefaultCallbackQuery` | Any callback query not matched by `@BotCallbackQuery` | `@BotDefaultCallbackQuery` |
| `@BotContact` | Contact-sharing message | `@BotContact` |
| `@BotLocation` | Location-sharing message | `@BotLocation` |
| `@BotReplyButton` | Reply keyboard button by message key | `@BotReplyButton("menu.btn.settings")` |
| `@BotDefaultHandler` | Global fallback — any update not matched | `@BotDefaultHandler` |

### Handler dispatch flow

```mermaid
flowchart TD
    U[Incoming Update] --> FC[BotFilter chain]
    FC --> D[BotDispatcher]
    D --> CMD{Has /command?}
    CMD -->|yes| BC[@BotCommand / @BotDefaultCommand]
    CMD -->|no| TXT{Plain text?}
    TXT -->|yes| BT[@BotText / @BotTextDefault\n/ @BotReplyButton]
    TXT -->|no| CBQ{Callback query?}
    CBQ -->|yes| BQ[@BotCallbackQuery\n/ @BotDefaultCallbackQuery]
    CBQ -->|no| SPEC{Contact\nor Location?}
    SPEC -->|contact| BCO[@BotContact]
    SPEC -->|location| BLO[@BotLocation]
    SPEC -->|other| BDH[@BotDefaultHandler]
    BC & BT & BQ & BCO & BLO --> CS{@BotChatState\nguard passes?}
    CS -->|yes| INVOKE[Invoke handler method]
    CS -->|no| BDH
```

---

## Parameter Injection Annotations

Place these annotations on **method parameters** to have the framework inject the value automatically.

| Annotation | Injects | Parameter type |
|---|---|---|
| `@BotCommandValue` | The full command string (e.g. `"/start"`) | `String` |
| `@BotCommandQueryParam` | The argument after the command (e.g. `"42"` from `/start 42`) | Any type convertible by `ObjectMapper` |
| `@BotTextValue` | The full message text | `String` |
| `@BotCallbackQueryData` | The `data` field of the callback query | `String` |

In addition, these parameter types are injected **by type** without an annotation:

| Type | Description |
|---|---|
| `Update` | Raw Telegram `Update` |
| `User` | Resolved sender (`update.getMessage().getFrom()` etc.) |
| `Chat` | Resolved chat |
| `Contact` | Shared contact (`update.getMessage().getContact()`) |
| `Location` | Shared location (`update.getMessage().getLocation()`) |
| `CallbackQuery` | Full callback query object |
| `TelegramClient` | Telegram API client for direct calls |
| `BotRequest` | Full request context |
| `BotResponse` | Mutable response accumulator |
| `BotMetadata` | Current bot context (token, ID, username) |
| `BotChatStateService` | Chat state service |
| `Locale` | *(i18n module)* Resolved user locale |
| `Throwable` (subtype) | *(Exception handlers only)* Thrown exception |

```java
@BotCommand("/start")
public SendMessage onStart(
        User user,
        Chat chat,
        TelegramClient client,
        BotRequest request,
        @BotCommandQueryParam String param) {
    // all parameters resolved automatically
}
```

---

## Chat State

### `@BotChatState`

Restricts a handler method (or all methods in a class) to run only when the chat is in one of
the specified states. See [`core-chatstate/README.md`](../core-chatstate/README.md) for the
full guide.

```java
@BotTextDefault
@BotChatState("AWAITING_NAME")       // only runs when state == "AWAITING_NAME"
public String collectName(@BotTextValue String name) { ... }
```

### `BotChatStateService`

Interface for reading and writing per-chat state:

```java
public interface BotChatStateService {
    String getState(Long chatId);
    void setState(Long chatId, String state);          // null clears the state
    default void setState(Long chatId, Enum<?> state); // enum overload
    default <T extends Enum<T>> T getStateAs(Long chatId, Class<T> type);
}
```

---

## Filter Pipeline

### `BotFilter`

Intercepts every update **before** it reaches the dispatcher. Filters are sorted by
`getOrder()` in ascending order (lower = earlier). Short-circuit by not calling
`filterChain.doFilter(...)`.

```java
@Component
public class RateLimitFilter implements BotFilter {

    @Override
    public int getOrder() { return 10; }   // runs early

    @Override
    public boolean shouldFilter(BotRequest req, BotResponse res) {
        return req.getUpdate().hasMessage();
    }

    @Override
    public void doFilter(BotRequest req, BotResponse res, BotFilterChain chain) {
        if (isRateLimited(req.getUser().getId())) {
            res.addBotApiMethod(SendMessage.builder()
                    .chatId(req.getChat().getId())
                    .text("⏳ Too many requests. Please wait.")
                    .build());
            return;   // chain NOT called → handler skipped
        }
        chain.doFilter(req, res);
    }
}
```

### `BotFilterChain`

```java
@FunctionalInterface
public interface BotFilterChain {
    void doFilter(BotRequest request, BotResponse response);
}
```

### Built-in filters (registered by `core`)

| Filter | Order | Purpose |
|---|---|---|
| `BotContextSetterFilter` | `Integer.MIN_VALUE` | Resolves `User` and `Chat` from the update and stores them on `BotRequest` |
| `BotUpdatePublishingFilter` | `MIN_VALUE + 1000` | Forwards the update to the configured `BotUpdatePublisher` (broker) |
| `BotApiMethodsSenderFilter` | `Integer.MAX_VALUE` | Sends all queued `BotApiMethod` instances at the end of the chain |

---

## Request & Response Models

### `BotRequest`

Passed through the entire filter and handler chain. Contains:

| Field | Type | Description |
|---|---|---|
| `update` | `Update` | The raw Telegram update |
| `telegramClient` | `TelegramClient` | API client for direct calls |
| `user` | `User` | Resolved sender (populated by `BotContextSetterFilter`) |
| `chat` | `Chat` | Resolved chat (populated by `BotContextSetterFilter`) |
| `throwable` | `Throwable` | Exception raised during processing (set before exception handlers run) |

### `BotResponse`

A mutable accumulator for `BotApiMethod<?>` instances. The `BotApiMethodsSenderFilter`
executes all queued methods at the end of the chain.

```java
@BotTextDefault
public void onText(BotResponse response, BotRequest request) {
    response.addBotApiMethod(SendMessage.builder()
            .chatId(request.getChat().getId())
            .text("Message 1")
            .build());
    response.addBotApiMethod(SendMessage.builder()
            .chatId(request.getChat().getId())
            .text("Message 2")
            .build());
    // both messages sent at chain end
}
```

---

## Exception Handling

### `@BotExceptionHandler`

Declare inside `@BotController` (local) or `@BotControllerAdvice` (global) to handle a
specific exception type. Controller-local handlers take priority.

```java
@BotController
public class PaymentController {

    @BotCommand("/pay")
    public String pay() {
        throw new PaymentException("Insufficient funds");
    }

    @BotExceptionHandler(PaymentException.class)
    public String onPaymentError(Throwable ex, BotRequest request) {
        return "💳 Payment failed: " + ex.getMessage();
    }
}

@BotControllerAdvice
public class GlobalErrorHandler {

    @BotExceptionHandler(Throwable.class)
    public String onAnyError(Throwable ex, Update update) {
        return "⚠️ An unexpected error occurred.";
    }
}
```

### `BotHandlerException`

Unchecked exception thrown by the framework when no suitable handler is found or when
the handler chain encounters an internal error.

---

## Infrastructure Providers

The framework exposes all infrastructure concerns as fine-grained `@FunctionalInterface` provider
beans, each guarded by `@ConditionalOnMissingBean`. Override exactly the one you need; everything
else keeps its default.

### Common providers (package `uz.osoncode.easygram.core.provider`)

| Interface | Method | Description |
|---|---|---|
| `BotObjectMapperProvider` | `ObjectMapper provide()` | Jackson mapper for (de)serialisation |
| `BotTelegramUrlProvider` | `TelegramUrl provide()` | Telegram API base URL |
| `BotOkHttpClientProvider` | `OkHttpClient provide()` | HTTP transport client |
| `BotExecutorServiceProvider` | `ExecutorService provide()` | Update-handling thread pool |
| `BotTelegramClientProvider` | `TelegramClient provide(String token)` | Outbound API client; token-parameterised for future multi-bot support |

### Long-polling-specific providers (package `uz.osoncode.easygram.longpolling.provider`)

| Interface | Method | Description |
|---|---|---|
| `BotScheduledExecutorServiceProvider` | `ScheduledExecutorService provide()` | Polling scheduler |
| `BotBackOffProvider` | `BackOff provide()` | Retry back-off strategy |
| `BotGetUpdatesGeneratorProvider` | `Function<Integer, GetUpdates> provide()` | `GetUpdates` factory function |

### Usage pattern

```java
// Override one provider; everything else uses the default
@Bean
public BotOkHttpClientProvider botOkHttpClientProvider() {
    OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    return () -> client;          // return a singleton-equivalent value
}
```

See the transport-specific READMEs for full customisation examples.

---

## Extension Points

### `BotArgumentResolver`

Implement to inject custom types into handler method parameters:

```java
@Component
public class CurrentUserResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.getType().equals(AppUser.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest req, BotResponse res) {
        return userService.findByTelegramId(req.getUser().getId());
    }
}
```

### `BotReturnTypeHandler`

Implement to handle a custom method return type:

```java
@Component
public class MyReturnHandler implements BotReturnTypeHandler {

    @Override
    public boolean supportsReturnType(Object returnValue) {
        return returnValue instanceof MyResult;
    }

    @Override
    public void handleReturnValue(Object returnValue, BotRequest req, BotResponse res) {
        MyResult result = (MyResult) returnValue;
        res.addBotApiMethod(SendMessage.builder()
                .chatId(req.getChat().getId())
                .text(result.toText())
                .build());
    }
}
```

### `BotStartTrigger`

Called once after the bot has successfully connected to Telegram. Use it to register
commands, send a startup notification, etc.:

```java
@Component
public class BotStartupHook implements BotStartTrigger {

    @Override
    public void onStart(User botUser, TelegramClient client) {
        log.info("Bot @{} is live", botUser.getUserName());
    }
}
```

### `BotConfigurer`

Provides the `ObjectMapper` used for JSON serialisation/deserialisation and the `@BotCommandQueryParam` type converter:

```java
@Bean
public BotConfigurer botConfigurer() {
    return new BotConfigurer(new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
}
```

---

## Package Map

```
uz.osoncode.easygram.core
├── stereotype/                     @BotController, @BotControllerAdvice
├── annotation/                     @BotConfiguration, @BotMarkup, @BotOrder
├── bind/annotation/                @BotCommand, @BotText, @BotCallbackQuery,
│                                   @BotExceptionHandler, @BotCommandValue,
│                                   @BotTextValue, @BotReplyMarkup, etc.
├── argumentresolver/               SPI for custom parameter injection
├── bot/
│   ├── BotConfigurer               ObjectMapper provider
│   └── BotTransportType            Enum: LONG_POLLING, WEBHOOK, KAFKA_CONSUMER, RABBIT_CONSUMER
├── chatstate/
│   └── BotChatStateService         State read/write interface (+ enum overloads)
├── exception/
│   └── BotHandlerException         Framework-level unchecked exception
├── filter/
│   ├── BotFilter                   Filter SPI
│   └── BotFilterChain              Chain delegation interface
├── handler/
│   └── BotHandler                  Handler metadata SPI
├── model/
│   ├── BotRequest                  Update + context (User, Chat, TelegramClient, Throwable)
│   └── BotResponse                 BotApiMethod accumulator
├── returntypehandler/
│   └── BotReturnTypeHandler        SPI for custom return types
└── trigger/
    └── BotStartTrigger             Post-startup hook
```

---

## See Also

- [core/README.md](../core/README.md) — processing engine implementation
- [core-chatstate/README.md](../core-chatstate/README.md) — chat state guide
- [core-i18n/README.md](../core-i18n/README.md) — internationalisation
