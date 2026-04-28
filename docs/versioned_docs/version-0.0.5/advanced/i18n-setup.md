---
id: i18n-setup
title: Internationalization (i18n)
description: Set up i18n in your Spring Boot Telegram bot with Easygram — BotMessageSource, YAML message files, LocalizedReply locale resolution, and locale-aware keyboard factories.
keywords: [telegram bot i18n setup, spring boot telegram internationalization, BotMessageSource, LocalizedReply, BotKeyboardFactory, multi-language bot setup]
---

# Internationalization Setup

The `core-i18n` module provides locale-aware message lookups, localized return types, localized
keyboards, and automatic `Locale` injection into handler methods — all built on Spring's
`MessageSource` infrastructure.

## 1. Setup

### Add the Dependency

`spring-boot-starter` includes `core-i18n` automatically. For individual modules:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>core-i18n</artifactId>
    <version>0.0.5</version>
</dependency>
```

### Create Message Files

Place locale-specific `.properties` files under `src/main/resources/messages/`:

```
src/main/resources/messages/
 bot.properties ← default fallback
 bot_en.properties ← English
 bot_ru.properties ← Russian
 bot_uz.properties ← Uzbek
```

**`messages/bot.properties`** (default / English fallback):
```properties
welcome=Hello, {0}! Welcome to the bot.
help.text=Available commands:\n/start — start\n/help — this message
profile.info=Name: {0}\nLanguage: {1}
register.step.name=Please enter your name:
register.step.phone=Please share your phone number:
register.done=Registration complete, {0}!
register.cancelled=Registration cancelled.

btn.cancel= Cancel
btn.confirm= Confirm
btn.back= Back
btn.profile= Profile
btn.settings= Settings
btn.help= Help
btn.yes= Yes
btn.no= No
```

**`messages/bot_ru.properties`** (Russian):
```properties
welcome=Привет, {0}! Добро пожаловать.
help.text=Доступные команды:\n/start — начало\n/help — это сообщение
profile.info=Имя: {0}\nЯзык: {1}
register.step.name=Введите ваше имя:
register.step.phone=Поделитесь вашим номером телефона:
register.done=Регистрация завершена, {0}!
register.cancelled=Регистрация отменена.

btn.cancel= Отмена
btn.confirm= Подтвердить
btn.back= Назад
btn.profile= Профиль
btn.settings= Настройки
btn.help= Помощь
btn.yes= Да
btn.no= Нет
```

**`messages/bot_uz.properties`** (Uzbek):
```properties
welcome=Salom, {0}! Xush kelibsiz.
help.text=Mavjud buyruqlar:\n/start — boshlash\n/help — bu xabar
profile.info=Ism: {0}\nTil: {1}
register.step.name=Ismingizni kiriting:
register.step.phone=Telefon raqamingizni yuboring:
register.done=Ro'yxatdan o'tish yakunlandi, {0}!
register.cancelled=Ro'yxatdan o'tish bekor qilindi.

btn.cancel= Bekor qilish
btn.confirm= Tasdiqlash
btn.back= Orqaga
btn.profile= Profil
btn.settings= Sozlamalar
btn.help= Yordam
btn.yes= Ha
btn.no= Yo'q
```

### Configure Spring MessageSource

```yaml
spring:
  messages:
    basename: messages/bot # resolves messages/bot.properties, messages/bot_en.properties …
    encoding: UTF-8
    use-code-as-default-message: false
    cache-duration: 3600 # seconds; omit or set to 0 during development
```

### Set the Default Locale

```yaml
easygram:
  i18n:
    default-locale: en # used when user has no language_code set
```

---

## 2. Return Types

### LocalizedReply

`LocalizedReply` resolves a single **message-bundle key** at dispatch time using the request's
locale. Optional varargs are substituted via `MessageFormat` (`{0}`, `{1}`, …).

```java
import uz.osoncode.easygram.core.i18n.reply.LocalizedReply;

@BotCommand("/start")
public LocalizedReply onStart(User user) {
    // Resolves "welcome" key with user's first name as {0}
    return LocalizedReply.of("welcome", user.getFirstName());
}

@BotCommand("/help")
public LocalizedReply onHelp() {
    return LocalizedReply.of("help.text");
}
```

`LocalizedReply` implements `MarkupAware` — attach a registered keyboard with `.withMarkup()`:

```java
@BotCommand("/menu")
public LocalizedReply onMenu() {
    return LocalizedReply.of("choose.option").withMarkup("main_menu");
}

