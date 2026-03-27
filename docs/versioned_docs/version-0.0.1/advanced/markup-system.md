---
id: markup-system
title: Keyboard & Markup System
---

# Keyboard & Markup System

easygram provides a first-class keyboard management system that lets you declare keyboards as named beans, attach them to responses declaratively or programmatically, and pass runtime parameters into factory methods — all without wiring Telegram API objects by hand in every handler.

---

## Overview

The markup system is built from five cooperating pieces:

| Component | Role |
|---|---|
| `@BotConfiguration` | Marks a class as a keyboard factory (component stereotype) |
| `@BotMarkup("id")` | Registers a method's return value as a named keyboard |
| `BotMarkupRegistry` | Stores and resolves named keyboards at runtime |
| `MarkupAware` | Interface implemented by `PlainReply`, `PlainTextTemplate`, `LocalizedReply`, and `LocalizedTemplate` — lets return values carry markup intent |
| `BotMarkupContext` | Typed parameter bag passed from a handler to a keyboard factory at resolution time |

Keyboards can be **statically declared** (the factory method has no dynamic inputs) or **dynamically built** at resolution time using `BotMarkupContext` or `BotRequest`.

---

## 1. Registering Keyboards — `@BotConfiguration` + `@BotMarkup`

Annotate any Spring bean class with `@BotConfiguration` (a `@Component` stereotype). Each method annotated with `@BotMarkup("id")` is scanned by `BotMarkupLoader` at startup and stored in `BotMarkupRegistry` under the given ID.

```java
@BotConfiguration
@RequiredArgsConstructor
public class MyKeyboards {

    @BotMarkup("main_menu")
    public ReplyKeyboardMarkup mainMenu() {
        return ReplyKeyboardMarkup.builder()
            .keyboardRow(new KeyboardRow(List.of(
                KeyboardButton.builder().text(" Confirm").build(),
                KeyboardButton.builder().text(" Cancel").build()
            )))
            .resizeKeyboard(true)
            .build();
    }
}
```

### Supported factory method signatures

The argument resolver infrastructure used for handler methods is also applied to `@BotMarkup` factory methods. Any combination of the following parameter types is supported:

| Parameter type | Resolved from |
|---|---|
| *(none)* | Static — evaluated once or on demand |
| `BotRequest` | The live request being processed |
| `BotMarkupContext` | Parameters passed by the calling handler (see 5) |
| `Locale` | Current user locale *(requires `core-i18n`)* |

```java
// Static — no parameters
@BotMarkup("confirm_menu")
public ReplyKeyboard confirmMenu() { ... }

// Request-aware — can read language code, user ID, etc.
@BotMarkup("locale_menu")
public ReplyKeyboard localeMenu(BotRequest request) {
    String lang = request.getUser().getLanguageCode();
    // build locale-sensitive keyboard
}

// Parameter-driven — handler passes runtime data
@BotMarkup("item_list")
public InlineKeyboardMarkup itemList(BotMarkupContext ctx) {
    List<String> items = ctx.get("items");
    // build keyboard from items
}

// Combined
@BotMarkup("user_actions")
public InlineKeyboardMarkup userActions(BotRequest request, BotMarkupContext ctx) {
    // both available simultaneously
}
```

---

## 2. Attaching Markup to Responses

There are five ways to attach a keyboard to a bot response, ranging from declarative annotations to inline construction.

### 2a. `@BotReplyMarkup` annotation — handler-level fallback

Place `@BotReplyMarkup("id")` on a handler method. This is always applied when the handler returns `String`. For `MarkupAware` return types it acts as a fallback — it is only applied when the return value itself carries neither a direct keyboard nor a markup ID.

```java
@BotCommand("/start")
@BotReplyMarkup("main_menu")
public String onStart() {
    return "Welcome! Choose an option:";
}
```

### 2b. `.withMarkup("id")` — on `MarkupAware` return types

`PlainReply`, `PlainTextTemplate`, `LocalizedReply`, and `LocalizedTemplate` all implement `MarkupAware`. Call `.withMarkup("id")` to attach a registered keyboard by ID. The return types are **immutable** — each fluent method returns a new instance.

```java
@BotCommand("/menu")
public PlainReply showMenu() {
    return PlainReply.of("Choose an option:").withMarkup("main_menu");
}

@BotCommand("/dashboard")
public LocalizedReply showDashboard() {
    return LocalizedReply.of("dashboard.title").withMarkup("main_menu");
}
```

### 2c. `.withKeyboard(ReplyKeyboard)` — inline direct keyboard, no registry

