---
id: handlers
title: Handler Annotations
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
| `@BotReplyButton` | Reply keyboard button press | `@BotReplyButton("✅ Confirm")` |
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
@BotOrder(1)  // Higher priority
public String onAdminAccess(User user) {
    if (isAdmin(user)) return "Admin panel";
    throw new UnauthorizedException();
}

@BotCommand("/admin")
@BotOrder(100)  // Lower priority, fallback
public String onAdminDefault() {
    return "Admin access denied";
}
```

## @BotText

Route exact plain-text messages (case-sensitive, full-string match).

```java
@BotText("hello")  // Case-sensitive
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
@BotReplyButton("✅ Confirm")
public String onConfirm() {
    return "Confirmed!";
}

@BotReplyButton({"❌ Cancel", "Back"})
@BotClearChatState
public String onCancel() {
    return "Cancelled. State cleared.";
}
```

**With `core-i18n`** — values are treated as message-bundle keys. The framework resolves each
key in the user's current locale and compares the result against the incoming text. A single
annotation covers all supported languages automatically:

```java
// messages/bot_en.properties: btn.confirm=✅ Confirm
// messages/bot_ru.properties: btn.confirm=✅ Подтвердить
@BotReplyButton("btn.confirm")
public String onConfirm() {
    return "Confirmed!";
}
```

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
    throw new UnauthorizedException();  // Try next handler
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
