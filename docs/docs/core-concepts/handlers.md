---
id: handlers
title: Handler Annotations
description: Reference for all Easygram handler annotations — @BotCommand, @BotText, @BotTextPattern, @BotCallbackQuery, @BotDefaultHandler, @BotController, and @BotExceptionHandler — with examples.
keywords: [BotCommand, BotText, BotController, telegram bot handler java, spring boot telegram annotations, @BotCallbackQuery, @BotDefaultHandler]
---

# Handler Annotations

Handlers are methods in `@BotController` classes that respond to Telegram updates. Easygram provides annotations for every update type.

## All Handler Annotations

| Annotation | Matches | Example |
|---|---|---|
| `@BotCommand` | Bot command messages | `@BotCommand("/start")` |
| `@BotDefaultCommand` | Any command not matched | `@BotDefaultCommand` |
| `@BotText` | Exact plain-text message | `@BotText("hello")` |
| `@BotTextPattern` | Regex-matched text message | `@BotTextPattern("^\\d{10}$")` |
| `@BotTextDefault` | Any text not matched | `@BotTextDefault` |
| `@BotCallbackQuery` | Callback query by data | `@BotCallbackQuery("btn_ok")` |
| `@BotDefaultCallbackQuery` | Any callback not matched | `@BotDefaultCallbackQuery` |
| `@BotContact` | Contact-sharing message | `@BotContact` |
| `@BotLocation` | Location-sharing message | `@BotLocation` |
| `@BotReplyButton` | Reply keyboard button press | `@BotReplyButton(" Confirm")` |
| `@BotEditedMessage` | Edited text/media messages | `@BotEditedMessage` |
| `@BotChannelPost` | Channel publication messages | `@BotChannelPost` |
| `@BotEditedChannelPost` | Edited channel posts | `@BotEditedChannelPost` |
| `@BotInlineQuery` | Inline queries | `@BotInlineQuery("search")` |
| `@BotChosenInlineResult` | Chosen inline results | `@BotChosenInlineResult` |
| `@BotShippingQuery` | Shipping address queries | `@BotShippingQuery` |
| `@BotPreCheckoutQuery` | Pre-checkout events | `@BotPreCheckoutQuery` |
| `@BotPoll` | Poll state changes | `@BotPoll` |
| `@BotPollAnswer` | User-voted-in-poll events | `@BotPollAnswer` |
| `@BotMyChatMember` | Bot's own member status changes | `@BotMyChatMember` |
| `@BotChatMember` | User member status changes | `@BotChatMember` |
| `@BotChatJoinRequest` | User join requests to a chat | `@BotChatJoinRequest` |
| `@BotBusinessConnection` | Business account connections | `@BotBusinessConnection` |
| `@BotBusinessMessage` | Messages via business account | `@BotBusinessMessage` |
| `@BotEditedBusinessMessage` | Edited messages via business account | `@BotEditedBusinessMessage` |
| `@BotDeletedBusinessMessages` | Business messages-deleted events | `@BotDeletedBusinessMessages` |
| `@BotPaidMediaPurchased` | Paid media purchase events | `@BotPaidMediaPurchased` |
| `@BotDefaultHandler` | Global fallback | `@BotDefaultHandler` |
| `@BotExceptionHandler` | Exception type handler | `@BotExceptionHandler(NullPointerException.class)` |

## @BotCommand

Route bot commands (messages starting with `/`).

```java
@BotCommand("/start")
public String onStart() {
    return "Welcome!";
}

@BotCommand("/help")
public String onHelp() {
    return "Commands: /start, /help, /cancel";
}
```

Multiple handlers for same command can use `@BotOrder` for priority:

```java
@BotCommand("/admin")
@BotOrder(1) // Higher priority
public String onAdminAccess(User user) {
    if (isAdmin(user)) return "Admin panel";
    throw new UnauthorizedException();
}

@BotCommand("/admin")
@BotOrder(100) // Lower priority, fallback
public String onAdminDefault() {
    return "Admin access denied";
}
```

## @BotText

Route exact plain-text messages (case-sensitive, full-string match).

```java
@BotText("hello") // Case-sensitive
public String onHello() {
    return "Hi there!";
}

@BotText("goodbye")
public String onGoodbye() {
    return "See you later!";
}
```

## @BotTextPattern

Route text messages matching a regular expression. Use for dynamic or structural inputs such
as phone numbers, order IDs, or messages following a known prefix.

Matching uses `Matcher.find()` — the pattern does **not** need to match the full string unless
you use `^` / `$` anchors. Patterns are compiled once at startup and cached.

