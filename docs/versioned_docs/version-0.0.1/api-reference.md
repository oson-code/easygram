---
id: api-reference
title: API Reference
---

# API Reference

Complete reference for all easygram annotations, interfaces, model classes, and extension points.

---

## Table of Contents

- [Structural Annotations](#structural-annotations) — `@BotController`, `@BotControllerAdvice`, `@BotConfiguration`, `@BotMarkup`, `@BotOrder`
- [Handler Routing Annotations](#handler-routing-annotations) — `@BotCommand`, `@BotText`, `@BotTextPattern`, `@BotCallbackQuery`, `@BotContact`, `@BotLocation`, `@BotReplyButton`, `@BotDefaultHandler`, `@BotExceptionHandler`, …
- [Parameter Annotations](#parameter-annotations) — `@BotCommandValue`, `@BotTextValue`, `@BotCallbackQueryData`, `@BotCommandQueryParam`
- [Response Annotations](#response-annotations) — `@BotReplyMarkup`, `@BotClearMarkup`
- [Chat State Annotations](#chat-state-annotations) — `@BotChatState`, `@BotForwardChatState`, `@BotClearChatState`
- [Return Types](#return-types)
- [Model Classes](#model-classes) — `BotRequest`, `BotResponse`, `BotMetadata`, `BotMarkupContext`
- [Extension Interfaces](#extension-interfaces) — `BotFilter`, `BotArgumentResolver`, `BotReturnTypeHandler`, `BotHandlerInvocationFilter`, `BotChatStateService`, `BotUpdatePublisher`, `BotHandlerConditionContributor`, `BotStartTrigger`
- [i18n Services](#i18n-services-core-i18n) — `BotLocaleResolver`, `BotMessageSource`, `BotKeyboardFactory`
- [Filter Order Constants](#filter-order-constants)
- [Invocation Filter Order Constants](#invocation-filter-order-constants)
- [Configuration Properties](#configuration-properties)

---

## Structural Annotations

### @BotController

**Package:** `uz.osoncode.easygram.core.annotation`
**Target:** `TYPE`

Marks a class as a bot controller. All handler method annotations (`@BotCommand`, `@BotText`, etc.) inside are scanned and registered at startup. Equivalent to Spring's `@Component` — auto-detected by component scanning.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | `""` | Optional bean name |

```java
@BotController
public class StartController {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "!";
    }
}
```

---

### @BotControllerAdvice

**Package:** `uz.osoncode.easygram.core.annotation`
**Target:** `TYPE`

Marks a class as a global exception handler. `@BotExceptionHandler` methods inside apply to all `@BotController` classes. Controller-local handlers always take priority over advice handlers for the same exception type.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | `""` | Optional bean name |
| `basePackages` | `String[]` | `{}` | Restrict to controllers in these packages |
| `assignableTypes` | `Class<?>[]` | `{}` | Restrict to controllers assignable to these types |
| `annotations` | `Class<? extends Annotation>[]` | `{}` | Restrict to controllers annotated with these annotations |

```java
@BotControllerAdvice
public class GlobalExceptionHandler {

    @BotExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArg(IllegalArgumentException ex) {
        return "Invalid input: " + ex.getMessage();
    }
}

// Scope to a specific package
@BotControllerAdvice(basePackages = "com.example.bot.payment")
public class PaymentExceptionHandler {

    @BotExceptionHandler(PaymentException.class)
    public String handlePayment(PaymentException ex) {
        return "Payment failed: " + ex.getMessage();
    }
}
```

---

### @BotConfiguration

**Package:** `uz.osoncode.easygram.core.annotation`
**Target:** `TYPE`

Marks a class as a markup factory holder. Methods annotated with `@BotMarkup` inside are scanned and registered in `BotMarkupRegistry` at startup. Equivalent to Spring's `@Component`.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String` | `""` | Optional bean name |

```java
@BotConfiguration
public class MyMarkups {

    @BotMarkup("main_menu")
    public ReplyKeyboard mainMenu() {
        return ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow("Option 1", "Option 2"))
                .resizeKeyboard(true)
                .build();
    }
}
```

---

### @BotMarkup

**Package:** `uz.osoncode.easygram.core.annotation`
**Target:** `METHOD`

Registers a method as a keyboard factory under a string ID in `BotMarkupRegistry`. The method must return `ReplyKeyboard` (or a subtype).

| Attribute | Type | Required | Description |
|---|---|---|---|
| `value` | `String` | | Registry key used with `@BotReplyMarkup("id")` and `.withMarkup("id")` |

**Supported method signatures:**
- `() → ReplyKeyboard` — static, no context
- `(BotRequest) → ReplyKeyboard` — locale/user-aware
- Any combination of parameters resolvable by the argument resolver system (`User`, `Chat`, `Locale`, `BotRequest`, etc.)

```java
@BotConfiguration
public class Markups {

    // Static keyboard
    @BotMarkup("confirm_kb")
    public ReplyKeyboard confirmKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                    InlineKeyboardButton.builder().text(" Yes").callbackData("yes").build(),
                    InlineKeyboardButton.builder().text(" No").callbackData("no").build()
                ))
                .build();
    }

    // Context-aware keyboard — receives params passed via PlainReply.withMarkup("id", Map)
    @BotMarkup("product_kb")
    public ReplyKeyboard productKeyboard(BotMarkupContext ctx) {
        String productId = ctx.get("productId");
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                    InlineKeyboardButton.builder()
                        .text("Buy").callbackData("buy:" + productId).build()
                ))
                .build();
    }
}
```

---

### @BotOrder

**Package:** `uz.osoncode.easygram.core.annotation`
**Target:** `METHOD`

Controls handler execution priority within the same routing tier. Lower value = higher priority. State-specific handlers always beat state-agnostic ones at equal order.

| Attribute | Type | Default |
|---|---|---|
| `value` | `int` | `Integer.MAX_VALUE` |

```java
@BotCommand("/start")
@BotOrder(10)
public String highPriorityStart() { ... }

@BotCommand("/start")
@BotOrder(20)
public String lowPriorityStart() { ... }
```

---

## Handler Routing Annotations

All routing annotations live in `uz.osoncode.easygram.core.bind.annotation`.

**Injectable parameters for all handler methods:** `Update`, `User`, `Chat`, `TelegramClient`, `BotRequest`, `BotResponse`, and any type provided by a registered `BotArgumentResolver`.

---

### @BotCommand

**Target:** `METHOD`

Routes a command message (text starting with `/`). Empty `value()` matches any command not matched by a specific handler (same as `@BotDefaultCommand`).

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String[]` | `{}` | Command strings (e.g. `"/start"`). Empty = any unmatched command |

```java
@BotCommand("/start")
public String onStart(@BotCommandValue String cmd, User user) {
    return "Welcome, " + user.getFirstName() + "!";
}

@BotCommand({"/help", "/?"})
public String onHelp() {
    return "Available commands: /start, /help";
}
```

**Extra injectable parameters:** `@BotCommandValue String`, `@BotCommandQueryParam` values.

---

### @BotDefaultCommand

**Target:** `METHOD`

Fallback for any command not matched by a specific `@BotCommand` handler.

```java
@BotDefaultCommand
public String onUnknownCommand(@BotCommandValue String cmd) {
    return "Unknown command: " + cmd + ". Try /help.";
}
```

---

### @BotText

**Target:** `METHOD`

Routes exact text messages (case-sensitive, full-string equality). Empty `value()` matches any text not matched by a more specific handler.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String[]` | `{}` | Exact text strings. Empty = any unmatched text |

```java
@BotText("hello")
public String onHello() { return "Hey!"; }

@BotText({"yes", "Yes", "YES"})
public String onYes() { return "Confirmed!"; }
```

**Extra injectable parameters:** `@BotTextValue String`.

---

### @BotTextPattern

**Target:** `METHOD`

Routes text messages matching a regular expression. Matching uses `Matcher.find()` (substring). Use `^` and `$` anchors for full-string matching. Patterns are compiled once at startup and cached.

| Attribute | Type | Required | Description |
|---|---|---|---|
| `value` | `String[]` | | Regex patterns — fires if **any** pattern matches |

```java
// Full-string phone number match
@BotTextPattern("^\\d{10}$")
public String onPhone(@BotTextValue String phone) {
    return "Got number: " + phone;
}

// Substring match — any message containing "order"
@BotTextPattern("order \\S+")
public String onOrder(@BotTextValue String text) {
    return "Processing: " + text;
}

// Multiple patterns — fires if any matches
@BotTextPattern({"^buy .+", "^purchase .+"})
public String onPurchase(@BotTextValue String text) {
    return "Adding to cart: " + text;
}
```

---

### @BotTextDefault

**Target:** `METHOD`

Fallback for any text message not matched by `@BotText`, `@BotTextPattern`, or `@BotReplyButton`.

```java
@BotTextDefault
public String onAnyText(@BotTextValue String text) {
    return "You said: " + text;
}
```

---

### @BotCallbackQuery

**Target:** `METHOD`

Routes inline keyboard callback queries by exact data string. Empty `value()` matches any callback not matched by a specific handler.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String[]` | `{}` | Exact callback data strings. Empty = any unmatched callback |

```java
@BotCallbackQuery("btn_confirm")
public String onConfirm() { return "Confirmed!"; }

@BotCallbackQuery({"page_prev", "page_next"})
public String onPage(@BotCallbackQueryData String data) {
    return "Navigating: " + data;
}
```

**Extra injectable parameters:** `@BotCallbackQueryData String`.

---

### @BotDefaultCallbackQuery

**Target:** `METHOD`

Fallback for any callback query not matched by `@BotCallbackQuery`.

```java
@BotDefaultCallbackQuery
public String onUnknownCallback(@BotCallbackQueryData String data) {
    return "Unknown action: " + data;
}
```

---

### @BotContact

**Target:** `METHOD`

Routes messages containing a shared contact (phone number, name, etc.).

```java
@BotContact
public String onContact(Update update) {
    Contact c = update.getMessage().getContact();
    return "Received: " + c.getFirstName() + " — " + c.getPhoneNumber();
}
```

---

### @BotLocation

**Target:** `METHOD`

Routes messages containing a shared location.

```java
@BotLocation
public String onLocation(Update update) {
    Location loc = update.getMessage().getLocation();
    return "Location: " + loc.getLatitude() + ", " + loc.getLongitude();
}
```

---

### @BotReplyButton

**Target:** `METHOD`

Routes text messages matching a reply keyboard button label. With `core-i18n` on the classpath, `value()` strings are treated as `MessageSource` keys and resolved per request locale before matching.

| Attribute | Type | Required | Description |
|---|---|---|---|
| `value` | `String[]` | | Button labels or i18n message keys |

```java
@BotReplyButton(" Cancel")
@BotClearChatState
@BotClearMarkup
public String onCancel() { return "Cancelled."; }

// With core-i18n: value is a message bundle key
@BotReplyButton("button.cancel")
@BotClearChatState
public String onCancel() { return "Cancelled."; }
```

---

### @BotDefaultHandler

**Target:** `METHOD`, `TYPE`

Global catch-all — matches any update not handled by a more specific handler.

```java
@BotDefaultHandler
public String onDefault() {
    return "I don't understand that. Send /help for a list of commands.";
}
```

---

### @BotExceptionHandler

**Target:** `METHOD`

Handles exceptions thrown by handler methods. Declare inside `@BotController` (controller-scoped) or `@BotControllerAdvice` (global). Controller-local handlers take priority over advice handlers for the same type.

| Attribute | Type | Required | Description |
|---|---|---|---|
| `value` | `Class<? extends Throwable>[]` | | Exception types this handler handles |

```java
// In a controller — handles RuntimeException thrown by this controller's handlers only
@BotExceptionHandler(RuntimeException.class)
public String onRuntimeError(RuntimeException ex, User user) {
    log.error("Error for user {}: {}", user.getId(), ex.getMessage());
    return "Something went wrong. Please try again.";
}

// In @BotControllerAdvice — global handler
@BotExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
public String onBadInput(Throwable ex) {
    return "Bad input: " + ex.getMessage();
}
```

**Injectable parameters:** `Throwable` (the thrown exception), `User`, `Chat`, `BotRequest`, `BotResponse`, `Update`, `TelegramClient`.

---

## Parameter Annotations

All parameter annotations live in `uz.osoncode.easygram.core.bind.annotation`.

---

### @BotCommandValue

**Target:** `PARAMETER`

Injects the matched command string (e.g. `"/start"`).

```java
@BotCommand("/start")
public String onStart(@BotCommandValue String cmd) {
    // cmd == "/start"
    return "Command was: " + cmd;
}
```

---

### @BotTextValue

**Target:** `PARAMETER`

Injects the full raw message text.

```java
@BotTextDefault
public String echo(@BotTextValue String text) {
    return "You said: " + text;
}
```

---

### @BotCallbackQueryData

**Target:** `PARAMETER`

Injects the callback query data string.

```java
@BotCallbackQuery("action_buy")
public String onBuy(@BotCallbackQueryData String data) {
    return "Action: " + data;
}
```

---

### @BotCommandQueryParam

**Target:** `PARAMETER`

Injects a typed positional argument following a command. For `/start 42`, injects `42` as the declared type. Conversion is performed via Jackson.

Supported types include: `String`, `Integer`, `Long`, `Boolean`, `Double`, and any Jackson-deserializable type.

```java
// User sends: /start 42
@BotCommand("/start")
public String onDeepLink(@BotCommandQueryParam Integer referralCode) {
    return "Referred by: " + referralCode;
}

// User sends: /open {"userId":7,"role":"admin"}
@BotCommand("/open")
public String onOpen(@BotCommandQueryParam MyParams params) {
    return "Opening for: " + params.getUserId();
}
```

---

## Response Annotations

### @BotReplyMarkup

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Attaches a pre-registered keyboard to the response by looking it up in `BotMarkupRegistry` at invocation time. Acts as a **fallback** — only applied if the return value itself carries no markup (i.e. `PlainReply.withMarkup(...)` takes precedence).

```java
@BotCommand("/menu")
@BotReplyMarkup("main_menu")
public String onMenu() {
    return "Here is the menu:";
}
```

Compatible with `String`, `PlainReply`, `PlainTextTemplate`, and `LocalizedReply` return types.

---

### @BotClearMarkup

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Sends a `ReplyKeyboardRemove` along with the response. Always overrides `@BotReplyMarkup` when both are present.

```java
@BotCommand("/cancel")
@BotClearChatState
@BotClearMarkup
public String onCancel() {
    return "Cancelled. Keyboard removed.";
}
```

---

## Chat State Annotations

All chat state annotations live in `uz.osoncode.easygram.core.bind.annotation` (and `core-chatstate`).

---

### @BotChatState

**Target:** `METHOD`, `TYPE`

Restricts a handler to run only when the chat's current state matches one of the declared values. On a class, applies as the default for all methods in that class.

```java
// Method-level: run only in AWAITING_PHONE state
@BotContact
@BotChatState("AWAITING_PHONE")
public String onPhone(Update update) { ... }

// Class-level: all methods default to AWAITING_NAME | AWAITING_AGE
@BotController
@BotChatState({"AWAITING_NAME", "AWAITING_AGE"})
public class RegistrationController {

    @BotTextDefault
    // inherits @BotChatState({"AWAITING_NAME", "AWAITING_AGE"}) from class
    public String onText(@BotTextValue String text) { ... }

    @BotCommand("/cancel")
    @BotChatState // empty — overrides class restriction, accepts any state
    public String onCancel() { return "Cancelled."; }
}
```

---

### @BotForwardChatState

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Transitions the chat to a new state **after** the handler method returns successfully.

| Attribute | Type | Required | Description |
|---|---|---|---|
| `value` | `String` | | State name to set |

```java
@BotTextDefault
@BotChatState("STEP_1")
@BotForwardChatState("STEP_2")
public String step1(@BotTextValue String name, BotRequest request) {
    // After return: chatStateService.setState(request.getChat().getId(), "STEP_2")
    return "Got it! Now send your age.";
}
```

---

### @BotClearChatState

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Clears the chat state (sets to `null`) **after** the handler method returns successfully.

```java
@BotCommand("/done")
@BotClearChatState
public String onDone() {
    return "Flow complete. State cleared.";
}
```

---

## Return Types

Handler methods may return any of the following types. `null` is treated as no-op (same as `void`).

| Return type | Module | Behavior |
|---|---|---|
| `void` | `core` | No message sent |
| `null` | `core` | No-op — same as `void` |
| `String` | `core` | `SendMessage` to current chat. `@BotReplyMarkup`/`@BotClearMarkup` applied by `BotStringReturnHandler` |
| `PlainReply` | `core` | `SendMessage` with optional markup. Implements `MarkupAware` |
| `PlainTextTemplate` | `core` | `SendMessage` with `#{index}` token substitution. Implements `MarkupAware` |
| `BotApiMethod<?>` | `core` | Enqueued and executed directly by `BotApiSenderFilter` |
| `Collection<BotApiMethod<?>>` | `core` | All methods enqueued and executed in order |
| `Collection<Object>` | `core` | Each element individually dispatched to matching `BotReturnTypeHandler` via `supportsElement()` |
| `LocalizedReply` | `core-i18n` | `MessageSource` key lookup resolved to locale-aware `SendMessage`. Implements `MarkupAware` |
| `LocalizedTemplate` | `core-i18n` | Mixed `${key}` / `#{index}` template resolved per locale. Implements `MarkupAware` |

### PlainReply — Fluent API

`PlainReply` is an immutable value object. All wither methods return new instances. A builder is also available.

```java
PlainReply.of("Hello!")                          // text only
PlainReply.of("Choose:").withMarkup("main_menu") // attach registered keyboard
PlainReply.of("Buy?").withMarkup("product_kb",   // attach keyboard with params
    Map.of("productId", "42"))
PlainReply.of("Pick:").withKeyboard(myKeyboard)  // attach ReplyKeyboard directly
PlainReply.of("Done.").removeMarkup()            // send ReplyKeyboardRemove

// Builder
PlainReply.builder()
    .text("Choose:")
    .markupId("main_menu")
    .build()
```

### PlainTextTemplate — Fluent API

Uses `#{index}` positional tokens (0-based) for value substitution. Same markup methods as `PlainReply`.

```java
PlainTextTemplate.of("Hello, #{0}! You have #{1} messages.", user.getFirstName(), count)
PlainTextTemplate.of("Order ##{0} ready.", orderId).withMarkup("order_kb")

// Builder
PlainTextTemplate.builder()
    .template("Hello, #{0}! Order ##{1} is ready.")
    .args(user.getFirstName(), orderId)
    .markupId("order_kb")
    .build()
```

### LocalizedReply — Fluent API *(core-i18n)*

Key is resolved via `BotMessageSource` using the request locale.

```java
LocalizedReply.of("welcome.message", user.getFirstName())
LocalizedReply.of("choose.option").withMarkup("main_menu")
LocalizedReply.of("confirm.prompt").withMarkup("confirm_kb", Map.of("id", itemId))

// Builder
LocalizedReply.builder()
    .key("welcome.message")
    .args(user.getFirstName())
    .markupId("main_menu")
    .build()
```

### LocalizedTemplate — Fluent API *(core-i18n)*

Supports mixed `${messageKey}` bundle lookups and `#{index}` positional arg substitution.

```java
LocalizedTemplate.of("${welcome.title}\n\nHello, #{0}!", user.getFirstName())
LocalizedTemplate.of("${stats.header}\n\nMessages: #{0}", count).withMarkup("stats_menu")

// Builder
LocalizedTemplate.builder()
    .template("${stats.header}\n\nMessages: #{0}\nCommands: #{1}")
    .args(messages, commands)
    .markupId("stats_menu")
    .build()
```

---

## Model Classes

### BotRequest

**Package:** `uz.osoncode.easygram.core.model`

The per-request context object. Created once per incoming Telegram update and carried through the entire filter chain into handler methods.

```java
// Core data
Update getUpdate() // raw Telegram Update
User getUser() // resolved sender (set by BotContextSetterFilter)
Chat getChat() // resolved chat (set by BotContextSetterFilter)
TelegramClient getTelegramClient()
BotMetadata getBotMetadata() // bot id, username, token
Throwable getThrowable() // populated inside @BotExceptionHandler

// Request-scoped attribute store
void setAttribute(String key, Object value) // null value removes the key
<T> T getAttribute(String key)
Map<String,Object> getAttributes() // unmodifiable view
```

**Sharing data between filters and handlers via attributes:**

```java
// In a BotFilter:
request.setAttribute("userId", resolvedUserId);

// In a handler or BotArgumentResolver:
Long userId = request.getAttribute("userId");
```

---

### BotResponse

**Package:** `uz.osoncode.easygram.core.model`

The per-request response accumulator. Queued `BotApiMethod` calls are executed in insertion order by `BotApiSenderFilter` after the full filter chain completes.

```java
void addBotApiMethod(BotApiMethod<?> method)
void addBotApiMethods(Collection<BotApiMethod<?>> methods)
Collection<BotApiMethod<?>> getBotApiMethods() // current queue, read-only

// Response-scoped attribute store
void setAttribute(String key, Object value)
<T> T getAttribute(String key)
Map<String,Object> getAttributes()
```

**Building and queuing API calls from a filter:**

```java
@Override
public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) {
    if (!isAllowed(request.getUser())) {
        response.addBotApiMethod(
            SendMessage.builder()
                .chatId(request.getChat().getId())
                .text(" Access denied.")
                .build()
        );
        return; // do NOT call chain.doFilter — pipeline stops here
    }
    chain.doFilter(request, response);
}
```

---

### BotMetadata

**Package:** `uz.osoncode.easygram.core.model`

Bot's own registration data. Available via `BotRequest.getBotMetadata()`.

```java
Long getId()
String getUsername()
String getToken()
```

---

### BotMarkupContext

**Package:** `uz.osoncode.easygram.core.markup`

Carries parameters into a `@BotMarkup` factory method when the keyboard was requested with `.withMarkup(id, Map)`. Available as a method parameter in any `@BotMarkup` method.

```java
static BotMarkupContext of(Map<String,Object> params)
static BotMarkupContext empty()

<T> T get(String key)
<T> T get(String key, Class<T> type)
<T> T getOrDefault(String key, T defaultValue)
boolean has(String key)
Map<String,Object> asMap()

String REQUEST_ATTRIBUTE_KEY = "__botMarkupContext__"
```

```java
// Return value passes params:
return PlainReply.of("View product:").withMarkup("product_kb", Map.of("productId", "99"));

// Factory receives them:
@BotMarkup("product_kb")
public ReplyKeyboard productKeyboard(BotMarkupContext ctx) {
    String id = ctx.get("productId");
    return InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(
                InlineKeyboardButton.builder()
                    .text(" Buy").callbackData("buy:" + id).build()
            ))
            .build();
}
```

---

## Extension Interfaces

All extension interfaces are registered as Spring `@Bean`s (or `@Component`s). The framework auto-collects them via `List<T>` injection.

---

### BotFilter

**Package:** `uz.osoncode.easygram.core.filter`

Intercepts every update before it reaches a handler. Analogous to a Servlet `Filter`.

```java
public interface BotFilter {

    /** Lower value = runs earlier. Default: Integer.MAX_VALUE. */
    default int getOrder() { return Integer.MAX_VALUE; }

    /** Return false to skip this filter for the current request. Default: always runs. */
    default boolean shouldFilter() { return true; }

    /** Call chain.doFilter(request, response) to continue; omit to short-circuit. */
    void doFilter(BotRequest request, BotResponse response, BotFilterChain chain);
}
```

```java
@Component
public class RateLimitFilter implements BotFilter {

    @Override
    public int getOrder() { return BotFilterOrder.PUBLISHING + 1; }

    @Override
    public void doFilter(BotRequest req, BotResponse res, BotFilterChain chain) {
        if (rateLimiter.tryAcquire(req.getUser().getId())) {
            chain.doFilter(req, res);
        } else {
            res.addBotApiMethod(SendMessage.builder()
                .chatId(req.getChat().getId())
                .text("Slow down!").build());
        }
    }
}
```

See [Custom Filters](advanced/custom-filters) for more examples.

---

### BotArgumentResolver

**Package:** `uz.osoncode.easygram.core.argumentresolver`

Resolves custom types or annotations into handler method parameters.

```java
public interface BotArgumentResolver {

    /** Return true to claim this parameter. Called once per handler registration. */
    boolean supportsParameter(java.lang.reflect.Parameter parameter);

    /** Return the resolved value. May return null for optional parameters. */
    Object resolveArgument(java.lang.reflect.Parameter parameter,
                           BotRequest request,
                           BotResponse response);
}
```

The framework invokes `supportsParameter` for each unresolved parameter and delegates to the **first matching resolver**. Built-in resolvers handle: `Update`, `User`, `Chat`, `TelegramClient`, `BotRequest`, `BotResponse`, `Throwable`, `Locale` (core-i18n), and all parameter annotations. Custom resolvers are ordered by `@Order`.

```java
@Component
public class CurrentUserResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter p) {
        return p.getType() == AppUser.class;
    }

    @Override
    public Object resolveArgument(Parameter p, BotRequest req, BotResponse res) {
        return userService.findByTelegramId(req.getUser().getId());
    }
}
```

See [Custom Argument Resolvers](advanced/custom-argument-resolvers) for more examples.

---

### BotReturnTypeHandler

**Package:** `uz.osoncode.easygram.core.returntypehandler`

Converts handler method return values into queued `BotApiMethod` calls.

```java
public interface BotReturnTypeHandler {

    /** Return true if this handler owns the return type of the given method. */
    boolean supportsReturnType(Method method);

    /**
     * Return true if this handler can dispatch a single element inside a Collection<Object>.
     * Enables mixed-collection routing — each element is dispatched independently.
     */
    default boolean supportsElement(Object element) { return false; }

    /** Process returnValue and add resulting API calls to response. */
    void handleReturnType(BotRequest request, BotResponse response, Object returnValue);

    /**
     * Annotation-aware variant — override when you need access to @BotReplyMarkup etc.
     * Default implementation delegates to the 3-arg overload.
     */
    default void handleReturnType(BotRequest request, BotResponse response,
                                  Object returnValue, Method method) {
        handleReturnType(request, response, returnValue);
    }
}
```

`BotReturnTypeHandlerFactory` collects all `BotReturnTypeHandler` beans and returns the **first** whose `supportsReturnType` returns `true`. Registration order matters — use `@Order` to control priority.

See [Custom Return-Type Handlers](advanced/custom-return-handlers) for examples.

---

### BotHandlerInvocationFilter

**Package:** `uz.osoncode.easygram.core.handler.invocation`

Intercepts handler method invocation after the dispatcher has selected a handler but before the method is called. Runs inside the inner invocation chain, distinct from the outer `BotFilter` pipeline.

```java
public interface BotHandlerInvocationFilter {

    /** Lower value = runs earlier. Default: Integer.MAX_VALUE. */
    default int getOrder() { return Integer.MAX_VALUE; }

    /** Call chain.proceed(ctx) to continue; omit to short-circuit. */
    void invoke(BotHandlerInvocationContext ctx, BotHandlerInvocationChain chain);
}
```

**Built-in invocation filters (executed in this order):**

| Filter | Order constant | Purpose |
|---|---|---|
| `MethodInvocationFilter` | `METHOD_INVOCATION` (`MIN_VALUE`) | Resolves arguments and invokes the controller method |
| `MarkupApplicationFilter` | `MARKUP_APPLICATION` (`MIN_VALUE + 1`) | Applies `@BotReplyMarkup`/`@BotClearMarkup` to the return value |
| `ReturnTypeDispatchFilter` | `RETURN_TYPE_DISPATCH` (`MIN_VALUE + 2`) | Routes the return value to the matching `BotReturnTypeHandler` |
| `ChatStateUpdateFilter` | `CHAT_STATE_UPDATE` (`MIN_VALUE + 3`) | Applies `@BotForwardChatState`/`@BotClearChatState` |

Custom filters with `getOrder() < METHOD_INVOCATION` run before method execution (e.g. permission guards). Inserted between constants, they run at that stage.

---

### BotChatStateService

**Package:** `uz.osoncode.easygram.core.chatstate`

Persistence layer for per-chat state. The default `InMemoryBotChatStateService` (from `core-chatstate`) stores state in a `ConcurrentHashMap`. Replace with a custom `@Bean` backed by Redis, JDBC, or any other store.

```java
public interface BotChatStateService {

    String getState(Long chatId);
    void setState(Long chatId, String state); // null clears the state

    // Enum convenience helpers
    default void setState(Long chatId, Enum<?> state) { setState(chatId, state.name()); }
    default <T extends Enum<T>> T getStateAs(Long chatId, Class<T> enumClass) { ... }
}
```

```java
// Custom Redis-backed implementation
@Bean
public BotChatStateService redisChatStateService(StringRedisTemplate redis) {
    return new RedisBotChatStateService(redis);
}
```

See [Chat State Backends](advanced/chat-state-backends) for examples.

---

### BotUpdatePublisher

**Package:** `uz.osoncode.easygram.messaging-api`

SPI for publishing raw Telegram updates to a message broker. Implement and register as a `@Bean` to replace or augment the built-in Kafka/RabbitMQ publishers.

```java
public interface BotUpdatePublisher {
    void publish(BotRequest request) throws Exception;
}
```

See [Broker Publishing](advanced/broker-publishing) for examples.

---

### BotHandlerConditionContributor

**Package:** `uz.osoncode.easygram.core.handler`

Contributes additional match conditions to every handler at startup. Register as a `@Bean`. Useful for permission annotations, feature flags, or other cross-cutting routing concerns.

```java
public interface BotHandlerConditionContributor {
    List<BotHandlerCondition> contribute(Method method, Object bean);
}
```

```java
@Component
public class RequiresAdminContributor implements BotHandlerConditionContributor {

    @Override
    public List<BotHandlerCondition> contribute(Method method, Object bean) {
        if (!method.isAnnotationPresent(RequiresAdmin.class)) return List.of();
        return List.of(request -> adminService.isAdmin(request.getUser().getId()));
    }
}
```

---

### BotStartTrigger

**Package:** `uz.osoncode.easygram.core.trigger`

Executed once at application startup after the bot's own `User` object and `TelegramClient` become available. Use for initial setup — registering webhook, sending notifications, etc.

```java
public interface BotStartTrigger {
    void execute(User bot, TelegramClient telegramClient);
}
```

```java
@Component
public class RegisterCommandsTrigger implements BotStartTrigger {

    @Override
    public void execute(User bot, TelegramClient client) {
        client.execute(SetMyCommands.builder()
            .command(BotCommand.builder().command("/start").description("Start the bot").build())
            .command(BotCommand.builder().command("/help").description("Help").build())
            .build());
    }
}
```

---

## i18n Services *(core-i18n)*

All i18n services live in `uz.osoncode.easygram.core.i18n` and are auto-configured by `BotI18nAutoConfiguration` when `core-i18n` is on the classpath. All are `@ConditionalOnMissingBean` — override any with your own `@Bean`.

---

### BotLocaleResolver

Resolves the `Locale` for a given `BotRequest`. The default implementation uses the `language_code` field from the Telegram `User` object.

```java
public interface BotLocaleResolver {
    Locale resolve(BotRequest request);
}
```

```java
// Custom: resolve from a database user preference
@Bean
public BotLocaleResolver dbLocaleResolver(UserRepository users) {
    return request -> users.findLocale(request.getUser().getId());
}
```

---

### BotMessageSource

Locale-aware wrapper around Spring's `MessageSource`. Inject wherever you need to look up i18n messages outside of a handler return type.

```java
public interface BotMessageSource {

    String getMessage(String code, BotRequest request, Object... args);
    String getMessage(String code, Locale locale, Object... args);
    String getOrDefault(String code, String defaultMessage, BotRequest request, Object... args);
    Locale resolveLocale(BotRequest request);
}
```

```java
@BotController
@RequiredArgsConstructor
public class NotifyController {

    private final BotMessageSource messageSource;
    private final TelegramClient telegramClient;

    @BotCommand("/notify")
    public void onNotify(BotRequest request) {
        String text = messageSource.getMessage("notification.ready", request);
        telegramClient.execute(
            SendMessage.builder().chatId(request.getChat().getId()).text(text).build()
        );
    }
}
```

---

### BotKeyboardFactory

**Package:** `uz.osoncode.easygram.core.i18n.keyboard`

Fluent factory for building localized inline and reply keyboards. Inject into `@BotConfiguration` markup factory methods or any Spring bean.

#### Inline keyboard builder

```java
// Start a builder scoped to the request locale
BotKeyboardFactory.InlineKeyboardBuilder builder = factory.inline(request);
// Or: factory.inline(locale)

builder
    .row("btn.yes", "cb_yes", "btn.no", "cb_no") // alternating (messageKey, callbackData) pairs
    .row(myCustomButton)
    .build(); // → InlineKeyboardMarkup
```

`.row(String... textAndCallbackPairs)` — pairs are interleaved: `(messageKey₁, callbackData₁, messageKey₂, callbackData₂, …)`.

#### Reply keyboard builder

```java
BotKeyboardFactory.ReplyKeyboardBuilder builder = factory.reply(request);

builder
    .row("btn.option1", "btn.option2") // message keys → resolved to localized button labels
    .row("btn.cancel")
    .resizeKeyboard(true) // default: true
    .oneTimeKeyboard(false) // default: false
    .build(); // → ReplyKeyboardMarkup
```

#### Single button factories

```java
InlineKeyboardButton btn = factory.inlineButton("btn.details", "cb_details", request);
KeyboardButton btn = factory.replyButton("btn.share_contact", request);
```

**Example — localized markup factory:**

```java
@BotConfiguration
@RequiredArgsConstructor
public class LocalizedMarkups {

    private final BotKeyboardFactory keyboardFactory;

    @BotMarkup("main_menu")
    public ReplyKeyboard mainMenu(BotRequest request) {
        return keyboardFactory.reply(request)
                .row("menu.catalog", "menu.cart", "menu.profile")
                .row("menu.support")
                .build();
    }

    @BotMarkup("confirm_kb")
    public ReplyKeyboard confirmKeyboard(BotRequest request) {
        return keyboardFactory.inline(request)
                .row("btn.yes", "confirm_yes", "btn.no", "confirm_no")
                .build();
    }
}
```

---

## Filter Order Constants

**Class:** `BotFilterOrder` (`uz.osoncode.easygram.core.filter`)

| Constant | Value | Built-in filter |
|---|---|---|
| `CONTEXT_SETTER` | `Integer.MIN_VALUE` | Sets `User` and `Chat` on `BotRequest` |
| `OBSERVATION` | `MIN_VALUE + 1` | Micrometer observation span (core-observability) |
| `API_SENDER` | `MIN_VALUE + 2` | Executes queued `BotApiMethod` calls via `TelegramClient` |
| `PUBLISHING` | `MIN_VALUE + 1000` | Forwards update to message broker (messaging-*) |
| *(custom default)* | `MAX_VALUE` | Default for user-defined filters — runs last |

Custom filters that need to run **after** context is set but **before** the handler should use a value between `API_SENDER` and `PUBLISHING` (e.g. `0` or `100`).

---

## Invocation Filter Order Constants

**Class:** `BotHandlerInvocationFilterOrder` (`uz.osoncode.easygram.core.handler.invocation`)

| Constant | Value | Built-in filter |
|---|---|---|
| `METHOD_INVOCATION` | `Integer.MIN_VALUE` | Resolves args and invokes the controller method |
| `MARKUP_APPLICATION` | `MIN_VALUE + 1` | Applies `@BotReplyMarkup` / `@BotClearMarkup` to return value |
| `RETURN_TYPE_DISPATCH` | `MIN_VALUE + 2` | Routes return value to `BotReturnTypeHandler` |
| `CHAT_STATE_UPDATE` | `MIN_VALUE + 3` | Applies `@BotForwardChatState` / `@BotClearChatState` |

---

## Configuration Properties

### Required

```yaml
telegram:
  bot:
    token: YOUR_BOT_TOKEN # From @BotFather — required for all transports
```

### Transport

```yaml
telegram:
  bot:
    transport: LONG_POLLING # Default
                              # Options: LONG_POLLING | WEBHOOK | KAFKA_CONSUMER | RABBIT_CONSUMER
```

### Long-Polling

Long-polling has no additional configurable properties. The polling behaviour (timeout, batch size, back-off) is handled internally by the telegrambots library and cannot be overridden via `application.yml`.

### Webhook

```yaml
telegram:
  bot:
    webhook:
      url: https://my-bot.example.com/webhook # Required — publicly reachable HTTPS URL
      path: /webhook # Local handler path (default: /webhook)
      secret-token: ${WEBHOOK_SECRET} # Recommended — validates Telegram requests
      max-connections: 40
      drop-pending-updates: false
      unregister-on-shutdown: false
```

### Broker Publishing

```yaml
telegram:
  bot:
    messaging:
      forward-only: false # true = publish to broker only, skip local handler dispatch
      producer:
        producer-type: kafka # kafka | rabbit
      kafka:
        topic: telegram-updates
        create-if-absent: true
        partitions: 1
        replication-factor: 1
      rabbit:
        exchange: telegram-exchange
        routing-key: telegram.updates
        queue: telegram-updates
        create-if-absent: true
```

### Broker Consumer

```yaml
telegram:
  bot:
    messaging:
      kafka:
        topic: telegram-updates
        group-id: my-bot-consumer
      rabbit:
        queue: telegram-updates
```

### i18n *(core-i18n)*

```yaml
telegram:
  bot:
    i18n:
      default-locale: en # Fallback locale when user language_code is absent

spring:
  messages:
    basename: messages/bot # Classpath location of .properties files (e.g. messages/bot_en.properties)
    encoding: UTF-8
    cache-duration: 3600
```

---

Need more detail? Check the [module READMEs](https://github.com/oson-code/easygram/tree/main)
or open an [issue on GitHub](https://github.com/oson-code/easygram/issues).