// Remove the keyboard after the reply:
@BotCommand("/done")
public LocalizedReply onDone() {
    return LocalizedReply.of("action.done").removeMarkup();
}
```

### LocalizedTemplate

`LocalizedTemplate` resolves a **template string** that can mix two token types:

| Token | Source | Example |
|---|---|---|
| `${key}` | Resolved from message bundle (Spring `MessageSource`) | `${welcome.title}` |
| `#{index}` | Replaced with positional `args[index]` (0-based) | `#{0}` |

```java
import uz.osoncode.easygram.core.i18n.reply.LocalizedTemplate;

@BotCommand("/profile")
public LocalizedTemplate onProfile(User user) {
    // ${profile.header} is resolved from the bundle; #{0} is user.getFirstName()
    return LocalizedTemplate.of("${profile.header}\n\n#{0} registered!", user.getFirstName());
}
```

A more complex example combining multiple bundle keys and positional args:

```java
@BotCommand("/stats")
public LocalizedTemplate onStats(User user) {
    long count = orderService.countByUser(user.getId());
    // Mixes bundle keys and runtime values in one template
    return LocalizedTemplate.of(
        "${stats.title}\n\n${stats.orders.label}: #{0}\n${stats.user.label}: #{1}",
        count,
        user.getFirstName()
    );
}
```

---

## 3. BotMessageSource — Direct Usage

Inject `BotMessageSource` when you need to resolve messages imperatively (e.g., inside service
logic or before building a `BotApiMethod`).

```java
@BotController
@RequiredArgsConstructor
public class InfoController {

    private final BotMessageSource messageSource;

    @BotCommand("/start")
    public String onStart(User user, BotRequest request) {
        // Auto-resolves locale from the request
        return messageSource.getMessage("welcome", request, user.getFirstName());
    }

    @BotCommand("/help")
    public String onHelp(Locale locale) {
        // Explicit locale
        return messageSource.getMessage("help.text", locale);
    }

    @BotCommand("/info")
    public String onInfo(BotRequest request) {
        // With fallback — never throws NoSuchMessageException
        return messageSource.getOrDefault("info.text", "No info available.", request);
    }
}
```

#### BotMessageSource method signatures

| Method | Description |
|---|---|
| `getMessage(String code, BotRequest request, Object... args)` | Resolves locale from the request automatically |
| `getMessage(String code, Locale locale, Object... args)` | Explicit locale |
| `getOrDefault(String code, String defaultMessage, BotRequest request, Object... args)` | Returns `defaultMessage` if the key is missing |
| `resolveLocale(BotRequest request)` | Returns the `Locale` resolved for this request |

---

## 4. BotKeyboardFactory — Localized Keyboards

`BotKeyboardFactory` builds keyboards whose button labels are **message-bundle keys** resolved at
runtime. Every user sees buttons in their own language automatically.

### Declaring Markups

Register keyboards in a `@BotConfiguration` class using `@BotMarkup`:

```java
@BotConfiguration
@RequiredArgsConstructor
public class MyKeyboards {

    private final BotKeyboardFactory keyboardFactory;

    // Reply keyboards

    @BotMarkup("main_menu")
    public ReplyKeyboard mainMenu(BotRequest request) {
        return keyboardFactory.reply(request)
            .row("btn.profile", "btn.settings") // one row with two buttons
            .row("btn.help") // second row
            .resizeKeyboard(true)
            .oneTimeKeyboard(false)
            .build();
    }

    @BotMarkup("cancel_menu")
    public ReplyKeyboard cancelMenu(BotRequest request) {
        return keyboardFactory.reply(request)
            .row("btn.cancel")
            .resizeKeyboard(true)
            .build();
    }

    // Inline keyboards

    @BotMarkup("inline_actions")
    public InlineKeyboardMarkup inlineActions(BotRequest request) {
        // .row() takes alternating (messageKey, callbackData) pairs — must be even count
        return keyboardFactory.inline(request)
            .row("btn.yes", "action:yes", "btn.no", "action:no")
            .build();
    }

    @BotMarkup("settings_inline")
    public InlineKeyboardMarkup settingsInline(BotRequest request) {
        return keyboardFactory.inline(request)
            .row("btn.profile", "settings:profile")
            .row("btn.back", "settings:back")
            .build();
    }
}
```

### ReplyKeyboardBuilder API

| Method | Description |
|---|---|
| `keyboardFactory.reply(BotRequest request)` | Create builder; locale from request |
| `keyboardFactory.reply(Locale locale)` | Create builder; explicit locale |
| `.row(String... messageKeys)` | Add a row of buttons with localized labels |
| `.row(KeyboardButton... buttons)` | Add a row of pre-built buttons |
| `.resizeKeyboard(boolean)` | Resize keyboard (default: `true`) |
| `.oneTimeKeyboard(boolean)` | Hide keyboard after use (default: `false`) |
| `.build()` | Returns `ReplyKeyboardMarkup` |

