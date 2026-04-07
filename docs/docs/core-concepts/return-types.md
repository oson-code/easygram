---
id: return-types
title: Return Types
---

# Return Types

Handler methods can return different types. Easygram handles each using a strategy chain of
`BotReturnTypeHandler` implementations — the first handler whose `supportsReturnType()` matches
the method's declared return type wins.

## Supported Return Types

| Return Type | Module | Behavior |
|---|---|---|
| `void` | `core` | No response sent |
| `String` | `core` | `SendMessage` to the current chat |
| `PlainReply` | `core` | `SendMessage` (or `EditMessageText` when `editMessage=true`) with optional keyboard |
| `PlainTextTemplate` | `core` | `SendMessage` (or `EditMessageText`) with `#{index}` token substitution |
| `BotApiMethod<?>` | `core` | Executed directly via `TelegramClient` |
| `Collection<BotApiMethod<?>>` | `core` | All executed in insertion order |
| `Collection<Object>` | `core` | Per-element dispatch via `supportsElement()` |
| `LocalizedReply` | `core-i18n` | `MessageSource` key lookup with `Locale` (or `EditMessageText` when `editMessage=true`) |
| `LocalizedTemplate` | `core-i18n` | Mixed `${key}` / `#{index}` template with `MessageSource` (or `EditMessageText` when `editMessage=true`) |

---

## `void`

No response is sent. Use when you only need a side effect:

```java
@BotCommand("/subscribe")
public void subscribe(User user) {
    subscriptionService.add(user.getId());
    // nothing sent to the user
}
```

---

## `String`

The simplest way to send a text message. Supports `@BotReplyMarkup` and `@BotClearMarkup`
annotations on the method for attaching or removing a keyboard:

```java
@BotCommand("/hello")
public String hello(User user) {
    return "Hello, " + user.getFirstName() + "!";
}

@BotReplyMarkup("main_menu")
@BotCommand("/start")
public String start() {
    return "Choose an option:";  // reply keyboard attached from registry
}

@BotClearMarkup
@BotCommand("/cancel")
public String cancel() {
    return "Cancelled.";  // ReplyKeyboardRemove sent automatically
}
```

Returning `null` from a `String` handler produces no action (same as `void`).

---

## `PlainReply`

Sends a `SendMessage` with optional keyboard control via fluent wither methods. All methods
return a new immutable instance. You can also use the builder:

```java
@BotCommand("/menu")
public PlainReply menu() {
    return PlainReply.of("Choose an option:")
        .withMarkup("main_menu");           // keyboard from registry by ID
}

@BotCommand("/done")
public PlainReply done() {
    return PlainReply.of("Done!")
        .removeMarkup();                    // sends ReplyKeyboardRemove
}

// Builder pattern
@BotCommand("/start")
public PlainReply start() {
    return PlainReply.builder()
        .text("Welcome! Choose an option:")
        .markupId("main_menu")
        .build();
}
```

