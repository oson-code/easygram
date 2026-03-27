---
id: parameter-injection
title: Parameter Injection
---

# Parameter Injection

Easygram automatically resolves and injects parameters into your handler methods via a set of built-in argument resolvers. You can declare any combination of supported types in any order — the framework matches each parameter by type (and annotation, where required).

## Complete Reference

| Resolved type / annotation | Module | Notes |
|---|---|---|
| `Update` | core | Raw Telegram update object |
| `User` | core | Message sender; set by `BotContextSetterFilter` |
| `Chat` | core | Chat context; set by `BotContextSetterFilter` |
| `BotRequest` | core | Current request object |
| `BotResponse` | core | Mutable response accumulator |
| `TelegramClient` | core | Telegram API client |
| `BotMetadata` | core | Bot username and ID |
| `Throwable` (or any subclass) | core | Exception — only populated in `@BotExceptionHandler` methods |
| `@BotCommandValue String` | core | The matched command (e.g. `"/start"`) |
| `@BotTextValue String` | core | Full message text |
| `@BotCallbackQueryData String` | core | Callback query data string |
| `@BotCommandQueryParam T` | core | Typed positional argument after the command (Jackson-converted) |
| `Contact` | core | Contact object — only in `@BotContact` handlers |
| `Location` | core | Location object — only in `@BotLocation` handlers |
| `InlineQuery` | core | Inline query — only in `@BotInlineQuery` handlers |
| `ChosenInlineResult` | core | Chosen inline result — only in `@BotChosenInlineResult` handlers |
| `ShippingQuery` | core | Shipping query — only in `@BotShippingQuery` handlers |
| `PreCheckoutQuery` | core | Pre-checkout query — only in `@BotPreCheckoutQuery` handlers |
| `Poll` | core | Poll object — only in `@BotPoll` handlers |
| `PollAnswer` | core | Poll answer — only in `@BotPollAnswer` handlers |
| `ChatMemberUpdated` | core | Chat member update — only in `@BotMyChatMember` / `@BotChatMember` handlers |
| `ChatJoinRequest` | core | Chat join request — only in `@BotChatJoinRequest` handlers |
| `BusinessConnection` | core | Business connection — only in `@BotBusinessConnection` handlers |
| `BusinessMessagesDeleted` | core | Deleted business messages — only in `@BotDeletedBusinessMessages` handlers |
| `BotMarkupContext` | core | Dynamic markup params — only in `@BotMarkup` factory methods |
| `Locale` | core-i18n | User's locale; requires `core-i18n` on the classpath |
| `@BotInlineQueryValue String` | core | Inline query text string — only in `@BotInlineQuery` handlers |
| `@BotChosenInlineResultId String` | core | Chosen result id — only in `@BotChosenInlineResult` handlers |
| `@BotShippingPayload String` | core | Invoice payload string — only in `@BotShippingQuery` handlers |
| `@BotPreCheckoutPayload String` | core | Invoice payload string — only in `@BotPreCheckoutQuery` handlers |

---

## Core Types

### User & Chat

`User` and `Chat` are extracted from the incoming `Update` by `BotContextSetterFilter` before your handler runs.

```java
@BotCommand("/whoami")
public String whoAmI(User user, Chat chat) {
    return "You are " + user.getFirstName() + " in chat " + chat.getId();
}
```

### Update

Inject the raw Telegram `Update` when you need access to data not exposed by the higher-level parameters.

```java
@BotCommand("/debug")
public String debug(Update update) {
    return "Update ID: " + update.getUpdateId();
}
```

### BotRequest & BotResponse

`BotRequest` is the internal request wrapper for the current update. `BotResponse` is a mutable accumulator — methods enqueued on it are sent by `BotApiSenderFilter` after the handler chain completes.

```java
@BotCommand("/status")
public void status(BotRequest request, BotResponse response) {
    long chatId = request.getChat().getId();
    response.addApiMethod(SendMessage.builder()
        .chatId(chatId)
        .text("Status: OK")
        .build());
}
```