### InlineKeyboardBuilder API

| Method | Description |
|---|---|
| `keyboardFactory.inline(BotRequest request)` | Create builder; locale from request |
| `keyboardFactory.inline(Locale locale)` | Create builder; explicit locale |
| `.row(String... textAndCallbackPairs)` | Alternating `(messageKey, callbackData)` pairs; must be even count |
| `.row(InlineKeyboardButton... buttons)` | Add pre-built inline buttons |
| `.build()` | Returns `InlineKeyboardMarkup` |

### Individual Button Factories

For assembling keyboards dynamically (e.g., generated from a database):

```java
// Single reply button
KeyboardButton cancelBtn = keyboardFactory.replyButton("btn.cancel", request);
KeyboardButton cancelBtn = keyboardFactory.replyButton("btn.cancel", locale);

// Single inline button
InlineKeyboardButton yesBtn = keyboardFactory.inlineButton("btn.yes", "action:yes", request);
InlineKeyboardButton yesBtn = keyboardFactory.inlineButton("btn.yes", "action:yes", locale);

// Assemble inline keyboard from rows
InlineKeyboardRow row1 = keyboardFactory.inlineRow(yesBtn, noBtn);
InlineKeyboardMarkup markup = keyboardFactory.inlineMarkup(row1);
```

---

## 5. Locale Injection in Handlers

Declare `Locale` as a handler parameter and the framework injects the resolved locale
automatically via `BotLocaleArgumentResolver`:

```java
@BotController
@RequiredArgsConstructor
public class ProfileController {

    private final BotMessageSource messageSource;

    @BotCommand("/start")
    public LocalizedReply onStart(User user, Locale locale) {
        // locale is the resolved locale for this user
        log.info("User {} connected with locale {}", user.getId(), locale);
        return LocalizedReply.of("welcome", user.getFirstName());
    }

    @BotCommand("/profile")
    public String onProfile(User user, Locale locale) {
        return messageSource.getMessage("profile.info", locale,
            user.getFirstName(),
            locale.getDisplayLanguage(locale));
    }
}
```

`Locale` can appear anywhere in the parameter list alongside `User`, `Chat`, `BotRequest`, etc.

---

## 6. @BotReplyButton with i18n Keys

When `core-i18n` is on the classpath, `@BotReplyButton` values are treated as **message-bundle
keys** rather than literal strings. The framework resolves each key and matches the incoming
reply-button text against all known localized variants.

This means a single handler covers all languages simultaneously:

```java
@BotController
public class RegistrationController {

    // btn.cancel resolves to " Cancel" / " Отмена" / " Bekor qilish" etc.
    @BotReplyButton("btn.cancel")
    @BotClearChatState
    public LocalizedReply onCancel() {
        return LocalizedReply.of("register.cancelled").removeMarkup();
    }

    @BotReplyButton("btn.confirm")
    public LocalizedReply onConfirm(BotRequest request) {
        return LocalizedReply.of("action.confirmed");
    }
}
```

---

## 7. @BotInlineQuery with i18n Keys

The same i18n pattern applies to `@BotInlineQuery`. When `core-i18n` is on the classpath,
`value` entries are treated as **message-bundle keys** — resolved per user locale before
being compared against the incoming inline query text.

```properties
# messages/bot_en.properties
inline.search=search
inline.order=order

# messages/bot_ru.properties
inline.search=поиск
inline.order=заказ
```

```java
@BotController
public class InlineController {

    // matches "@YourBot search" for EN users, "@YourBot поиск" for RU users
    @BotInlineQuery("inline.search")
    public void onSearch(InlineQuery query, @BotInlineQueryValue String text) {
        List<InlineQueryResult> results = catalogService.search(text);
        bot.execute(AnswerInlineQuery.builder()
            .inlineQueryId(query.getId())
            .results(results)
            .build());
    }

    @BotInlineQuery("inline.order")
    public void onOrder(InlineQuery query, @BotInlineQueryValue String text) {
        // handles "order" / "заказ" depending on user's language
    }
}
```

Empty `@BotInlineQuery` (no `value`) still matches all queries regardless of locale.

---

## 8. Custom BotLocaleResolver

By default, the locale is derived from the Telegram user's `language_code` field. Override this
by declaring a `BotLocaleResolver` bean — for example, to persist user language preferences in
a database:

```java
@Configuration
@RequiredArgsConstructor
public class LocaleConfig {

    private final UserLanguageRepository userLanguageRepository;

    @Bean
    public BotLocaleResolver botLocaleResolver() {
        return request -> {
            Long userId = request.getUser().getId();
            String langCode = userLanguageRepository.findLanguageByUserId(userId);
            return Locale.forLanguageTag(langCode != null ? langCode : "en");
        };
    }
}
```