```java
// Match 10-digit phone numbers only
@BotTextPattern("^\\d{10}$")
public String onPhone(@BotTextValue String phone) {
    return "Got your number: " + phone;
}

// Match messages starting with a keyword
@BotTextPattern({"^buy .+", "^order .+"})
public String onPurchaseIntent(@BotTextValue String text) {
    return "Processing your request: " + text;
}
```

Can be combined with `@BotChatState` to match regex input only in a specific conversation state.

## @BotCallbackQuery

Route inline button clicks (callback queries).

```java
@BotCallbackQuery("btn_yes")
public String onYesClicked() {
    return "You clicked Yes!";
}

@BotCallbackQuery("btn_no")
public String onNoClicked() {
    return "You clicked No!";
}
```

Typically used with inline keyboards:

```java
@BotCommand("/poll")
public PlainReply onPoll() {
    InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
        .keyboardRow(
            InlineKeyboardButton.builder()
                .text("Yes")
                .callbackData("btn_yes")
                .build(),
            InlineKeyboardButton.builder()
                .text("No")
                .callbackData("btn_no")
                .build()
        )
        .build();
    return PlainReply.of("Do you like this?").withMarkup(keyboard);
}
```

## @BotContact

Route contact-sharing messages (when user shares their phone via keyboard).

```java
@BotContact
public String onContactShared(Contact contact) {
    return "Got your contact: " + contact.getPhoneNumber();
}
```

## @BotLocation

Route location-sharing messages.

```java
@BotLocation
public String onLocationShared(Location location) {
    return "Your location: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude();
}
```

## @BotDefaultCommand, @BotTextDefault, @BotDefaultCallbackQuery

Fallback handlers for unmatched updates of specific type.

```java
@BotDefaultCommand
public String onUnknownCommand() {
    return "Unknown command. Try /help";
}

@BotTextDefault
public String onUnknownText() {
    return "I don't understand that text.";
}

@BotDefaultCallbackQuery
public String onUnknownCallback() {
    return "Unknown button action.";
}
```

## @BotReplyButton

Routes a plain-text message that exactly matches a reply keyboard button label. This is how
Telegram delivers button presses — as plain text messages with the button's label as content.

**Without `core-i18n`** — values are matched as literal strings:

```java
@BotReplyButton(" Confirm")
public String onConfirm() {
    return "Confirmed!";
}

@BotReplyButton({" Cancel", "Back"})
@BotClearChatState
public String onCancel() {
    return "Cancelled. State cleared.";
}
```

**With `core-i18n`** — values are treated as message-bundle keys. The framework resolves each
key in the user's current locale and compares the result against the incoming text. A single
annotation covers all supported languages automatically:

```java
// messages/bot_en.properties: btn.confirm= Confirm
// messages/bot_ru.properties: btn.confirm= Подтвердить
@BotReplyButton("btn.confirm")
public String onConfirm() {
    return "Confirmed!";
}
```

## New Update Type Handlers (0.0.2)

Easygram 0.0.2 adds handler annotations for every remaining Telegram `Update` field that was previously unaddressed. Each annotation follows the same rules as the existing ones: declare it on a method inside a `@BotController` class, inject the relevant update-specific types as method parameters, and combine freely with `@BotChatState`, `@BotOrder`, and other meta-annotations. All annotations live in `uz.osoncode.easygram.core.bind.annotation`.

### @BotEditedMessage

Routes updates where the user edits a previously sent text or media message (`update.getEditedMessage()`).

**No configurable attributes.**

```java
@BotEditedMessage
public String onEdited(Message editedMessage) {
    return "You edited your message to: " + editedMessage.getText();
}
```

---

### @BotChannelPost

Routes new messages published to a channel that the bot administers (`update.getChannelPost()`).

**No configurable attributes.**

```java
@BotChannelPost
public void onChannelPost(Message post) {
    log.info("New channel post: {}", post.getText());
}
```

---

### @BotEditedChannelPost

Routes edits to channel posts (`update.getEditedChannelPost()`).

**No configurable attributes.**

```java
@BotEditedChannelPost
public void onEditedChannelPost(Message edited) {
    log.info("Channel post edited: {}", edited.getText());
}
```

---

### @BotInlineQuery

Routes inline queries — triggered when users type `@BotUsername …` in any chat (`update.getInlineQuery()`).

| Attribute | Type | Default | Description |
|---|---|---|---|
| `value` | `String[]` | `{}` | Match inline query text; empty = all inline queries |

**Without `core-i18n`** — values are matched as literal strings:

```java
// Match all inline queries
@BotInlineQuery
public void onAnyInlineQuery(InlineQuery query, @BotInlineQueryValue String queryText) {
    log.info("Inline query: {}", queryText);
}

// Match only when query text is exactly "search"
@BotInlineQuery("search")
public void onSearchQuery(InlineQuery query, @BotInlineQueryValue String queryText) {
    // Handle search inline query
}
```

**With `core-i18n`** — values are treated as message-bundle keys. The framework resolves each
key in the user's current locale and compares the result against the incoming query text. A single
annotation covers all supported languages automatically:

```java
// messages/bot_en.properties: inline.search=search
// messages/bot_ru.properties: inline.search=поиск
@BotInlineQuery("inline.search")
public void onSearchQuery(InlineQuery query, @BotInlineQueryValue String queryText) {
    // matches "@YourBot search" for English users
    // and "@YourBot поиск" for Russian users
}
```

---

### @BotChosenInlineResult

Routes the event fired when a user selects a result from an inline query (`update.getChosenInlineQuery()`). Requires "inline feedback" to be enabled in BotFather.

**No configurable attributes.**

```java
@BotChosenInlineResult
public void onChosenResult(
        ChosenInlineResult result,
        @BotChosenInlineResultId String resultId) {
    log.info("User chose inline result: {}", resultId);
}
```

---

### @BotShippingQuery

Routes shipping address queries for payments with flexible shipping (`update.getShippingQuery()`).

**No configurable attributes.**

```java
@BotShippingQuery
public void onShipping(
        ShippingQuery query,
        @BotShippingPayload String invoicePayload) {
    log.info("Shipping query for invoice: {}", invoicePayload);
}
```

---

### @BotPreCheckoutQuery

Routes pre-checkout events sent just before a payment is confirmed (`update.getPreCheckoutQuery()`). You must answer with `AnswerPreCheckoutQuery` within 10 seconds.

**No configurable attributes.**

```java
@BotPreCheckoutQuery
public void onPreCheckout(
        PreCheckoutQuery query,
        @BotPreCheckoutPayload String invoicePayload,
        TelegramClient client) throws TelegramApiException {
    client.execute(AnswerPreCheckoutQuery.builder()
        .preCheckoutQueryId(query.getId())
        .ok(true)
        .build());
}
```

---

### @BotPoll

Routes poll state-change updates — sent when a poll is stopped or its vote counts change (`update.getPoll()`).

**No configurable attributes.**

```java
@BotPoll
public void onPollUpdate(Poll poll) {
    log.info("Poll '{}' updated, total voters: {}", poll.getQuestion(), poll.getTotalVoterCount());
}
```

---

### @BotPollAnswer

Routes events fired when a user votes in a non-anonymous poll (`update.getPollAnswer()`).

**No configurable attributes.**

```java
@BotPollAnswer
public void onPollAnswer(PollAnswer answer) {
    log.info("User {} voted option(s) {}", answer.getUser().getId(), answer.getOptionIds());
}
```

---

### @BotMyChatMember

Routes updates about the bot's own membership status changes in a chat (`update.getMyChatMember()`). Useful for detecting when the bot is added to or removed from groups/channels.

**No configurable attributes.**

```java
@BotMyChatMember
public void onBotMembershipChange(ChatMemberUpdated updated) {
    log.info("Bot status in chat {} changed from {} to {}",
        updated.getChat().getId(),
        updated.getOldChatMember().getStatus(),
        updated.getNewChatMember().getStatus());
}
```

---

### @BotChatMember

Routes updates about a user's membership status changes in a chat (`update.getChatMember()`). Requires the bot to be an administrator.

**No configurable attributes.**

```java
@BotChatMember
public void onUserMembershipChange(ChatMemberUpdated updated) {
    log.info("User {} status changed in chat {}",
        updated.getFrom().getId(), updated.getChat().getId());
}
```

---

### @BotChatJoinRequest

Routes join requests submitted to a chat that requires admin approval (`update.getChatJoinRequest()`).

**No configurable attributes.**

```java
@BotChatJoinRequest
public void onJoinRequest(ChatJoinRequest request, TelegramClient client) throws TelegramApiException {
    // Approve the request automatically
    client.execute(ApproveChatJoinRequest.builder()
        .chatId(request.getChat().getId())
        .userId(request.getUser().getId())
        .build());
}
```

---

### @BotBusinessConnection

Routes updates about business account connections (`update.getBusinessConnection()`). Fired when a user connects or disconnects their business account from the bot.

**No configurable attributes.**