### TelegramClient

Use `TelegramClient` to execute API calls directly and inspect the result synchronously.

```java
@BotCommand("/pin")
public void pin(Chat chat, TelegramClient client) throws TelegramApiException {
    client.execute(PinChatMessage.builder()
        .chatId(chat.getId())
        .messageId(someMessageId)
        .build());
}
```

### BotMetadata

Provides the bot's own username and ID, as registered with Telegram.

```java
@BotCommand("/me")
public String botInfo(BotMetadata meta) {
    return "I am @" + meta.getUsername() + " (ID " + meta.getBotId() + ")";
}
```

---

## Annotated Parameters

### @BotCommandValue

Injects the matched command string exactly as received.

```java
@BotCommand("/start")
public String onStart(@BotCommandValue String command) {
    // command = "/start"
    return "Hello from " + command;
}
```

### @BotTextValue

Injects the full text of the incoming message.

```java
@BotText("hello")
public String onHello(@BotTextValue String text) {
    // text = "hello"
    return "You said: " + text;
}
```

### @BotCallbackQueryData

Injects the data string from an inline keyboard callback query.

```java
@BotCallbackQuery("action:")
public String onAction(@BotCallbackQueryData String data) {
    // data = "action:something"
    return "Received: " + data;
}
```

### @BotCommandQueryParam

Extracts and type-converts the single positional argument that follows the command (the second space-separated token). Conversion is performed via Jackson's `ObjectMapper`, so any type Jackson can deserialize from a plain string is supported.

```java
// User sends: /start 42
@BotCommand("/start")
public String onDeepLink(@BotCommandQueryParam Integer referralCode) {
    return "Referred by: " + referralCode;
}
```

```java
// User sends: /item widget
@BotCommand("/item")
public String onItem(@BotCommandQueryParam String itemName) {
    return "You selected: " + itemName;
}
```

> **Note:** Only one positional argument is supported — the token at `parts[1]` (index 1 after splitting on whitespace). There is no named-parameter syntax.

---

## Handler-Specific Parameters

### Contact (`@BotContact`)

`Contact` is only resolved when the handler is annotated with `@BotContact`.

```java
@BotContact
public String onContact(User user, Contact contact) {
    return "Received contact: " + contact.getPhoneNumber();
}
```

### Location (`@BotLocation`)

`Location` is only resolved when the handler is annotated with `@BotLocation`.

```java
@BotLocation
public String onLocation(Location location) {
    return "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude();
}
```

### Throwable (`@BotExceptionHandler`)

`Throwable` (or any subclass) is only resolved inside `@BotExceptionHandler` methods. Declare the most specific exception type you need — the resolver matches any assignable subclass.

```java
@BotExceptionHandler
public String onError(Throwable ex, User user) {
    return "Sorry " + user.getFirstName() + ", something went wrong: " + ex.getMessage();
}

// Or target a specific exception type:
@BotExceptionHandler
public String onIllegalArg(IllegalArgumentException ex) {
    return "Bad input: " + ex.getMessage();
}
```

### BotMarkupContext (`@BotMarkup` factory methods only)

`BotMarkupContext` carries runtime parameters passed when a handler returns a `PlainReply` (or similar) with a parameterised markup ID. It is **only** available in `@BotMarkup`-annotated factory methods inside a `@BotConfiguration` class — not in regular handler methods.

```java
@BotConfiguration
public class Keyboards {

    @BotMarkup("confirm")
    public ReplyKeyboard confirmKeyboard(BotMarkupContext ctx) {
        String action = (String) ctx.getParam("action");
        return InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(
                InlineKeyboardButton.builder().text("Yes").callbackData("yes:" + action).build(),
                InlineKeyboardButton.builder().text("No").callbackData("no:" + action).build()
            ))
            .build();
    }
}
```

The markup is requested from a handler via `PlainReply.withMarkup`:

```java
@BotCommand("/delete")
public PlainReply onDelete() {
    return PlainReply.of("Are you sure?")
        .withMarkup("confirm", Map.of("action", "delete"));
}
```

---

## New Parameter Types (0.0.2)

The following type-based and annotation-based parameter resolvers were added in 0.0.2 to support the new update-type handler annotations.

### InlineQuery (`@BotInlineQuery`)

Inject the raw `InlineQuery` object in handlers annotated with `@BotInlineQuery`.

```java
@BotInlineQuery
public void onInline(InlineQuery query) {
    log.info("Inline query from user {}: {}", query.getFrom().getId(), query.getQuery());
}
```

### ChosenInlineResult (`@BotChosenInlineResult`)

Inject the `ChosenInlineResult` object in handlers annotated with `@BotChosenInlineResult`.

```java
@BotChosenInlineResult
public void onChosen(ChosenInlineResult result) {
    log.info("Result '{}' chosen by user {}", result.getResultId(), result.getFrom().getId());
}
```

### ShippingQuery (`@BotShippingQuery`)

Inject the `ShippingQuery` object in handlers annotated with `@BotShippingQuery`.

```java
@BotShippingQuery
public void onShipping(ShippingQuery query) {
    log.info("Shipping address: {}", query.getShippingAddress());
}
```

### PreCheckoutQuery (`@BotPreCheckoutQuery`)

Inject the `PreCheckoutQuery` object in handlers annotated with `@BotPreCheckoutQuery`.

```java
@BotPreCheckoutQuery
public void onPreCheckout(PreCheckoutQuery query) {
    log.info("Pre-checkout for {} {} from user {}", query.getTotalAmount(), query.getCurrency(), query.getFrom().getId());
}
```

### Poll (`@BotPoll`)

Inject the `Poll` object in handlers annotated with `@BotPoll`.

```java
@BotPoll
public void onPoll(Poll poll) {
    log.info("Poll '{}' updated", poll.getQuestion());
}
```

### PollAnswer (`@BotPollAnswer`)

Inject the `PollAnswer` object in handlers annotated with `@BotPollAnswer`.

```java
@BotPollAnswer
public void onPollAnswer(PollAnswer answer) {
    log.info("User {} answered poll {}", answer.getUser().getId(), answer.getPollId());
}
```

### ChatMemberUpdated (`@BotMyChatMember` / `@BotChatMember`)

Inject `ChatMemberUpdated` in handlers annotated with `@BotMyChatMember` or `@BotChatMember`.

```java
@BotMyChatMember
public void onBotStatus(ChatMemberUpdated update) {
    log.info("Bot status changed: {} → {}", update.getOldChatMember().getStatus(), update.getNewChatMember().getStatus());
}
```

### ChatJoinRequest (`@BotChatJoinRequest`)

Inject the `ChatJoinRequest` object in handlers annotated with `@BotChatJoinRequest`.

```java
@BotChatJoinRequest
public void onJoinRequest(ChatJoinRequest request) {
    log.info("Join request from user {}", request.getUser().getId());
}
```

### BusinessConnection (`@BotBusinessConnection`)

Inject the `BusinessConnection` object in handlers annotated with `@BotBusinessConnection`.

```java
@BotBusinessConnection
public void onConnection(BusinessConnection connection) {
    log.info("Business connection {} enabled: {}", connection.getId(), connection.getIsEnabled());
}
```

### BusinessMessagesDeleted (`@BotDeletedBusinessMessages`)

Inject `BusinessMessagesDeleted` in handlers annotated with `@BotDeletedBusinessMessages`.

```java
@BotDeletedBusinessMessages
public void onDeleted(BusinessMessagesDeleted deleted) {
    log.info("Deleted messages: {}", deleted.getMessageIds());
}
```

### @BotInlineQueryValue

Injects the inline query text string. Only available in `@BotInlineQuery` handlers.

```java
@BotInlineQuery
public void onInline(@BotInlineQueryValue String queryText, InlineQuery query) {
    log.info("Query text: {}", queryText);
}
```