The `BotLocaleResolver` bean is picked up automatically — no additional registration needed.

---

## 9. Full Configuration Reference

```yaml
easygram:
  i18n:
    default-locale: en # Fallback locale when language_code is absent

spring:
  messages:
    basename: messages/bot # Base name — resolves bot.properties, bot_en.properties …
    encoding: UTF-8 # Always UTF-8 for non-ASCII languages
    use-code-as-default-message: false
    cache-duration: 3600 # Cache in production; omit or 0 in development
```

### Locale Resolution Order

1. `BotLocaleResolver` bean (if declared — overrides everything)
2. Telegram user's `language_code` BCP 47 tag (`"ru"`, `"en"`, `"en-US"`)
3. `easygram.i18n.default-locale` property
4. `"en"` hard fallback

Both `"en"` and `"en-US"` resolve to `bot_en.properties`. Completely unknown codes fall back to
`bot.properties`.

---

## 10. Real-World Example — Registration Wizard

A multi-step registration wizard with locale-aware keyboards and reply buttons.

### Keyboards

```java
@BotConfiguration
@RequiredArgsConstructor
public class RegistrationKeyboards {

    private final BotKeyboardFactory keyboardFactory;

    @BotMarkup("reg_cancel")
    public ReplyKeyboard cancelKeyboard(BotRequest request) {
        return keyboardFactory.reply(request)
            .row("btn.cancel")
            .resizeKeyboard(true)
            .build();
    }

    @BotMarkup("reg_confirm")
    public ReplyKeyboard confirmKeyboard(BotRequest request) {
        return keyboardFactory.reply(request)
            .row("btn.confirm", "btn.cancel")
            .resizeKeyboard(true)
            .build();
    }

    @BotMarkup("main_menu")
    public ReplyKeyboard mainMenu(BotRequest request) {
        return keyboardFactory.reply(request)
            .row("btn.profile", "btn.settings")
            .row("btn.help")
            .resizeKeyboard(true)
            .build();
    }
}
```

### Wizard Controller

```java
@BotController
public class RegistrationController {

    // Entry point

    @BotCommand("/register")
    @BotForwardChatState("ENTER_NAME")
    public LocalizedReply startRegistration() {
        return LocalizedReply.of("register.step.name").withMarkup("reg_cancel");
    }

    // Step 1: collect name

    @BotText
    @BotChatState("ENTER_NAME")
    @BotForwardChatState("ENTER_PHONE")
    public LocalizedReply onName(BotRequest request, @BotTextValue String name) {
        request.getAttributes().put("reg.name", name);
        return LocalizedReply.of("register.step.phone").withMarkup("reg_cancel");
    }

    // Step 2: collect phone

    @BotContact
    @BotChatState("ENTER_PHONE")
    @BotForwardChatState("CONFIRM")
    public LocalizedTemplate onPhone(BotRequest request) {
        String phone = request.getUpdate().getMessage().getContact().getPhoneNumber();
        request.getAttributes().put("reg.phone", phone);
        // Template mixes bundle key ${register.confirm.prompt} with runtime value #{0}
        return LocalizedTemplate.of("${register.confirm.prompt}\n\n#{0}", phone);
    }

    // Confirmation

    @BotReplyButton("btn.confirm")
    @BotChatState("CONFIRM")
    @BotClearChatState
    public LocalizedReply onConfirm(BotRequest request, User user) {
        String name = (String) request.getAttributes().get("reg.name");
        // Save to database …
        return LocalizedReply.of("register.done", name).withMarkup("main_menu");
    }

    // Cancel at any state

    @BotReplyButton("btn.cancel")
    @BotClearChatState
    public LocalizedReply onCancel() {
        return LocalizedReply.of("register.cancelled").removeMarkup();
    }
}
```

### Adding a New Language

1. Create `src/main/resources/messages/bot_de.properties` and translate all keys.
2. No code changes required — the framework resolves the locale at runtime.

---

## Production Recommendations

- Set `spring.messages.cache-duration` to avoid file re-reads on every request.
- Always use `UTF-8` encoding for properties files containing non-ASCII characters.
- For languages with special character sets or text direction (Arabic, Japanese), test keyboard and message layout manually.
- Use a database-backed `BotLocaleResolver` when users can switch language inside the bot.

---

See also:
- [Parameter Injection](../core-concepts/parameter-injection) — `Locale` injection details
- [Return Types](../core-concepts/return-types) — `LocalizedReply` and `LocalizedTemplate`
- [Markup & Keyboards](./markup-system) — `@BotMarkup` and `@BotConfiguration`
- [Custom Argument Resolvers](./custom-argument-resolvers) — extend parameter injection
