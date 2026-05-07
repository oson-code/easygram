---
id: api-reference
title: API Reference
description: Complete API reference for all Easygram annotations, SPI interfaces, reply types, configuration properties, and extension points for the Spring Boot Telegram bot framework.
keywords: [easygram api reference, telegram bot annotations java, BotController, BotCommand, PlainReply, BotFilter, spring boot telegram properties]
---

# API Reference

Complete reference for all easygram annotations, interfaces, model classes, and extension points.

---

## Table of Contents

- [Structural Annotations](#structural-annotations) — `@BotController`, `@BotControllerAdvice`, `@BotConfiguration`, `@BotMarkup`, `@BotOrder`
- [Handler Routing Annotations](#handler-routing-annotations) — `@BotCommand`, `@BotText`, `@BotTextPattern`, `@BotCallbackQuery`, `@BotDynamicCallbackQuery`, `@BotContact`, `@BotLocation`, `@BotReplyButton`, `@BotEditedMessage`, `@BotInlineQuery`, `@BotMyChatMember`, `@BotChatMemberUpdate`, `@BotDefaultHandler`, `@BotExceptionHandler`, … (+14 more update types)
- [Parameter Annotations](#parameter-annotations) — `@BotCommandValue`, `@BotTextValue`, `@BotCallbackQueryData`, `@BotCommandQueryParam`, `@BotInlineQueryValue`, `@BotChosenInlineResultId`, `@BotShippingPayload`, `@BotPreCheckoutPayload`
- [Response Annotations](#response-annotations) — `@BotReplyMarkup`, `@BotClearMarkup`
- [Chat State Annotations](#chat-state-annotations) — `@BotChatState`, `@BotForwardChatState`, `@BotClearChatState`
- [Return Types](#return-types)
- [Model Classes](#model-classes) — `BotRequest`, `BotResponse`, `BotMetadata`, `BotMarkupContext`, `BotDynamicCallbackData`
- [Dynamic Callbacks](#dynamic-callbacks) — `@BotDynamicCallbackQuery`, `BotDynamicCallbackData`, `BotDynamicCallbackQueryService`
- [Extension Interfaces](#extension-interfaces) — `BotFilter`, `BotArgumentResolver`, `BotReturnTypeHandler`, `BotHandlerInvocationFilter`, `BotChatStateService`, `BotUpdatePublisher`, `BotHandlerConditionContributor`, `BotStartTrigger`, `BotHandler`, `BotHandlerCondition`, `BotInlineQueryMatcher`, `BotReplyButtonMatcher`, `BotMarkupRegistry`, `MarkupAware`
- [Provider Interfaces](#provider-interfaces) — `BotTelegramClientProvider`, `BotOkHttpClientProvider`, `BotObjectMapperProvider`, `BotExecutorServiceProvider`, `BotTelegramUrlProvider`
- [Advanced SPI](#advanced-spi) — `BotMetaDataResolver`, `BotMetaDataDefaultResolver`, `BotMetaDataSpecResolver`, `BotHandlerInvocationContext`, `BotHandlerException`
- [i18n Services](#i18n-services-core-i18n) — `BotLocaleResolver`, `BotMessageSource`, `BotKeyboardFactory`
- [Observability](#observability-core-observability) — `BotHealthIndicator`, `BotInfoContributor`, `BotObservabilityFilter`
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

### @BotDynamicCallbackQuery

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`
**Since:** `0.0.4`

Routes inline keyboard callback queries by a **type** stored server-side via `BotDynamicCallbackQueryService`. Unlike `@BotCallbackQuery`, which matches the raw Telegram callback data string, this annotation routes by a type discriminator resolved from the stored `BotDynamicCallbackData` payload. This pattern is ideal when callback payloads exceed Telegram's 64-byte limit or when rich structured data is needed.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String[]` | `{}` | Type strings to match against `BotDynamicCallbackData.getType()`. Empty = any unmatched dynamic callback |

**Extra injectable parameters:** `BotDynamicCallbackData` (the resolved payload).

```java
// When building the keyboard:
String key = UUID.randomUUID().toString();
dynamicCallbackQueryService.store(key, BotDynamicCallbackData.builder()
    .type("product_buy")
    .put("id", productId)
    .put("currency", "USD")
    .build());
InlineKeyboardButton button = InlineKeyboardButton.builder()
    .text("Buy")
    .callbackData(key)
    .build();

// Or use BotKeyboardFactory which generates the key automatically:
InlineKeyboardButton btn = keyboardFactory.dynamicInlineButton(
    "btn.buy",
    BotDynamicCallbackData.builder().type("product_buy").put("id", productId).build(),
    request);

// When the callback fires:
@BotDynamicCallbackQuery("product_buy")
public String onBuy(BotDynamicCallbackData data) {
    Long id = (Long) data.getData().get("id");
    return "You selected product #" + id;
}

// Match multiple types:
@BotDynamicCallbackQuery({"product_buy", "product_view"})
public String onProduct(BotDynamicCallbackData data) {
    return "Action: " + data.getType() + " on product #" + data.getData().get("id");
}
```

---



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

### @BotEditedMessage

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes updates where a user edits a previously-sent text or media message. The original message is replaced in Telegram but the bot receives the edited copy.

**Injectable parameters:** `Message` (the edited message), `User`, `Chat`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotEditedMessage
public void onEdited(Message edited, User user) {
    log.info("User {} edited message {}: '{}'",
        user.getId(), edited.getMessageId(), edited.getText());
}
```

---

### @BotChannelPost

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes messages posted to a Telegram channel where the bot is an admin. `User` is not available (channel posts have no sender user); use `Chat` for the channel details.

**Injectable parameters:** `Message` (the channel post), `Chat`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotChannelPost
public void onChannelPost(Message post, Chat channel) {
    log.info("New post in channel @{}: '{}'",
        channel.getUserName(), post.getText());
}
```

---

### @BotEditedChannelPost

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes updates when a previously-published channel post is edited.

**Injectable parameters:** `Message` (the edited channel post), `Chat`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotEditedChannelPost
public void onEditedChannelPost(Message edited, Chat channel) {
    log.info("Channel @{} edited post {}", channel.getUserName(), edited.getMessageId());
}
```

---

### @BotInlineQuery

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes inline queries — triggered when a user types `@YourBot …` in any chat. The optional `value` attribute filters by query text; omit it to match all inline queries.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String[]` | `{}` (match all) | Match inline query text against these strings; empty = any query |

**Injectable parameters:** `InlineQuery`, `@BotInlineQueryValue String` (query text), `User`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

**Without `core-i18n`** — values are compared as literal strings (exact match):

```java
// Match any inline query
@BotInlineQuery
public void onAnyInline(InlineQuery query, @BotInlineQueryValue String text) {
    List<InlineQueryResult> results = searchProducts(text);
    bot.execute(AnswerInlineQuery.builder()
        .inlineQueryId(query.getId())
        .results(results)
        .build());
}

// Match only when query text is exactly "search"
@BotInlineQuery("search")
public void onSearchInline(@BotInlineQueryValue String text, InlineQuery query) {
    // triggered only when user types "@YourBot search"
}
```

**With `core-i18n`** — values are treated as message-bundle keys resolved per user locale.
A single annotation covers all languages automatically:

```java
// messages/bot_en.properties: inline.search=search
// messages/bot_ru.properties: inline.search=поиск
@BotInlineQuery("inline.search")
public void onSearchInline(InlineQuery query, @BotInlineQueryValue String text) {
    // matches "@YourBot search" for English users
    // and "@YourBot поиск" for Russian users
}
```

The matching strategy is provided by `BotInlineQueryMatcher` (default: exact-text; with `core-i18n`: locale-aware). Override the bean to implement custom matching logic.

---

### @BotChosenInlineResult

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes the feedback event sent by Telegram when a user selects one of the inline results your bot returned. Requires **inline feedback** to be enabled in BotFather.

**Injectable parameters:** `ChosenInlineResult`, `@BotChosenInlineResultId String` (the chosen result's ID), `User`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotChosenInlineResult
public void onChosen(
        ChosenInlineResult result,
        @BotChosenInlineResultId String resultId,
        User user) {
    analytics.track(user.getId(), "inline_chosen", resultId);
}
```

---

### @BotShippingQuery

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes shipping address queries sent by Telegram during the payment flow when the invoice has `is_flexible = true`. The bot must reply with `AnswerShippingQuery` to confirm or reject shipping options.

**Injectable parameters:** `ShippingQuery`, `@BotShippingPayload String` (invoice payload), `User`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotShippingQuery
public void onShipping(
        ShippingQuery query,
        @BotShippingPayload String payload,
        TelegramClient client) throws TelegramApiException {
    List<ShippingOption> options = shippingService.getOptions(query.getShippingAddress());
    client.execute(AnswerShippingQuery.builder()
        .shippingQueryId(query.getId())
        .ok(true)
        .shippingOptions(options)
        .build());
}
```

---

### @BotPreCheckoutQuery

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes the pre-checkout event fired by Telegram just before a payment is completed. The bot **must** respond with `AnswerPreCheckoutQuery` within 10 seconds to confirm or cancel the transaction.

**Injectable parameters:** `PreCheckoutQuery`, `@BotPreCheckoutPayload String` (invoice payload), `User`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotPreCheckoutQuery
public void onPreCheckout(
        PreCheckoutQuery query,
        @BotPreCheckoutPayload String payload,
        TelegramClient client) throws TelegramApiException {
    boolean valid = orderService.validate(payload, query.getTotalAmount());
    client.execute(AnswerPreCheckoutQuery.builder()
        .preCheckoutQueryId(query.getId())
        .ok(valid)
        .errorMessage(valid ? null : "Order is no longer available.")
        .build());
}
```

---

### @BotPoll

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes poll state-change updates. Telegram sends these when a non-anonymous poll is stopped or its vote counts change.

**Injectable parameters:** `Poll`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotPoll
public void onPollUpdate(Poll poll) {
    if (poll.getIsClosed()) {
        int winner = findWinner(poll.getOptions());
        log.info("Poll '{}' closed. Winner option index: {}", poll.getQuestion(), winner);
    }
}
```

---

### @BotPollAnswer

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes individual vote events — triggered when a user votes (or retracts their vote) in a non-anonymous poll created by the bot.

**Injectable parameters:** `PollAnswer`, `User`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotPollAnswer
public void onVote(PollAnswer answer, User user) {
    List<Integer> chosen = answer.getOptionIds();
    log.info("User {} voted: options {}", user.getId(), chosen);
}
```

---

### @BotMyChatMember

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes updates about the **bot's own** membership status in a chat — e.g. the bot was added to a group, promoted to admin, or removed.

**Injectable parameters:** `ChatMemberUpdated`, `User` (the user who made the change), `Chat`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotMyChatMember
public void onBotMemberChange(ChatMemberUpdated update, Chat chat) {
    ChatMember newStatus = update.getNewChatMember();
    if (newStatus instanceof ChatMemberMember) {
        log.info("Bot was added to chat {}", chat.getId());
    } else if (newStatus instanceof ChatMemberLeft) {
        log.info("Bot was removed from chat {}", chat.getId());
    }
}
```

---

### @BotChatMemberUpdate

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes updates about **another user's** membership status in a chat managed by the bot. Requires the bot to have admin rights and the `chat_member` update type to be subscribed. Named `BotChatMemberUpdate` to avoid a naming clash with the Telegram API's `ChatMember` class.

**Injectable parameters:** `ChatMemberUpdated`, `User` (the user who made the change), `Chat`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotChatMemberUpdate
public void onUserMemberChange(ChatMemberUpdated event, Chat chat) {
    User affected = event.getNewChatMember().getUser();
    ChatMember newRole = event.getNewChatMember();
    log.info("User {} status changed in chat {}: {}",
        affected.getId(), chat.getId(), newRole.getStatus());
}
```

---

### @BotChatJoinRequest

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes join requests when a user asks to join a channel or group that requires admin approval. The bot can approve or decline via `ApproveChatJoinRequest` / `DeclineChatJoinRequest`.

**Injectable parameters:** `ChatJoinRequest`, `User` (the requester), `Chat`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotChatJoinRequest
public void onJoinRequest(
        ChatJoinRequest request,
        User requester,
        TelegramClient client) throws TelegramApiException {
    if (membershipService.isAllowed(requester.getId())) {
        client.execute(ApproveChatJoinRequest.builder()
            .chatId(request.getChat().getId())
            .userId(requester.getId())
            .build());
    } else {
        client.execute(DeclineChatJoinRequest.builder()
            .chatId(request.getChat().getId())
            .userId(requester.getId())
            .build());
    }
}
```

---

### @BotBusinessConnection

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes business connection updates — fired when a Telegram Business account connects or disconnects from your bot.

**Injectable parameters:** `BusinessConnection`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotBusinessConnection
public void onBusinessConnection(BusinessConnection connection) {
    if (connection.getIsEnabled()) {
        log.info("Business account {} connected", connection.getId());
        businessService.activate(connection.getUser().getId());
    } else {
        log.info("Business account {} disconnected", connection.getId());
        businessService.deactivate(connection.getUser().getId());
    }
}
```

---

### @BotBusinessMessage

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes messages sent by customers to a connected Telegram Business account. The bot receives these to provide automated responses on behalf of the business.

**Injectable parameters:** `Message`, `User`, `Chat`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotBusinessMessage
public void onBusinessMessage(Message message, User customer) {
    String reply = autoReplyService.generateReply(message.getText());
    log.info("Business message from {}: '{}' → auto-reply: '{}'",
        customer.getId(), message.getText(), reply);
}
```

---

### @BotEditedBusinessMessage

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes edit events for messages that were previously received via a connected business account.

**Injectable parameters:** `Message` (the edited message), `User`, `Chat`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotEditedBusinessMessage
public void onEditedBusinessMessage(Message edited) {
    log.info("Business message {} was edited: '{}'",
        edited.getMessageId(), edited.getText());
}
```

---

### @BotDeletedBusinessMessages

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes bulk-deletion events for messages in a connected business account chat.

**Injectable parameters:** `BusinessMessagesDeleted`, `Update`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotDeletedBusinessMessages
public void onDeletedBusinessMessages(BusinessMessagesDeleted deleted) {
    log.info("Business chat {}: {} messages deleted",
        deleted.getChat().getId(),
        deleted.getMessageIds().size());
    auditService.recordDeletion(deleted.getBusinessConnectionId(), deleted.getMessageIds());
}
```

---

### @BotPaidMediaPurchased

**Package:** `uz.osoncode.easygram.core.bind.annotation`
**Target:** `METHOD`

Routes the event fired when a user completes a paid media purchase. Inject `Update` and call `update.getPaidMediaPurchased()` to access the purchase details.

**Injectable parameters:** `Update`, `User`, `BotRequest`, `BotResponse`, `TelegramClient`.

```java
@BotPaidMediaPurchased
public void onPaidMediaPurchased(Update update, User buyer) {
    var purchase = update.getPaidMediaPurchased();
    log.info("User {} purchased paid media. Payload: '{}'",
        buyer.getId(), purchase.getPaidMediaPayload());
    orderService.fulfil(buyer.getId(), purchase.getPaidMediaPayload());
}
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

### Update-Type Parameter Annotations (0.0.2)

All annotations below are in package `uz.osoncode.easygram.core.bind.annotation`, target `PARAMETER`.

| Annotation | Injects | Available in |
|---|---|---|
| `@BotInlineQueryValue` | `String` — inline query text | `@BotInlineQuery` handlers |
| `@BotChosenInlineResultId` | `String` — chosen result ID | `@BotChosenInlineResult` handlers |
| `@BotShippingPayload` | `String` — invoice payload | `@BotShippingQuery` handlers |
| `@BotPreCheckoutPayload` | `String` — invoice payload | `@BotPreCheckoutQuery` handlers |

```java
@BotInlineQuery
public void onInline(@BotInlineQueryValue String queryText, InlineQuery query) { ... }

@BotChosenInlineResult
public void onChosen(@BotChosenInlineResultId String resultId) { ... }

@BotShippingQuery
public void onShipping(@BotShippingPayload String payload, ShippingQuery query) { ... }

@BotPreCheckoutQuery
public void onPreCheckout(@BotPreCheckoutPayload String payload, PreCheckoutQuery query) { ... }
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
PlainReply.of("Updated!").withEditMessage()      // edit originating callback-query message (since 0.0.2)

// Answer callback query — reply text used as popup notification (since 0.0.5)
PlainReply.of("Confirmed!").asAnswerCallbackQuery()              // toast popup
PlainReply.of("Deleted.").asAnswerCallbackQuery().withCallbackAlert()  // alert dialog
PlainReply.of("Open page").asAnswerCallbackQuery().withCallbackUrl("https://example.com")
PlainReply.of("OK").asAnswerCallbackQuery().withCallbackCacheTime(10)

// Builder
PlainReply.builder()
    .text("Choose:")
    .markupId("main_menu")
    .editMessage(true)            // edit instead of send (since 0.0.2)
    .answerCallbackQuery(true)    // send AnswerCallbackQuery (since 0.0.5)
    .callbackAlert(true)          // showAlert=true (since 0.0.5)
    .callbackUrl("https://...")   // optional URL (since 0.0.5)
    .callbackCacheTime(10)        // cache seconds (since 0.0.5)
    .build()
```

### PlainTextTemplate — Fluent API

Uses `#{index}` positional tokens (0-based) for value substitution. Same markup methods as `PlainReply`.
Supports the same AnswerCallbackQuery API — the **resolved template text** is used as the popup notification.

```java
PlainTextTemplate.of("Hello, #{0}! You have #{1} messages.", user.getFirstName(), count)
PlainTextTemplate.of("Order ##{0} ready.", orderId).withMarkup("order_kb")
PlainTextTemplate.of("Updated #{0}!").withEditMessage()  // edit callback-query message (since 0.0.2)

// Answer callback query — resolved template text used as popup (since 0.0.5)
PlainTextTemplate.of("Step #{0} complete!", step).asAnswerCallbackQuery()              // toast
PlainTextTemplate.of("Deleted #{0}.", item).asAnswerCallbackQuery().withCallbackAlert() // alert dialog
PlainTextTemplate.of("Done #{0}.").asAnswerCallbackQuery().withCallbackUrl("https://example.com")
PlainTextTemplate.of("OK #{0}.").asAnswerCallbackQuery().withCallbackCacheTime(10)

// Builder
PlainTextTemplate.builder()
    .template("Hello, #{0}! Order ##{1} is ready.")
    .args(user.getFirstName(), orderId)
    .markupId("order_kb")
    .editMessage(true)            // edit instead of send (since 0.0.2)
    .answerCallbackQuery(true)    // send AnswerCallbackQuery (since 0.0.5)
    .callbackAlert(true)          // showAlert=true (since 0.0.5)
    .callbackUrl("https://...")   // optional URL (since 0.0.5)
    .callbackCacheTime(10)        // cache seconds (since 0.0.5)
    .build()
```

### LocalizedReply — Fluent API *(core-i18n)*

Key is resolved via `BotMessageSource` using the request locale.

```java
LocalizedReply.of("welcome.message", user.getFirstName())
LocalizedReply.of("choose.option").withMarkup("main_menu")
LocalizedReply.of("confirm.prompt").withMarkup("confirm_kb", Map.of("id", itemId))
LocalizedReply.of("updated.text").withEditMessage()  // edit callback-query message (since 0.0.2)

// Answer callback query — resolved i18n message used as popup text (since 0.0.5)
LocalizedReply.of("action.confirmed").asAnswerCallbackQuery()
LocalizedReply.of("item.deleted").asAnswerCallbackQuery().withCallbackAlert()
LocalizedReply.of("opening.page").asAnswerCallbackQuery().withCallbackUrl("https://example.com")

// Builder
LocalizedReply.builder()
    .key("welcome.message")
    .args(user.getFirstName())
    .markupId("main_menu")
    .editMessage(true)            // since 0.0.2
    .answerCallbackQuery(true)    // since 0.0.5
    .callbackAlert(true)          // showAlert=true (since 0.0.5)
    .callbackUrl("https://...")   // optional URL (since 0.0.5)
    .callbackCacheTime(10)        // cache seconds (since 0.0.5)
    .build()
```

### LocalizedTemplate — Fluent API *(core-i18n)*

Supports mixed `${messageKey}` bundle lookups and `#{index}` positional arg substitution.
Supports the same AnswerCallbackQuery API — the **fully resolved template string** is used as the popup notification.

```java
LocalizedTemplate.of("${welcome.title}\n\nHello, #{0}!", user.getFirstName())
LocalizedTemplate.of("${stats.header}\n\nMessages: #{0}", count).withMarkup("stats_menu")
LocalizedTemplate.of("${updated}").withEditMessage()  // edit callback-query message (since 0.0.2)

// Answer callback query — fully resolved template text used as popup (since 0.0.5)
LocalizedTemplate.of("${action.confirmed}").asAnswerCallbackQuery()              // toast
LocalizedTemplate.of("${item.deleted}").asAnswerCallbackQuery().withCallbackAlert() // alert dialog
LocalizedTemplate.of("${opening.page}").asAnswerCallbackQuery().withCallbackUrl("https://example.com")

// Builder
LocalizedTemplate.builder()
    .template("${stats.header}\n\nMessages: #{0}\nCommands: #{1}")
    .args(messages, commands)
    .markupId("stats_menu")
    .editMessage(true)            // since 0.0.2
    .answerCallbackQuery(true)    // send AnswerCallbackQuery (since 0.0.5)
    .callbackAlert(true)          // showAlert=true (since 0.0.5)
    .callbackUrl("https://...")   // optional URL (since 0.0.5)
    .callbackCacheTime(10)        // cache seconds (since 0.0.5)
    .build()
```

---

## Dynamic Callbacks

*(Since 0.0.4)* The dynamic callback system lets you store rich structured data server-side and route inline keyboard callbacks by a **type** discriminator, bypassing Telegram's 64-byte `callbackData` limit.

---

### BotDynamicCallbackData

**Package:** `uz.osoncode.easygram.core.dynamiccallback`

Immutable record that stores a type discriminator and an arbitrary key-value data map. Instances are stored via `BotDynamicCallbackQueryService` and retrieved by the framework when a matching callback query arrives.

```java
// Build a payload
BotDynamicCallbackData payload = BotDynamicCallbackData.builder()
    .type("product_buy")
    .put("id", 42L)
    .put("currency", "USD")
    .build();

// Access in a handler
@BotDynamicCallbackQuery("product_buy")
public String onBuy(BotDynamicCallbackData data) {
    String type = data.getType();            // "product_buy"
    Long id = (Long) data.getData().get("id"); // 42
    return "Buying product #" + id;
}
```

**API:**

```java
String getType()               // type discriminator; never null
Map<String,Object> getData()   // unmodifiable data map; never null

// Record accessors
String type()
Map<String,Object> data()

static final String ATTRIBUTE_KEY = "DYNAMIC_CALLBACK_DATA"  // BotRequest attribute key

static Builder builder()
```

**Builder methods:**

```java
Builder type(String type)                    // required — type discriminator
Builder data(Map<String,Object> data)        // replace entire data map
Builder put(String key, Object value)        // add a single entry
BotDynamicCallbackData build()
```

---

### BotDynamicCallbackQueryService

**Package:** `uz.osoncode.easygram.core.dynamiccallback`

SPI for storing and retrieving `BotDynamicCallbackData` payloads. The default in-memory implementation is auto-configured. Replace with a `@Bean` backed by Redis, JDBC, etc. for persistence across restarts.

```java
public interface BotDynamicCallbackQueryService {

    /** Looks up the payload stored under callbackData (the UUID key). Returns null if not found. */
    BotDynamicCallbackData resolve(String callbackData);

    /** Stores payload under callbackData, replacing any previous mapping. */
    void store(String callbackData, BotDynamicCallbackData payload);

    /** Removes the payload for callbackData. No-op if not found. */
    void remove(String callbackData);
}
```

```java
// Custom Redis-backed implementation
@Bean
public BotDynamicCallbackQueryService redisDynamicCallbackService(StringRedisTemplate redis) {
    return new RedisBotDynamicCallbackQueryService(redis);
}
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
    void setState(Long chatId, String state); // throws IllegalArgumentException if state is null (since 0.0.2)
    void clearState(Long chatId);             // Added in 0.0.2 — removes state without null check

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

**Package:** `uz.osoncode.easygram.messaging`

SPI for publishing raw Telegram updates to a message broker. Implement and register as a `@Bean` to integrate with any broker. Ready-made implementations are provided by `messaging-kafka` and `messaging-rabbit`.

```java
public interface BotUpdatePublisher {
    void publish(Update update);
}
```

```java
@Component
public class MyCustomPublisher implements BotUpdatePublisher {
    @Override
    public void publish(Update update) {
        // send to your broker
    }
}
```

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

### BotHandler

**Package:** `uz.osoncode.easygram.core.handler`

Low-level update processing strategy. The framework auto-discovers and uses `@BotController` methods via built-in `BotHandler` implementations. Only implement this interface directly for advanced custom routing that cannot be expressed with handler annotations.

```java
public interface BotHandler extends Comparable<BotHandler> {

    /** Lower value = evaluated first. Default: Integer.MAX_VALUE. */
    default int getOrder() { return Integer.MAX_VALUE; }

    /** Return true if this handler can process the given request. */
    boolean supports(BotRequest botRequest);

    /** Process the update and populate the response. */
    void handle(BotRequest botRequest, BotResponse botResponse)
            throws InvocationTargetException, IllegalAccessException;

    /** Human-readable description for logging. */
    String info();
}
```

---

### BotHandlerCondition

**Package:** `uz.osoncode.easygram.core.handler`

Functional predicate that decides whether a handler should process a given request. The framework provides two built-in conditions: `BotMetaDataCondition` (annotation matching) and `BotChatStateCondition` (state guard). Custom conditions are contributed via `BotHandlerConditionContributor`.

```java
@FunctionalInterface
public interface BotHandlerCondition {
    boolean matches(BotRequest botRequest);
}
```

```java
// Ad-hoc usage — requires admin
BotHandlerCondition adminOnly =
    request -> adminService.isAdmin(request.getUser().getId());
```

---

### BotInlineQueryMatcher

**Package:** `uz.osoncode.easygram.core.handler.inlinequery`

Strategy for matching `@BotInlineQuery` annotation values against an incoming inline-query update. The default implementation does exact-text comparison. With `core-i18n` on the classpath, a locale-aware implementation is registered that treats values as message-bundle keys.

```java
@FunctionalInterface
public interface BotInlineQueryMatcher {

    /**
     * Returns true if the inline query matches at least one annotation value.
     * An empty values array means "match all" — this method is not called in that case.
     */
    boolean matches(String[] values, BotRequest request);
}
```

```java
// Custom case-insensitive matcher
@Bean
public BotInlineQueryMatcher caseInsensitiveMatcher() {
    return (values, request) -> {
        String query = request.getUpdate().getInlineQuery().getQuery();
        return Arrays.stream(values).anyMatch(v -> query.equalsIgnoreCase(v));
    };
}
```

---

### BotReplyButtonMatcher

**Package:** `uz.osoncode.easygram.core.handler.message.replybutton`

Strategy for matching `@BotReplyButton` annotation values against an incoming message text. The default implementation does exact-text comparison. With `core-i18n`, values are resolved as message-bundle keys per user locale.

```java
@FunctionalInterface
public interface BotReplyButtonMatcher {

    /**
     * Returns true if the incoming message text matches one of the annotation values.
     */
    boolean matches(String[] values, BotRequest request);
}
```

```java
@Bean
public BotReplyButtonMatcher trimmedMatcher() {
    return (values, request) -> {
        String text = request.getUpdate().getMessage().getText().trim();
        return Arrays.asList(values).contains(text);
    };
}
```

---

### BotMarkupRegistry

**Package:** `uz.osoncode.easygram.core.markup`

Registry that maps markup IDs (and chat-state names) to keyboard factory functions. Populated at startup by scanning `@BotMarkup` methods. The default implementation is `InMemoryBotMarkupRegistry`. Override with a custom `@Bean` for distributed or database-backed registries.

```java
public interface BotMarkupRegistry {

    /** Register a factory under an ID. */
    void register(String id, Function<BotRequest, ReplyKeyboard> factory);

    /** Resolve the keyboard for id using the given request context (may be null for static markups). */
    ReplyKeyboard resolve(String id, BotRequest request);

    /** Returns true if an ID is registered. */
    boolean contains(String id);

    /** Register as the default keyboard for a chat state (called by BotMarkupLoader). */
    default void registerForState(String state, Function<BotRequest, ReplyKeyboard> factory) {}

    /** Resolve the keyboard registered as default for the given chat state. */
    default ReplyKeyboard resolveByState(String state, BotRequest request) { return null; }
}
```

---

### MarkupAware

**Package:** `uz.osoncode.easygram.core.markup`

Marker interface implemented by all reply types that support carrying markup: `PlainReply`, `PlainTextTemplate`, `LocalizedReply`, and `LocalizedTemplate`. Enables the framework to apply markup to any return type uniformly.

**Markup resolution precedence:**
1. `isRemoveMarkup()` — sends `ReplyKeyboardRemove`; overrides everything (triggered by `@BotClearMarkup`)
2. `getKeyboard()` non-null — attached directly, no registry lookup
3. `getMarkupId()` non-null — registry lookup, optionally with `getMarkupParams()` forwarded via `BotMarkupContext`
4. `@BotReplyMarkup` annotation — fallback; only applied when neither keyboard nor markup ID is set

```java
public interface MarkupAware {
    String getMarkupId();
    Map<String, Object> getMarkupParams();
    ReplyKeyboard getKeyboard();
    boolean isRemoveMarkup();
    boolean isEditMessage();          // true = edit callback-query message (since 0.0.2)

    MarkupAware withMarkup(String markupId);
    MarkupAware withMarkup(String markupId, Map<String, Object> params);
    MarkupAware withKeyboard(ReplyKeyboard keyboard);
    MarkupAware removeMarkup();
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

// Dynamic inline buttons — stores payload via BotDynamicCallbackQueryService (since 0.0.4)
InlineKeyboardButton dynBtn = factory.dynamicInlineButton(
    "btn.buy",
    BotDynamicCallbackData.builder().type("product_buy").put("id", productId).build(),
    request);

// Or with explicit Locale
InlineKeyboardButton dynBtn = factory.dynamicInlineButton(
    "btn.buy",
    BotDynamicCallbackData.builder().type("product_buy").put("id", productId).build(),
    locale);
```

#### InlineKeyboardBuilder — dynamic rows

```java
// Dynamic row — stores payloads, generates UUID keys automatically (since 0.0.4)
keyboardFactory.inline(request)
    .dynamicRow(
        "btn.buy",   BotDynamicCallbackData.builder().type("buy").put("id", 1L).build(),
        "btn.view",  BotDynamicCallbackData.builder().type("view").put("id", 1L).build()
    )
    .build();
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

## Provider Interfaces

All provider interfaces are in `uz.osoncode.easygram.core.provider` and are `@FunctionalInterface`. Register a Spring `@Bean` of any provider type to replace the default implementation.

---

### BotTelegramClientProvider

Supplies the `TelegramClient` used to send API replies. Override to use a custom HTTP client, proxy, or test stub.

```java
@FunctionalInterface
public interface BotTelegramClientProvider {
    /** Create or return a TelegramClient for the given bot token. */
    TelegramClient provide(String botToken);
}
```

```java
@Bean
public BotTelegramClientProvider botTelegramClientProvider() {
    return botToken -> new OkHttpTelegramClient(
            customObjectMapper(),
            customHttpClient(),
            botToken,
            TelegramUrl.DEFAULT_URL);
}
```

---

### BotOkHttpClientProvider

Supplies the `OkHttpClient` for all outbound Telegram API calls. Override to configure timeouts, interceptors, TLS, or a proxy.

```java
@FunctionalInterface
public interface BotOkHttpClientProvider {
    OkHttpClient provide();
}
```

```java
@Bean
public BotOkHttpClientProvider botOkHttpClientProvider() {
    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(new LoggingInterceptor())
            .build();
    return () -> client;
}
```

---

### BotObjectMapperProvider

Supplies the Jackson `ObjectMapper` for Telegram API payload serialisation. Override to register custom modules or configure naming strategy.

```java
@FunctionalInterface
public interface BotObjectMapperProvider {
    ObjectMapper provide();
}
```

```java
@Bean
public BotObjectMapperProvider botObjectMapperProvider() {
    ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return () -> mapper;
}
```

---

### BotExecutorServiceProvider

Supplies the `ExecutorService` for processing incoming updates. Override to control thread-pool size, naming, or rejection policy.

```java
@FunctionalInterface
public interface BotExecutorServiceProvider {
    ExecutorService provide();
}
```

```java
@Bean
public BotExecutorServiceProvider botExecutorServiceProvider() {
    ExecutorService executor = Executors.newFixedThreadPool(4,
            new ThreadFactoryBuilder().setNameFormat("bot-worker-%d").build());
    return () -> executor;
}
```

> **Important:** always return the same `ExecutorService` instance on every call — the framework calls `provide()` once at startup and shuts it down when the context closes.

---

### BotTelegramUrlProvider

Supplies the Telegram API base URL. Override to point the bot at a local Bot API server.

```java
@FunctionalInterface
public interface BotTelegramUrlProvider {
    TelegramUrl provide();
}
```

```java
@Bean
public BotTelegramUrlProvider botTelegramUrlProvider() {
    return () -> new TelegramUrl("https://my-local-bot-api.example.com/");
}
```

When no custom bean is present, defaults to `TelegramUrl.DEFAULT_URL`.

---

## Advanced SPI

These types are used internally or by advanced framework extensions.

---

### BotMetaDataResolver

**Package:** `uz.osoncode.easygram.core.handler.metadataresolver`

Generic strategy for matching a specific handler annotation against an incoming `BotRequest`. Each implementation binds to one annotation type `T` and encodes the matching logic.

```java
public interface BotMetaDataResolver<T extends Annotation> {
    /** Returns the annotation type this resolver handles. */
    Class<T> getAnnotationType();

    /** Returns true if this annotation's handler should handle the given request. */
    boolean support(BotRequest botRequest, T annotation);
}
```

Two sub-interfaces partition resolvers into two registries:

| Interface | Purpose |
|---|---|
| `BotMetaDataSpecResolver<T>` | Specific/non-default resolvers — used with explicit match criteria (e.g. `@BotCommand("/start")`) |
| `BotMetaDataDefaultResolver<T>` | Default/fallback resolvers — consulted only when no specific resolver matches (e.g. `@BotDefaultCommand`) |

Implement and register as a `@Bean` to support custom routing annotations.

---

### BotHandlerInvocationContext

**Package:** `uz.osoncode.easygram.core.handler.invocation`

Mutable context passed through the `BotHandlerInvocationFilter` chain for a single handler method execution.

```java
BotRequest getRequest()    // the current bot request
BotResponse getResponse()  // the mutable response
Method getMethod()         // the handler method being invoked
Object getBean()           // the controller bean owning the method
Object getReturnValue()    // value produced by the method (null before MethodInvocationFilter sets it)
void setReturnValue(Object value)  // allows filters to transform the return value
```

---

### BotHandlerException

**Package:** `uz.osoncode.easygram.core.exception`

Unchecked exception thrown when a bot handler method cannot be invoked via reflection. Wraps the underlying cause so it propagates through the dispatch chain without checked exception declarations.

```java
public class BotHandlerException extends RuntimeException {
    public BotHandlerException(String message, Throwable cause) { ... }
}
```

---

### BotConfigurer

**Package:** `uz.osoncode.easygram.core.bot`

Immutable record that carries shared infrastructure objects available to framework components at startup. Inject this record to inspect the active transport type or access the shared `ObjectMapper`.

```java
public record BotConfigurer(
    ObjectMapper objectMapper,   // shared Jackson ObjectMapper
    BotTransportType transportType  // active transport (LONG_POLLING, WEBHOOK, etc.)
) {}
```

```java
@Component
@RequiredArgsConstructor
public class MyComponent {
    private final BotConfigurer botConfigurer;

    public void init() {
        if (botConfigurer.transportType() == BotTransportType.LONG_POLLING) {
            // long-polling specific setup
        }
    }
}
```

---

### BotTransportType

**Package:** `uz.osoncode.easygram.core.bot`

Enumerates the supported update-delivery transports. Set via `easygram.update.transport`.

| Constant | Description |
|---|---|
| `LONG_POLLING` | Bot repeatedly calls `getUpdates` (default — omit `update` block entirely) |
| `WEBHOOK` | Telegram pushes updates to an HTTPS endpoint |

---



## Observability *(core-observability)*

Auto-configured when `core-observability` is on the classpath via `BotActuatorAutoConfiguration` and `ObservabilityAutoConfiguration`.

| Component | Description |
|---|---|
| `BotHealthIndicator` | Spring Boot actuator health check — reports `UP`/`DOWN` based on bot connectivity |
| `BotInfoContributor` | Actuator `/info` endpoint — exposes bot username, id, and active transport type |
| `BotObservabilityFilter` | `BotFilter` at `BotFilterOrder.OBSERVATION` — wraps every update in a Micrometer observation span; emits `easygram.update` metric |

All three beans are `@ConditionalOnMissingBean` — replace any with a custom `@Bean`.

---

## Filter Order Constants

**Class:** `BotFilterOrder` (`uz.osoncode.easygram.core.filter`)

| Constant | Value | Built-in filter |
|---|---|---|
| `MDC_CONTEXT` | `Integer.MIN_VALUE` | Sets `bot.update.id` and `bot.transport` MDC keys |
| `CONTEXT_SETTER` | `MIN_VALUE + 1` | Sets `User` and `Chat` on `BotRequest`; enriches MDC |
| `OBSERVATION` | `MIN_VALUE + 2` | Micrometer observation span (core-observability) |
| `API_SENDER` | `MIN_VALUE + 3` | Executes queued `BotApiMethod` calls via `TelegramClient` |
| `PUBLISHING` | `MIN_VALUE + 1000` | Forwards update to message broker (messaging-api) |
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
easygram:
  token: YOUR_BOT_TOKEN # From @BotFather — required for all transports
```

### Transport

```yaml
easygram:
  update:
    transport: LONG_POLLING   # Default — omit block entirely for long-polling
                              # Options: LONG_POLLING | WEBHOOK
```

### Long-Polling

Long-polling has no additional configurable properties. The polling behaviour (timeout, batch size, back-off) is handled internally by the telegrambots library.

### Webhook

```yaml
easygram:
  update:
    transport: WEBHOOK
    webhook:
      url: https://my-bot.example.com/webhook # Required — publicly reachable HTTPS URL
      path: /webhook                           # Local handler path (default: /webhook)
      secret-token: ${WEBHOOK_SECRET}          # Recommended — validates Telegram requests
      max-connections: 40
      drop-pending-updates: false
      unregister-on-shutdown: false
```

### Broker Publishing

```yaml
easygram:
  messaging:
    type: PRODUCER            # Required: PRODUCER or CONSUMER
    forward-only: false       # true = publish to broker only, skip local handler dispatch
    producer:
      type: KAFKA             # KAFKA | RABBIT
    kafka:
      topic: easygram-updates
      create-if-absent: true
      partitions: 1
      replication-factor: 1
    rabbit:
      exchange: easygram-exchange
      routing-key: easygram.updates
      queue: easygram-updates
      create-if-absent: true
```

### Broker Consumer

```yaml
easygram:
  messaging:
    type: CONSUMER            # Required: PRODUCER or CONSUMER
    consumer:
      type: KAFKA             # KAFKA | RABBIT
    kafka:
      topic: easygram-updates
      group-id: my-bot-consumer   # consumer group ID (default: easygram-bot)
    rabbit:
      exchange: easygram-exchange
      queue: easygram-updates
```

### i18n *(core-i18n)*

```yaml
easygram:
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
