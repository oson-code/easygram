# core-i18n

> Internationalisation (i18n) support for the Easygram framework.
> Provides locale resolution, `BotMessageSource`, `BotKeyboardFactory`, `@BotReplyButton`, and `Locale` parameter injection out of the box.

## Maven Dependency

Already included transitively through the starter. To add it explicitly:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>core-i18n</artifactId>
    <version>0.0.2</version>
</dependency>
```

---

## Quick Setup

### 1. Configure Spring message bundles

```yaml
spring:
  messages:
    basename: messages/bot        # src/main/resources/messages/bot*.properties
    encoding: UTF-8
    use-code-as-default-message: true   # returns key if translation missing

telegram:
  bot:
    i18n:
      default-locale: en          # fallback when user language is unknown
```

### 2. Create locale-specific property files

```
src/main/resources/
  messages/
    bot.properties          ← English fallback
    bot_uz.properties       ← Uzbek
    bot_ru.properties       ← Russian
```

```properties
# bot.properties
welcome         = Welcome, {0}!
choose.option   = Please choose an option:
btn.yes         = Yes
btn.no          = No
btn.cancel      = Cancel
btn.profile     = Profile
btn.settings    = Settings
```

```properties
# bot_uz.properties
welcome         = Xush kelibsiz, {0}!
choose.option   = Iltimos, variantni tanlang:
btn.yes         = Ha
btn.no          = Yo'q
btn.cancel      = Bekor qilish
btn.profile     = Profil
btn.settings    = Sozlamalar
```

```properties
# bot_ru.properties
welcome         = Добро пожаловать, {0}!
choose.option   = Пожалуйста, выберите вариант:
btn.yes         = Да
btn.no          = Нет
btn.cancel      = Отмена
btn.profile     = Профиль
btn.settings    = Настройки
```

---

## BotMessageSource

`BotMessageSource` is a locale-aware wrapper around Spring's `MessageSource`. It auto-resolves the locale from `BotRequest` so handlers never manage locales manually.

```java
@BotController
@RequiredArgsConstructor
public class GreetingHandler {

    private final BotMessageSource messages;

    @BotCommand("/start")
    public String onStart(BotRequest request, User user) {
        // resolved in the user's language automatically
        return messages.getMessage("welcome", request, user.getFirstName());
    }

    @BotCommand("/help")
    public String onHelp(BotRequest request) {
        return messages.getMessage("help.text", request);
    }
}
```

| Method | Description |
|--------|-------------|
| `getMessage(code, request, args...)` | Resolve using user's locale from request |
| `getMessage(code, locale, args...)` | Resolve with explicit locale |
| `getOrDefault(code, default, request, args...)` | Silent fallback — no exception if key missing |
| `resolveLocale(request)` | Return the resolved `Locale` for the request |

---

## Locale Injection

Inject `Locale` directly into any handler method parameter — the framework resolves it automatically from `user.getLanguageCode()`:

```java
@BotCommand("/start")
public String onStart(User user, Locale locale) {
    return switch (locale.getLanguage()) {
        case "uz" -> "Assalomu alaykum, " + user.getFirstName() + "!";
        case "ru" -> "Привет, " + user.getFirstName() + "!";
        default  -> "Hello, " + user.getFirstName() + "!";
    };
}
```

---

## Localised Reply Keyboards

`BotKeyboardFactory` builds `InlineKeyboardMarkup` and `ReplyKeyboardMarkup` from message keys. Button labels are resolved in the user's language at send time.

### InlineKeyboardMarkup (recommended)

Inline buttons carry locale-independent `callbackData` — use these when you need locale-independent handler matching.

```java
@BotController
@RequiredArgsConstructor
public class MenuHandler {

    private final BotKeyboardFactory keyboards;
    private final BotMessageSource messages;

    @BotCommand("/menu")
    public SendMessage sendMenu(Update update, BotRequest request) {
        InlineKeyboardMarkup kb = keyboards.inline(request)
            .row("btn.yes", "cb_yes",  "btn.no",  "cb_no")    // (messageKey, callbackData) pairs
            .row("btn.cancel", "cb_cancel")
            .build();

        return SendMessage.builder()
            .chatId(update.getMessage().getChatId())
            .text(messages.getMessage("choose.option", request))
            .replyMarkup(kb)
            .build();
    }
}
```

### ReplyKeyboardMarkup

Reply keyboard buttons send a text message with the (localised) button label when pressed.

```java
ReplyKeyboardMarkup kb = keyboards.reply(request)
    .row("btn.profile", "btn.settings")   // message keys per button
    .row("btn.help")
    .resizeKeyboard(true)
    .build();