See the [MarkupAware section](#markupaware) for all keyboard attachment options.

### Edit-Message {#edit-message}

When a handler is triggered by a callback query button, you often want to **edit** the
message the button is on instead of sending a new one. Set `editMessage(true)` on the builder,
or call `withEditMessage()` on an existing instance:

```java
@BotCallbackQuery("confirm")
public PlainReply onConfirm() {
    return PlainReply.builder()
            .text("Confirmed! ✓")
            .editMessage(true)       // edits the message that held the inline button
            .build();
}

// Wither style
@BotCallbackQuery("cancel")
public PlainReply onCancel(PlainReply reply) {
    return PlainReply.of("Cancelled.").withEditMessage();
}
```

When `editMessage=true` and the request has a callback query, the framework emits:
1. `EditMessageText` — updates the message text.
2. `EditMessageReplyMarkup` — updates the inline keyboard (omitted if no keyboard is set and `removeMarkup` is false).

:::caution Inline keyboards only
Telegram's `EditMessageReplyMarkup` API only accepts `InlineKeyboardMarkup`. If you attach a
`ReplyKeyboardMarkup` in edit context it is silently ignored.
:::

---

## `PlainTextTemplate`

Like `PlainReply`, but substitutes `#{index}` positional tokens (0-based) into the message at
send time. No message-bundle lookup is performed — use `LocalizedTemplate` when i18n is needed.

```java
@BotCommand("/welcome")
public PlainTextTemplate welcome(User user) {
    return PlainTextTemplate.of("Hello, #{0}! You have #{1} messages.", user.getFirstName(), 5);
    // Sends: "Hello, Alice! You have 5 messages."
}

@BotCommand("/balance")
public PlainTextTemplate balance(User user) {
    double amount = accountService.getBalance(user.getId());
    return PlainTextTemplate.of("Your balance: #{0} USD", amount)
        .withMarkup("account_menu");
}

// Builder pattern
@BotCommand("/info")
public PlainTextTemplate info(User user) {
    return PlainTextTemplate.builder()
        .template("Hello, #{0}! Your ID is #{1}.")
        .args(user.getFirstName(), user.getId())
        .markupId("main_menu")
        .build();
}
```

:::note
`PlainTextTemplate` uses `#{index}` tokens (same format as `LocalizedTemplate`). If the index
is out of bounds the token is left unchanged.
:::

`PlainTextTemplate` also supports the `editMessage` flag — see [Edit-Message](#edit-message)
above for details (the behaviour is identical to `PlainReply`).

---

## `BotApiMethod<?>`

Return any Telegram Bot API method directly. It is executed synchronously by the framework and
its result (if any) is discarded. Covers the full API surface: media, editing, callbacks, etc.

### Send a photo

```java
@BotCommand("/photo")
public BotApiMethod<?> photo(Chat chat) {
    return SendPhoto.builder()
        .chatId(chat.getId())
        .photo(new InputFile(new File("photo.jpg")))
        .caption("Here's a photo!")
        .build();
}
```

### Send a document

```java
@BotCommand("/report")
public BotApiMethod<?> report(Chat chat) {
    return SendDocument.builder()
        .chatId(chat.getId())
        .document(new InputFile(new File("report.pdf")))
        .caption("Your monthly report")
        .build();
}
```

### Edit an existing message

```java
@BotCallbackQuery("refresh")
public BotApiMethod<?> refresh(Update update) {
    var query = update.getCallbackQuery();
    return EditMessageText.builder()
        .chatId(query.getFrom().getId())
        .messageId(query.getMessage().getMessageId())
        .text("Updated content")
        .build();
}
```

### Delete a message

```java
@BotCallbackQuery("close")
public BotApiMethod<?> close(Update update) {
    var query = update.getCallbackQuery();
    return DeleteMessage.builder()
        .chatId(query.getFrom().getId())
        .messageId(query.getMessage().getMessageId())
        .build();
}
```

### Answer a callback query (toast / modal alert)

```java
@BotCallbackQuery("confirm")
public BotApiMethod<?> confirm(Update update) {
    return AnswerCallbackQuery.builder()
        .callbackQueryId(update.getCallbackQuery().getId())
        .text("Done!")
        .showAlert(false)   // true = modal alert, false = brief toast
        .build();
}
```

---

## `Collection<BotApiMethod<?>>`

Execute multiple API methods in one handler, in insertion order:

```java
@BotCommand("/broadcast")
public Collection<BotApiMethod<?>> broadcast(Chat chat) {
    return List.of(
        SendMessage.builder().chatId(chat.getId()).text("Part 1").build(),
        SendMessage.builder().chatId(chat.getId()).text("Part 2").build(),
        SendPhoto.builder()
            .chatId(chat.getId())
            .photo(new InputFile("file_id_here"))
            .caption("Final image")
            .build()
    );
}
```

---

## `Collection<Object>`

A mixed-type collection where **each element is dispatched independently** to the first
`BotReturnTypeHandler` whose `supportsElement(element)` returns `true`. Elements are processed
in insertion order; elements with no matching handler are silently skipped.

This lets you combine strings, API methods, and rich reply objects in a single return:

```java
@BotCommand("/profile")
public Collection<Object> profile(User user, Chat chat) {
    return List.of(
        SendPhoto.builder()                              // → BotApiMethod handler
            .chatId(chat.getId())
            .photo(new InputFile("avatar_file_id"))
            .build(),
        "Name: " + user.getFirstName(),                  // → String handler → SendMessage
        PlainReply.of("What would you like to do?")      // → PlainReply handler
            .withMarkup("profile_menu")
    );
}
```

```java
@BotCallbackQuery("show_summary")
public Collection<Object> showSummary(Update update, Chat chat) {
    var query = update.getCallbackQuery();
    return List.of(
        AnswerCallbackQuery.builder()                    // dismiss the spinner
            .callbackQueryId(query.getId())
            .build(),
        PlainTextTemplate.of("Summary for #{0}:\n#{1}",      // #{index} token substitution
            chat.getFirstName(),
            summaryService.get(chat.getId()))
    );
}
```

---

## `LocalizedReply` *(core-i18n)*

Resolves a `MessageSource` key against the user's current `Locale`. Requires the `core-i18n`
module on the classpath.

```java
// messages/bot_en.properties:  welcome=Welcome to the bot!
// messages/bot_ru.properties:  welcome=Добро пожаловать!

@BotCommand("/start")
public LocalizedReply start() {
    return LocalizedReply.of("welcome");
}
```

Pass arguments to the `MessageSource` pattern (`{0}`, `{1}` — standard Spring `MessageSource`
placeholders):

```java
// messages/bot_en.properties:  greeting=Hello, {0}! You joined on {1}.

@BotCommand("/me")
public LocalizedReply me(User user) {
    return LocalizedReply.of("greeting", user.getFirstName(), joinDate(user.getId()));
}
```

With a keyboard:

```java
return LocalizedReply.of("choose.option").withMarkup("main_menu");
```

Builder pattern:

```java
return LocalizedReply.builder()
    .key("greeting")
    .args(user.getFirstName(), joinDate(user.getId()))
    .markupId("main_menu")
    .build();
```

`LocalizedReply` also supports the `editMessage` flag — see [Edit-Message](#edit-message)
for details. Use `withEditMessage()` or `Builder.editMessage(true)`.

---

## `LocalizedTemplate` *(core-i18n)*

Combines `MessageSource` lookups and positional argument interpolation in a **single template
string** you write inline. Two token types are supported:

| Token | Meaning |
|---|---|
| `${key}` | Replaced with `messageSource.getMessage(key, locale)` |
| `#{index}` | Replaced with `args[index]` (0-based) |

The framework resolves all `${key}` tokens first, then substitutes `#{index}` placeholders with
the provided arguments.

```java
// messages/bot_en.properties:
//   welcome.title=Welcome!
//   welcome.body=Here is what you can do.

@BotCommand("/start")
public LocalizedTemplate start(User user) {
    return LocalizedTemplate.of(
        "${welcome.title}\n\n${welcome.body}\n\nHello, #{0}!",
        user.getFirstName()
    );
    // Sends: "Welcome!\n\nHere is what you can do.\n\nHello, Alice!"
}
```

When every substitution comes from the bundle (no runtime args needed), omit `#{index}`:

```java
// messages/bot_en.properties:
//   register.complete=Registration complete. You can now use all features.

@BotCommand("/done")
public LocalizedTemplate registrationComplete() {
    return LocalizedTemplate.of("${register.complete}");
}
```

With runtime args and a keyboard:

```java
@BotCommand("/stats")
public LocalizedTemplate stats(User user) {
    return LocalizedTemplate.of(
        "${stats.header}\n\nMessages: #{0}\nCommands: #{1}",
        statsService.messages(user.getId()),
        statsService.commands(user.getId())
    ).withMarkup("stats_menu");
}
```

Builder pattern:

```java
return LocalizedTemplate.builder()
    .template("${stats.header}\n\nMessages: #{0}\nCommands: #{1}")
    .args(statsService.messages(user.getId()), statsService.commands(user.getId()))
    .markupId("stats_menu")
    .build();
```

:::caution
`LocalizedTemplate` does **not** use `MessageFormat` syntax. Use `#{0}` for positional args,
not `{0}`. The `${key}` tokens are message bundle lookups, not Spring EL.
:::

`LocalizedTemplate` also supports the `editMessage` flag — see [Edit-Message](#edit-message)
for details. Use `withEditMessage()` or `Builder.editMessage(true)`.

---

## MarkupAware

`PlainReply`, `PlainTextTemplate`, `LocalizedReply`, and `LocalizedTemplate` all implement
`MarkupAware`. They are **immutable** — every method returns a new instance with the change applied.

Five methods are available:

### `.withMarkup(String id)`

Looks up a keyboard registered in `BotMarkupRegistry` by its ID (defined via `@BotMarkup` in a
`@BotConfiguration` class):

```java
return PlainReply.of("Choose:").withMarkup("main_menu");
```

### `.withMarkup(String id, Map<String, Object> params)`

Like `.withMarkup(id)` but passes a `Map` of runtime parameters to the `@BotMarkup` factory
method via `BotMarkupContext`. Use this when your keyboard factory needs dynamic data
(e.g., user-specific buttons):

```java
return PlainReply.of("Pick a city:")
    .withMarkup("city_picker", Map.of("country", "UZ", "limit", 10));
```

The factory method receives these params via `BotMarkupContext`. See the
[Markup System](../advanced/markup-system) documentation for details.

### `.withKeyboard(ReplyKeyboard keyboard)`

Attach a `ReplyKeyboard` (or `InlineKeyboardMarkup`) you built directly, bypassing the registry:

```java
InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
    .keyboardRow(List.of(
        InlineKeyboardButton.builder().text("Yes").callbackData("yes").build(),
        InlineKeyboardButton.builder().text("No").callbackData("no").build()
    ))
    .build();

return PlainReply.of("Are you sure?").withKeyboard(kb);
```

### `.removeMarkup()`

Sends a `ReplyKeyboardRemove` to clear the user's reply keyboard:

```java
return PlainReply.of("Done. Keyboard removed.").removeMarkup();
```

### `.withEditMessage()`

Returns a copy with `editMessage=true`. When the request comes from a callback query, the
framework edits the original message instead of sending a new one:

```java
return PlainReply.of("Updated!").withEditMessage();
```

### Answering Callback Queries {#callback-answer}

Every Telegram callback query (inline keyboard button press) must be acknowledged, otherwise
the user sees a loading spinner indefinitely. Call `.asAnswerCallbackQuery()` to send an
`AnswerCallbackQuery` alongside your reply. The reply's own text is used as the popup
notification text. If the update is **not** a callback query, the call is silently ignored.

```java
// Toast popup using the reply text
@BotCallbackQuery("confirm")
public PlainReply onConfirm() {
    return PlainReply.of("Confirmed! ✅").asAnswerCallbackQuery();
}

// Alert dialog (showAlert = true)
@BotCallbackQuery("delete")
public PlainReply onDelete() {
    return PlainReply.of("Item deleted.").asAnswerCallbackQuery().withCallbackAlert();
}

// Full control: alert + URL + cache time
@BotCallbackQuery("premium")
public PlainReply onPremium() {
    return PlainReply.of("Opening premium page…")
            .asAnswerCallbackQuery()
            .withCallbackAlert()
            .withCallbackUrl("https://example.com/premium")
            .withCallbackCacheTime(10);
}

// Combine with edit-message: edit the original message AND answer the callback
@BotCallbackQuery("approve")
public PlainReply onApprove() {
    return PlainReply.of("✅ Approved").withEditMessage().asAnswerCallbackQuery();
}
```

Builder equivalent:

```java
PlainReply.builder()
        .text("Saved!")
        .answerCallbackQuery(true)
        .callbackAlert(true)
        .callbackCacheTime(5)
        .build();
```

`LocalizedReply` has identical methods — the resolved i18n message is used as the popup text:

```java
@BotCallbackQuery("confirm")
public LocalizedReply onConfirm() {
    return LocalizedReply.of("action.confirmed").asAnswerCallbackQuery();
}

@BotCallbackQuery("delete")
public LocalizedReply onDelete() {
    return LocalizedReply.of("item.deleted").asAnswerCallbackQuery().withCallbackAlert();
}
```

| Method | Effect |
|---|---|
| `.asAnswerCallbackQuery()` | Activates `AnswerCallbackQuery` — toast popup with reply text |
| `.withCallbackAlert()` | Sets `showAlert=true` — alert dialog instead of toast |
| `.withCallbackUrl(String)` | Sets the URL to open (deep link or game URL) |
| `.withCallbackCacheTime(int)` | Sets the client-side cache duration in seconds |

---

## Markup Precedence

When multiple markup sources are present, the following priority order applies (highest first):

1. **`@BotClearMarkup` on the method** — always sends `ReplyKeyboardRemove`, overrides everything.
2. **`.withKeyboard(keyboard)` / `getKeyboard() != null`** — the directly-provided `ReplyKeyboard` is used.
3. **`.withMarkup(id)` / `.withMarkup(id, params)` / `getMarkupId() != null`** — registry lookup with optional params.
4. **`@BotReplyMarkup("id")` on the method** — fallback; only applied if the return value carries no keyboard or markup ID.

```java
// @BotClearMarkup wins — keyboard is removed even if PlainReply carries .withMarkup(...)
@BotClearMarkup
@BotCommand("/reset")
public PlainReply reset() {
    return PlainReply.of("Reset!").withMarkup("some_kb");  // withMarkup ignored
}

// @BotReplyMarkup is the fallback — applies because PlainReply carries no markup
@BotReplyMarkup("main_menu")
@BotCommand("/home")
public PlainReply home() {
    return PlainReply.of("Home.");  // main_menu keyboard applied
}

// Explicit .withMarkup beats @BotReplyMarkup
@BotReplyMarkup("main_menu")
@BotCommand("/special")
public PlainReply special() {
    return PlainReply.of("Special!").withMarkup("special_kb");  // special_kb wins
}
```

For `String` return types, markup annotations (`@BotReplyMarkup`, `@BotClearMarkup`) are the
**only** way to attach or remove a keyboard — the `String` type itself has no fluent API.

---

## Returning `null`

Returning `null` from a handler is treated as a no-op — the framework produces no response and
does not throw. If you need "no response" semantically, prefer `void` or return an empty
collection for clarity.

```java
// All equivalent — no message sent
public void handler() { }
public String handler() { return null; }
public Collection<Object> handler() { return Collections.emptyList(); }
```

---

Next: Learn about [exception handling](exception-handling).