Use `.withKeyboard(kb)` to attach a keyboard object directly. The registry is bypassed entirely — useful for one-off or fully dynamic keyboards that do not warrant a named registration.

```java
@BotCommand("/quick")
public PlainReply quickChoice() {
    ReplyKeyboardMarkup kb = ReplyKeyboardMarkup.builder()
        .keyboardRow(new KeyboardRow(List.of(
            new KeyboardButton("Yes"),
            new KeyboardButton("No")
        )))
        .resizeKeyboard(true)
        .oneTimeKeyboard(true)
        .build();

    return PlainReply.of("Continue?").withKeyboard(kb);
}
```

### 2d. `.removeMarkup()` — send `ReplyKeyboardRemove`

Explicitly remove any existing reply keyboard from the client.

```java
@BotCommand("/cancel")
public PlainReply onCancel() {
    return PlainReply.of("Operation cancelled.").removeMarkup();
}
```

### 2e. `@BotClearMarkup` annotation — always removes

Placing `@BotClearMarkup` on a handler method unconditionally sends a `ReplyKeyboardRemove`, regardless of what the return value contains. It has the highest precedence of all markup directives.

```java
@BotCommand("/done")
@BotClearMarkup
public String onDone() {
    return "All done! Keyboard removed.";
}
```

---

## 3. State-Bound Keyboards — `@BotChatState` on `@BotMarkup`

A `@BotMarkup` method can be annotated with `@BotChatState` to declare it as the **default keyboard for that chat state**. When a handler method returns without any explicit markup directive, the framework automatically resolves the keyboard bound to the handler's **effective next state** — no `@BotReplyMarkup` needed on the handler side.

### How it works

1. Add `@BotChatState("STATE_NAME")` to a `@BotMarkup` factory method. You may list multiple states.
2. The framework detects the annotation at startup (via `BotMarkupLoader`) and calls `BotMarkupRegistry.registerForState(state, factory)` for each declared state.
3. At dispatch time, `MarkupApplicationFilter` determines the **effective next state** from the handler method's annotations and uses it to look up the state-bound keyboard.

### Determining the effective next state

| Annotation on the handler | Effective state used for lookup |
|---|---|
| `@BotForwardChatState("X")` | `"X"` (the state the chat will enter after this handler) |
| `@BotClearChatState` | No auto-keyboard (state will be cleared) |
| neither | Current state from `BotChatStateService` (the state the chat is in now) |

:::note Why annotations and not `chatStateService.getState()`?
`ChatStateUpdateFilter` — the filter that actually writes the new state — runs **after** `MarkupApplicationFilter`. At the time the keyboard is being resolved, the state hasn't been updated yet. Reading `@BotForwardChatState` directly from the method annotation ensures the keyboard is attached to the correct state.
:::

### Example

```java
@BotConfiguration
@RequiredArgsConstructor
public class RegistrationMarkups {

    private final BotKeyboardFactory keyboardFactory;

    // Bound to AWAITING_NAME and AWAITING_CITY — auto-attached when entering those states
    @BotMarkup("kb_cancel")
    @BotChatState({"AWAITING_NAME", "AWAITING_CITY"})
    public ReplyKeyboard cancelKeyboard(BotRequest request) {
        return keyboardFactory.reply(request)
            .row("btn.cancel")
            .resizeKeyboard(true)
            .oneTimeKeyboard(true)
            .build();
    }

    // Bound to AWAITING_PHONE — auto-attached when entering or staying in that state
    @BotMarkup("kb_send_phone")
    @BotChatState("AWAITING_PHONE")
    public ReplyKeyboard sharePhoneKeyboard(BotRequest request) {
        KeyboardButton phoneBtn = keyboardFactory.replyButton("btn.send.phone", request);
        phoneBtn.setRequestContact(true);
        return keyboardFactory.reply(request)
            .row(phoneBtn)
            .build();
    }
}

@BotController
@BotChatState({"AWAITING_NAME", "AWAITING_PHONE", "AWAITING_CITY"})
public class RegistrationController {

    @BotCommand("/register")
    @BotChatState // override class guard — accept any state
    @BotForwardChatState("AWAITING_NAME")
    // ↑ kb_cancel auto-attached because AWAITING_NAME is bound to it
    public LocalizedReply startRegistration(User user) {
        return LocalizedReply.of("register.start");
    }

    @BotTextDefault
    @BotChatState("AWAITING_NAME")
    @BotForwardChatState("AWAITING_PHONE")
    // ↑ kb_send_phone auto-attached because AWAITING_PHONE is bound to it
    public LocalizedReply collectName(@BotTextValue String name) {
        return LocalizedReply.of("register.name.saved", name);
    }

    @BotContact
    @BotChatState("AWAITING_PHONE")
    @BotForwardChatState("AWAITING_CITY")
    // ↑ kb_cancel auto-attached because AWAITING_CITY is bound to it
    public LocalizedReply collectPhone(Contact contact) {
        return LocalizedReply.of("register.phone.saved", contact.getPhoneNumber());
    }

    @BotTextDefault
    @BotChatState("AWAITING_PHONE")
    // ↑ No @BotForwardChatState → current state ("AWAITING_PHONE") is used
    // → kb_send_phone auto-attached
    public LocalizedReply invalidPhone() {
        return LocalizedReply.of("register.phone.invalid");
    }

    @BotTextDefault
    @BotChatState("AWAITING_CITY")
    @BotClearChatState
    @BotClearMarkup // explicitly clears keyboard — state-bound lookup is skipped
    public LocalizedTemplate collectCity(@BotTextValue String city) {
        return LocalizedTemplate.of("${register.complete}", "(saved)", "(saved)", city);
    }
}
```