```java
@BotBusinessConnection
public void onBusinessConnection(BusinessConnection connection) {
    log.info("Business connection {} is enabled: {}", connection.getId(), connection.getIsEnabled());
}
```

---

### @BotBusinessMessage

Routes messages sent on behalf of a connected business account (`update.getBusinessMessage()`).

**No configurable attributes.**

```java
@BotBusinessMessage
public void onBusinessMessage(Message message) {
    log.info("Business message from {}: {}", message.getChat().getId(), message.getText());
}
```

---

### @BotEditedBusinessMessage

Routes edits to messages sent via a connected business account (`update.getEditedBuinessMessage()`).

**No configurable attributes.**

```java
@BotEditedBusinessMessage
public void onEditedBusinessMessage(Message edited) {
    log.info("Business message edited in chat {}", edited.getChat().getId());
}
```

---

### @BotDeletedBusinessMessages

Routes events fired when messages sent via a business account are deleted (`update.getDeletedBusinessMessages()`).

**No configurable attributes.**

```java
@BotDeletedBusinessMessages
public void onDeletedBusinessMessages(BusinessMessagesDeleted deleted) {
    log.info("Deleted {} messages in chat {}",
        deleted.getMessageIds().size(), deleted.getChat().getId());
}
```

---

### @BotPaidMediaPurchased

Routes paid media purchase events. Inject the raw `Update` and call `update.getPaidMediaPurchased()` to access the payload.

**No configurable attributes.**

```java
@BotPaidMediaPurchased
public void onPaidMedia(Update update) {
    var purchase = update.getPaidMediaPurchased();
    log.info("Paid media purchased by user {}, payload: {}",
        purchase.getFrom().getId(), purchase.getPaidMediaPayload());
}
```

---

## @BotDefaultHandler

Global fallback for any unmatched update.

```java
@BotDefaultHandler
public String onDefault() {
    return "I didn't understand. Try /start or /help";
}
```

Used last, after all other handlers fail.

## @BotExceptionHandler

Handle specific exception types.

```java
@BotExceptionHandler(IllegalArgumentException.class)
public String handleValidationError(IllegalArgumentException e) {
    return "Validation error: " + e.getMessage();
}

@BotExceptionHandler(Exception.class)
public String handleGenericError(Exception e) {
    return "Sorry, an error occurred!";
}
```

Scoped to the containing `@BotController` class. For global exception handling across all controllers, use `@BotControllerAdvice`.

## @BotOrder

Control execution priority when multiple handlers match the same update.

Lower value = higher priority. Default: `Integer.MAX_VALUE`.

When two handlers share the **same** `@BotOrder` value, execution order is determined by the
order they were registered (typically class load order) — which is undefined. Always use
distinct values when handler priority matters.

```java
@BotCommand("/start")
@BotOrder(1)
public String vipStart(User user) {
    if (isVip(user)) return "Welcome, VIP!";
    throw new UnauthorizedException(); // Try next handler
}

@BotCommand("/start")
@BotOrder(100)
public String defaultStart() {
    return "Welcome!";
}
```

:::tip Skipping to the next handler
Throwing an exception inside a handler causes the dispatcher to skip to the next matching
handler (with a higher `@BotOrder` value). Use this pattern to implement priority-based access
control without early return logic.
:::

## Handler Dispatch Order

When an update arrives, Easygram searches in this order:

1. **Tier 1**: State handlers (if `@BotChatState` matches user's state)
2. **Tier 2**: Spec handlers (`@BotCommand`, `@BotText`, etc.)
3. **Tier 3**: Default handlers (`@BotDefaultHandler`, etc.)

Within each tier, handlers with lower `@BotOrder` value are tried first.

First matching handler is executed; others are skipped.

## Example: Complete Handler Class

```java
@BotController
public class MyBotHandler {
    
    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "!";
    }
    
    @BotCommand("/help")
    @BotOrder(1)
    public String onHelp() {
        return "Commands: /start, /help, /cancel";
    }
    
    @BotText("hello")
    public String onHello() {
        return "Hi there!";
    }
    
    @BotCallbackQuery("btn_yes")
    public String onYesButton() {
        return "Thanks for clicking!";
    }
    
    @BotContact
    public String onContactShared(Contact contact) {
        return "Got your number!";
    }
    
    @BotDefaultHandler
    public String onDefault() {
        return "I don't understand. Try /help";
    }
    
    @BotExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorized(UnauthorizedException e) {
        return "You don't have permission!";
    }
}
```

---

Next: Learn about [parameter injection](parameter-injection) to access user data and message content.