```

---

## Handling Reply Button Presses

### Option 1 — `@BotReplyButton` (recommended with i18n)

`@BotReplyButton` lives in `core` and works in two modes depending on whether `core-i18n` is present:

| Classpath | `value()` treated as | Matching |
|-----------|---------------------|---------|
| `core` only | Exact text strings | `"Yes"`, `"Да"`, `"Ha"` listed explicitly |
| `core` + `core-i18n` | Message-bundle keys | Resolved per user locale automatically |

**Without `core-i18n`** — exact text matching:
```java
@BotReplyButton({"Yes", "Да", "Ha"})
public String onYes(BotRequest request) {
    return "Confirmed!";
}
```

**With `core-i18n`** — message-key matching (single annotation covers all languages):
```java
@BotReplyButton("btn.yes")
public String onYes(BotRequest request) {
    return messages.getMessage("response.confirmed", request);
}

@BotReplyButton("btn.no")
public String onNo(BotRequest request) {
    return messages.getMessage("response.cancelled", request);
}

// Multiple keys on one method
@BotReplyButton({"btn.cancel", "btn.back"})
public String onCancel(BotRequest request) {
    return messages.getMessage("response.cancelled", request);
}
```

When an Uzbek user sends "Ha", an English user sends "Yes", and a Russian user sends "Да", the **same** `@BotReplyButton("btn.yes")` method handles all three.

### Option 2 — `@BotCallbackQuery` with InlineKeyboardMarkup

For locale-independent button handling without `core-i18n`, use inline keyboards and `@BotCallbackQuery`. The `callbackData` never changes regardless of language.

```java
@BotCallbackQuery("cb_yes")
public AnswerCallbackQuery onYes(CallbackQuery query) {
    return AnswerCallbackQuery.builder()
        .callbackQueryId(query.getId())
        .text("Confirmed!")
        .build();
}

@BotCallbackQuery("cb_cancel")
public AnswerCallbackQuery onCancel(CallbackQuery query) {
    return AnswerCallbackQuery.builder()
        .callbackQueryId(query.getId())
        .text("Cancelled")
        .build();
}
```

---

## Custom Locale Resolver

Override `BotLocaleResolver` to use a different strategy (e.g. stored user preference from a database):

```java
@Bean
public BotLocaleResolver botLocaleResolver(UserRepository users) {
    return request -> {
        if (request.getUser() == null) return Locale.ENGLISH;
        return users.findById(request.getUser().getId())
            .map(u -> Locale.forLanguageTag(u.getLanguage()))
            .orElse(Locale.ENGLISH);
    };
}
```

The default resolver reads `user.getLanguageCode()` from the Telegram `User` object and falls back to `telegram.bot.i18n.default-locale`.

---

## LocalizedTemplate — Zero-boilerplate i18n Returns with Mixed Content

`LocalizedTemplate` is a return type that lets handler methods reply with a
**template string** containing message-bundle keys and positional arguments.
The framework resolves everything and sends the resulting text automatically — no
`BotMessageSource` injection required. This is ideal when you need **mixed literal text, multiple message keys, and formatting arguments** in a single response.

### Template syntax

| Token | Meaning |
|-------|---------|
| `${key}` | Resolved from the message bundle in the user's locale via `BotMessageSource` |
| `#{index}` | Replaced with `args[index]` (0-based positional argument) |

### Usage

```java
@BotController
public class GreetingHandler {

    // Single bundle key
    @BotCommand("/help")
    public LocalizedTemplate onHelp() {
        return LocalizedTemplate.of("${help.text}");
    }

    // Key + positional argument
    @BotCommand("/start")
    public LocalizedTemplate onStart(User user) {
        return LocalizedTemplate.of("${welcome} #{0}!", user.getFirstName());
    }

    // Mix of literal text, multiple keys and arguments
    @BotCommand("/info")
    public LocalizedTemplate onInfo(User user) {
        return LocalizedTemplate.of("${hello} #{0}, ${how.are.you}?", user.getFirstName());
    }
}
```

Message bundle:
```properties
# bot.properties
welcome       = Welcome,
hello         = Hello
how.are.you   = how are you
help.text     = Here is the help text.
```

A Russian user calling `/start` with first name "Ivan" receives: `"Добро пожаловать, Ivan!"` — the `${welcome}` key is resolved in their locale, `#{0}` is replaced with the argument.