### State-bound keyboard vs. `@BotReplyMarkup`

| Feature | `@BotReplyMarkup("id")` | `@BotChatState` on `@BotMarkup` |
|---|---|---|
| Declared on | Controller handler | Keyboard factory method |
| Coupling | Controller knows the keyboard ID | Keyboard knows the state; controller knows nothing |
| Reuse | One annotation per handler | Register once, auto-applied everywhere |
| Override | N/A | Set `.withMarkup("id")` or `@BotReplyMarkup` to override per-handler |

:::tip Best practice
Use state-bound keyboards when a keyboard naturally belongs to a state (e.g. a cancel keyboard for every step of a wizard). Use `@BotReplyMarkup` or `.withMarkup("id")` when the keyboard is specific to a single handler regardless of state.
:::

---

## 4. Markup Precedence

When multiple markup directives are in play, they are evaluated in this order (highest wins):

| Priority | Source | Behaviour |
|---|---|---|
| **1** | `@BotClearMarkup` annotation | Always sends `ReplyKeyboardRemove`. Overrides everything. |
| **2** | `getKeyboard() != null` on `MarkupAware` | Uses the keyboard object directly; no registry lookup. |
| **3** | `getMarkupId() != null` on `MarkupAware` | Registry lookup, optionally with `BotMarkupContext` params. |
| **4** | `@BotReplyMarkup` annotation | Fallback; only applied when neither keyboard nor markupId is set on the return value. |
| **5** | State-bound keyboard (`@BotChatState` on `@BotMarkup`) | Lowest-priority fallback; resolved from `BotMarkupRegistry` using the handler's effective next state. |

:::note String returns
If a handler returns `String` (not `MarkupAware`), `@BotReplyMarkup` is the only declarative option and is always applied.
:::

---

## 5. Dynamic Markup Parameters — `BotMarkupContext`

When a keyboard factory needs data that only exists at request time (such as a list of user-specific items), the calling handler can pass a parameter map via `.withMarkup("id", params)`. The factory method receives those parameters as a `BotMarkupContext`.

```java
// Handler — passes params at return time
@BotCommand("/items")
public PlainReply showItems(User user) {
    List<String> items = itemService.getItems(user.getId());

    return PlainReply.of("Your items:")
        .withMarkup("item_list", Map.of(
            "items", items,
            "userId", user.getId()
        ));
}

// Keyboard factory — receives params via BotMarkupContext
@BotConfiguration
@RequiredArgsConstructor
public class ItemKeyboards {

    @BotMarkup("item_list")
    public InlineKeyboardMarkup itemList(BotMarkupContext ctx) {
        List<String> items = ctx.get("items");
        Long userId = ctx.get("userId", Long.class);

        var rows = items.stream()
            .map(item -> new InlineKeyboardRow(List.of(
                InlineKeyboardButton.builder()
                    .text(item)
                    .callbackData("select:" + userId + ":" + item)
                    .build())))
            .toList();

        return new InlineKeyboardMarkup(rows);
    }
}
```

### `BotMarkupContext` API

```java
ctx.get("key") // unchecked cast to inferred type
ctx.get("key", String.class) // safe typed cast
ctx.getOrDefault("key", "fallback") // returns fallback when key absent
ctx.has("key") // existence check → boolean
ctx.asMap() // read-only view of full parameter map
BotMarkupContext.empty() // empty context (no params)
```