### @BotChosenInlineResultId

Injects the chosen result ID string. Only available in `@BotChosenInlineResult` handlers.

```java
@BotChosenInlineResult
public void onChosen(@BotChosenInlineResultId String resultId) {
    log.info("Chosen result ID: {}", resultId);
}
```

### @BotShippingPayload

Injects the invoice payload string. Only available in `@BotShippingQuery` handlers.

```java
@BotShippingQuery
public void onShipping(@BotShippingPayload String payload, ShippingQuery query) {
    log.info("Invoice payload: {}", payload);
}
```

### @BotPreCheckoutPayload

Injects the invoice payload string. Only available in `@BotPreCheckoutQuery` handlers.

```java
@BotPreCheckoutQuery
public void onPreCheckout(@BotPreCheckoutPayload String payload, PreCheckoutQuery query) {
    log.info("Invoice payload: {}", payload);
}
```

---

## Locale (core-i18n)

`Locale` is resolved by `BotLocaleArgumentResolver`, which is auto-configured only when `core-i18n` is on the classpath.

```java
@BotCommand("/lang")
public String showLocale(Locale locale) {
    return "Your language: " + locale.getDisplayLanguage();
}
```

---

## Sharing Data Between Filters and Handlers

Both `BotRequest` and `BotResponse` carry a generic attribute map that is scoped to a single update processing cycle. This is the idiomatic way to pass computed data from a `BotFilter` to a handler without a shared Spring service.

```java
// In a BotFilter (runs before handlers):
@Component
@Order(BotFilterOrder.CONTEXT_SETTER + 10)
public class UserEnrichmentFilter implements BotFilter {

    private final UserRepository userRepository;

    public UserEnrichmentFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) {
        User telegramUser = request.getUser();
        AppUser appUser = userRepository.findByTelegramId(telegramUser.getId())
            .orElseGet(() -> userRepository.save(new AppUser(telegramUser)));
        request.setAttribute("appUser", appUser);
        chain.doFilter(request, response);
    }
}
```

```java
// Custom resolver that reads the attribute:
@Component
public class AppUserArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.getType() == AppUser.class;
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest request, BotResponse response) {
        return request.getAttribute("appUser");
    }
}
```

```java
// Handler receives the enriched object directly:
@BotCommand("/profile")
public String profile(AppUser appUser) {
    return "Welcome back, " + appUser.getDisplayName();
}
```

To remove an attribute, set it to `null`:

```java
request.setAttribute("appUser", null);
```

---

## Custom Argument Resolvers

For types not covered by the built-in resolvers, implement `BotArgumentResolver` and register it as a Spring `@Bean` (or `@Component`). The framework collects all resolver beans automatically.

```java
@Component
public class PaginationResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.getType() == PageRequest.class;
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest request, BotResponse response) {
        // Parse a page number stored in callback data, defaulting to page 0
        String data = request.getUpdate().getCallbackQuery() != null
            ? request.getUpdate().getCallbackQuery().getData()
            : "";
        int page = data.startsWith("page:") ? Integer.parseInt(data.substring(5)) : 0;
        return PageRequest.of(page, 10);
    }
}

// Use in any handler:
@BotDefaultCallbackQuery
public String onPage(PageRequest page, User user) {
    return "Showing page " + page.getPageNumber() + " for " + user.getFirstName();
}
```

---

## Parameter Order and Null Safety

- Parameters may be declared in **any order** — the framework matches them by type and annotation, not position.
- If a parameter cannot be resolved, an `IllegalStateException` is thrown with a descriptive message.
- Parameters are never injected as `null` by the built-in resolvers.

```java
@BotCommand("/profile")
public String showProfile(
    User user,
    Chat chat,
    @BotCommandValue String command,
    BotRequest request,
    TelegramClient client
) {
    return user.getFirstName() + " in " + chat.getId() + " via " + command;
}
```

---

Next: Learn about [chat state management](chat-state) for building multi-step flows.