### Compare with BotMessageSource

```java
// Before — manual injection
@BotCommand("/start")
public String onStart(BotRequest request, User user) {
    return messages.getMessage("welcome", request) + " " + user.getFirstName() + "!";
}

// After — LocalizedTemplate
@BotCommand("/start")
public LocalizedTemplate onStart(User user) {
    return LocalizedTemplate.of("${welcome} #{0}!", user.getFirstName());
}
```

> `LocalizedTemplate` is registered via `BotLocalizedTemplateReturnTypeHandler` in
> `BotI18nAutoConfiguration`. It is available as long as `core-i18n` is on the classpath
> (already included transitively through `spring-boot-starter`).

---

## LocalizedReply — Simple Message-Key Lookup

`LocalizedReply` is a lightweight return type for responses that consist of a **single message-bundle key**
with optional formatting arguments. Use this when your entire response is just one translated message,
without needing mixed template syntax or multiple keys.

Unlike `LocalizedTemplate`, `LocalizedReply` does not support `${key}` token syntax — the key is resolved
directly from the message bundle, and arguments are passed through.

### Usage

```java
@BotController
@RequiredArgsConstructor
public class GreetingHandler {

    // Single key, no arguments
    @BotCommand("/help")
    public LocalizedReply onHelp() {
        return LocalizedReply.of("help.text");
    }

    // Single key with formatting arguments
    @BotCommand("/start")
    public LocalizedReply onStart(User user) {
        return LocalizedReply.of("welcome.message", user.getFirstName());
    }

    // Another simple example
    @BotCommand("/status")
    public LocalizedReply onStatus() {
        return LocalizedReply.of("status.ready");
    }
}
```

Message bundle:
```properties
# bot.properties
help.text         = Here is the help text.
welcome.message   = Welcome, %s!
status.ready      = System is ready to accept commands.
```

```properties
# bot_uz.properties
help.text         = Mana yordam matni.
welcome.message   = Xush kelibsiz, %s!
status.ready      = Tizim buyruqlarni qabul qilishga tayyordir.
```

A Russian user calling `/start` receives: `"Добро пожаловать, Иван!"` (assuming the message is translated).

### When to Use

| Use Case | Best Option |
|----------|-------------|
| Single message key only | `LocalizedReply` |
| Key + arguments | `LocalizedReply` |
| Multiple keys + mixed text | `LocalizedTemplate` |
| Literal text (no i18n) | `PlainTextTemplate` |

> `LocalizedReply` is registered via `BotLocalizedReplyReturnTypeHandler` in
> `BotI18nAutoConfiguration`. It is available as long as `core-i18n` is on the classpath.

---

## PlainTextTemplate — Non-i18n Formatted Text

`PlainTextTemplate` is for responses that should **not be translated**. The template string is used as-is
with standard `String.format()` support if arguments are provided. Use this when you need to send literal text
that doesn't belong in the message bundles.

### Usage

```java
@BotController
public class InfoHandler {

    // Plain literal text
    @BotCommand("/version")
    public PlainTextTemplate onVersion() {
        return PlainTextTemplate.of("Bot version: 1.0.0");
    }

    // With String.format() arguments
    @BotCommand("/debug")
    public PlainTextTemplate onDebug(BotRequest request) {
        return PlainTextTemplate.of(
            "User ID: %d, Chat ID: %d",
            request.getUser().getId(),
            request.getMessage().getChatId()
        );
    }

    // Literal text with dynamic content
    @BotCommand("/show-stats")
    public PlainTextTemplate onShowStats(long count) {
        return PlainTextTemplate.of("Total items processed: %d", count);
    }
}
```

### When to Use

| Use Case | Best Option |
|----------|-------------|
| Translatable message | `LocalizedReply` or `LocalizedTemplate` |
| Debug info, system messages, timestamps | `PlainTextTemplate` |
| Literal text with no formatting | `PlainTextTemplate` |
| User-provided content (IDs, names from DB) | `PlainTextTemplate` |

> `PlainTextTemplate` is registered via `BotPlainTextTemplateReturnTypeHandler` in
> `CoreAutoConfiguration`. It is available in the `core` module and does not require `core-i18n`.

---

## See Also

- [Root README — Internationalisation section](../README.md#internationalisation-i18n)
- [spring-boot-starter/README.md](../spring-boot-starter/README.md) — includes `core-i18n` transitively