:::tip
`BotMarkupContext.empty()` is injected automatically when a handler uses `.withMarkup("id")` without a parameter map. Factory methods that declare a `BotMarkupContext` parameter are therefore always safe to call.
:::

---

## 6. Locale-aware Keyboards with `core-i18n` — `BotKeyboardFactory`

When the `core-i18n` module is on the classpath, `BotKeyboardFactory` is available for building keyboards whose button labels are resolved from a Spring `MessageSource`. Inject it into any `@BotConfiguration` class.

```java
@BotConfiguration
@RequiredArgsConstructor
public class LocalizedMenus {

    private final BotKeyboardFactory keyboardFactory;

    // Reply keyboard — message bundle keys as button text
    @BotMarkup("localized_menu")
    public ReplyKeyboard localizedMenu(BotRequest request) {
        return keyboardFactory.reply(request)
            .row("btn.confirm", "btn.cancel") // message bundle keys
            .row("btn.back")
            .resizeKeyboard(true)
            .build();
    }

    // Inline keyboard — pairs of (textKey, callbackData)
    @BotMarkup("inline_actions")
    public InlineKeyboardMarkup inlineActions(BotRequest request) {
        return keyboardFactory.inline(request)
            .row(
                "btn.yes", "action:yes",
                "btn.no", "action:no"
            )
            .build();
    }
}
```

`BotKeyboardFactory` resolves button labels via `BotMessageSource` using the locale derived from `BotRequest`, so the same keyboard registration automatically adapts to each user's language.

---

## 7. Inline Keyboard — Full Handler + Factory Pattern

The following example shows a complete round-trip: a callback query handler returns an edited message and attaches an inline keyboard via `@BotReplyMarkup`.

```java
@BotController
@RequiredArgsConstructor
public class EditController {

    @BotCallbackQuery("edit:")
    @BotReplyMarkup("edit_options")
    public BotApiMethod<?> onEdit(CallbackQuery query) {
        return EditMessageText.builder()
            .chatId(query.getMessage().getChatId())
            .messageId(query.getMessage().getMessageId())
            .text("What do you want to edit?")
            .build();
    }
}

@BotConfiguration
public class EditKeyboards {

    @BotMarkup("edit_options")
    public InlineKeyboardMarkup editOptions() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(List.of(
                InlineKeyboardButton.builder()
                    .text("Name")
                    .callbackData("edit:name")
                    .build(),
                InlineKeyboardButton.builder()
                    .text("Email")
                    .callbackData("edit:email")
                    .build()
            )))
            .build();
    }
}
```

:::note
`@BotReplyMarkup` on a method returning `BotApiMethod<?>` is handled differently to `String` or `MarkupAware` types. The framework sets the `replyMarkup` field on the outgoing method object before it is sent, so no manual wiring is required.
:::

---

## 8. Overriding `BotMarkupRegistry`

The default implementation is `InMemoryBotMarkupRegistry`. All beans in the autoconfiguration are annotated `@ConditionalOnMissingBean`, so replacing the registry is a single `@Bean` declaration:

```java
@Configuration
public class MarkupConfig {

    @Bean
    public BotMarkupRegistry myMarkupRegistry() {
        return new MyDatabaseBackedMarkupRegistry();
    }
}
```

Your custom registry must implement `BotMarkupRegistry`, which requires at minimum:
- `register(String id, ...)` — called by `BotMarkupLoader` at startup for each `@BotMarkup` method
- `resolve(String id, BotMarkupContext ctx)` — called at dispatch time to produce a `ReplyKeyboard`

---

## Quick Reference

### Which approach should I use?

| Scenario | Recommended approach |
|---|---|
| Same keyboard used on many handlers at a given state | `@BotChatState` on `@BotMarkup` — auto-attached to all handlers entering that state |
| Same keyboard used on many handlers (state-independent) | `@BotConfiguration` + `@BotMarkup` + `@BotReplyMarkup` |
| Keyboard varies by return value, same handler | `.withMarkup("id")` on `MarkupAware` |
| Keyboard needs runtime data (user-specific) | `.withMarkup("id", params)` + `BotMarkupContext` in factory |
| One-off inline keyboard, not reused | `.withKeyboard(kb)` directly on `MarkupAware` |
| Remove keyboard | `.removeMarkup()` or `@BotClearMarkup` |
| Locale-translated button labels | `BotKeyboardFactory` inside `@BotMarkup` factory |

### Markup precedence at a glance

```
@BotClearMarkup > .withKeyboard(kb) > .withMarkup("id") > @BotReplyMarkup > state-bound keyboard
```
