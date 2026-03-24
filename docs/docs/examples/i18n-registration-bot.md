---
id: i18n-registration-bot
title: i18n Registration Bot
---

# i18n Registration Bot

This example walks through a **multi-step registration wizard** that fully supports internationalization (i18n). The bot speaks English, Uzbek, and Russian — the reply language is resolved automatically from the user's Telegram language settings.

You can find the complete source in [`samples/i18n-registration-bot`](https://github.com/osoncode/easygram/tree/main/samples/i18n-registration-bot).

---

## Features demonstrated

| Feature | Where used |
|---|---|
| `LocalizedReply` | Every step prompt, error, and cancellation message |
| `LocalizedTemplate` | `/start` welcome message and registration summary |
| `BotKeyboardFactory` | Locale-aware cancel + share-phone keyboards |
| `@BotReplyButton` with bundle key | Cancel button auto-matched in all languages |
| `@BotTextPattern` + `@BotTextDefault` | Declarative phone-number routing without `if/else` |
| `@BotContact` | Receiving a Telegram shared contact |
| Class-level `@BotChatState` | Restrict all wizard handlers to active registration states |
| `@BotForwardChatState` / `@BotClearChatState` | Wizard state transitions |
| `@NotBlank` / `@Size` on `@BotTextValue` params | Jakarta Bean Validation on name and city input |
| `@BotExceptionHandler(ConstraintViolationException.class)` | Friendly error reply when constraints fail |

---

## Registration flow

```
/register
    
      state: AWAITING_NAME
collectName (@BotTextDefault) → AWAITING_PHONE
    
      state: AWAITING_PHONE
collectPhone (@BotTextPattern E.164) → AWAITING_CITY (valid typed phone)
collectPhone (@BotContact) → AWAITING_CITY (Telegram shared contact)
invalidPhone (@BotTextDefault fallback) → stays AWAITING_PHONE
    
      state: AWAITING_CITY
collectCity (@BotTextDefault) → (state cleared, wizard done)

At any step:
onCancelButton (@BotReplyButton "btn.cancel") → (state cleared, keyboard removed)
```

---

## Project structure

```
i18n-registration-bot/
 pom.xml
 src/main/
     java/uz/example/i18n/
        I18nRegistrationBotApplication.java
        RegistrationState.java
        RegistrationController.java
        GlobalController.java
        markup/
            RegistrationMarkups.java
     resources/
         application.yml
         messages/
             bot.properties ← default / English fallback
             bot_en.properties
             bot_uz.properties
             bot_ru.properties
```

---

## Step 1 — Dependencies (`pom.xml`)

The sample uses **`longpolling`** (which pulls in `core`, `core-api`, and `core-chatstate`) plus **`core-i18n`** for i18n support.

```xml
<dependencies>
    <!--
        longpolling transitively provides:
          - core (dispatcher, filter chain, return-type handlers)
          - core-api (annotations, interfaces, models)
          - core-chatstate (InMemoryBotChatStateService)
    -->
    <dependency>
        <groupId>uz.osoncode.easygram</groupId>
        <artifactId>longpolling</artifactId>
        <version>0.0.2</version>
    </dependency>

    <!-- i18n: BotMessageSource, BotKeyboardFactory, LocalizedReply, LocalizedTemplate -->
    <dependency>
        <groupId>uz.osoncode.easygram</groupId>
        <artifactId>core-i18n</artifactId>
        <version>0.0.2</version>
    </dependency>
</dependencies>
```

:::tip Spring Boot Starter
Alternatively, use the `spring-boot-starter` artifact to pull in all transports and `core-i18n` in one dependency:

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.2</version>
</dependency>
```
:::

---

## Step 2 — Configuration (`application.yml`)

```yaml
telegram:
  bot:
    # Required: your Bot API token from @BotFather
    token: ${TELEGRAM_BOT_TOKEN}
    # Transport defaults to LONG_POLLING — no need to set it explicitly

spring:
  application:
    name: i18n-registration-bot

  messages:
    basename: messages/bot # loads messages/bot*.properties
    encoding: UTF-8
    use-code-as-default-message: false
```

The `messages.basename` tells Spring where to find your bundles. The framework's built-in `UserLanguageCodeLocaleResolver` automatically picks the right locale from the user's Telegram `languageCode` field — no extra wiring needed.

---

## Step 3 — Registration state enum

Using an `enum` instead of raw strings gives you compile-time safety and IDE autocompletion. `BotChatStateService` has dedicated `setState(Long, Enum)` and `getStateAs(Long, Class<E>)` overloads that work with it directly.

```java
package uz.example.i18n;

/**
 * Represents the three stages of the user registration wizard.
 *
 * Flow:
 * /register → AWAITING_NAME → AWAITING_PHONE → AWAITING_CITY → (cleared)
 */
public enum RegistrationState {

    /** Waiting for the user's full name. */
    AWAITING_NAME,

    /** Waiting for a valid phone number. */
    AWAITING_PHONE,

    /** Waiting for the user's city. */
    AWAITING_CITY
}
```

---

## Step 4 — Message bundles

Create one properties file per locale under `src/main/resources/messages/`.

### `messages/bot_en.properties`

```properties
# --- Welcome ---
welcome.title= Welcome, {0}!
welcome.body=I am a registration bot with full i18n support.\n\nI speak English, Ўзбекча and Русский — my reply language is detected from your Telegram profile.
welcome.commands=Commands:\n /register — start the registration wizard\n /status — check wizard progress\n /cancel — cancel the wizard at any step

# --- Registration wizard ---
register.start= Let's get you registered!\n\nStep 1/3 — What is your full name?
register.name.saved= Name saved: {0}\n\nStep 2/3 — Please enter your phone number.\nFormat: +XXXXXXXXXXXX (include country code)
register.phone.invalid= Invalid phone number. Please use international format, e.g. +998901234567
register.phone.saved= Phone saved: {0}\n\nStep 3/3 — Which city do you live in?
register.complete= Registration complete!\n\nYour details:\n• Name: #{0}\n• Phone: #{1}\n• City: #{2}\n\nUse /register to update your profile.
register.cancelled= Registration cancelled. Use /register to start again.

# --- Status ---
status.none=ℹ No active registration wizard. Use /register to start one.
status.awaiting_name= Wizard in progress — waiting for your name (step 1/3).
status.awaiting_phone= Wizard in progress — waiting for your phone number (step 2/3).
status.awaiting_city= Wizard in progress — waiting for your city (step 3/3).

# --- Cancel command ---
cancel.none=ℹ There is no active wizard to cancel.
cancel.done= Registration wizard cancelled. Use /register to start again.

# --- Fallback ---
error.unknown=I didn't understand that.\nUse /register to start, /status to check progress, or /cancel to abort.

# --- Keyboard buttons ---
btn.cancel= Cancel
btn.send.phone= Share phone number
```

### `messages/bot_ru.properties`

```properties
# --- Приветствие ---
welcome.title= Добро пожаловать, {0}!
welcome.body=Я бот регистрации с полной поддержкой i18n.\n\nЯзык ответов определяется автоматически по настройкам вашего Telegram.
welcome.commands=Команды:\n /register — начать регистрацию\n /status — статус анкеты\n /cancel — отменить

# --- Регистрация ---
register.start= Начнём регистрацию!\n\nШаг 1/3 — Введите ваше полное имя:
register.name.saved= Имя сохранено: {0}\n\nШаг 2/3 — Введите номер телефона.\nФормат: +XXXXXXXXXXXX (с кодом страны)
register.phone.invalid= Неверный формат номера. Используйте международный формат, например: +79001234567
register.phone.saved= Телефон сохранён: {0}\n\nШаг 3/3 — В каком городе вы живёте?
register.complete= Регистрация завершена!\n\nВаши данные:\n• Имя: #{0}\n• Телефон: #{1}\n• Город: #{2}\n\nДля обновления используйте /register.
register.cancelled= Регистрация отменена. Для начала используйте /register.

# --- Статус ---
status.none=ℹ Активной регистрации нет. Начните с /register.
status.awaiting_name= Регистрация в процессе — ожидаю ваше имя (шаг 1/3).
status.awaiting_phone= Регистрация в процессе — ожидаю номер телефона (шаг 2/3).
status.awaiting_city= Регистрация в процессе — ожидаю название города (шаг 3/3).

# --- Отмена ---
cancel.none=ℹ Нет активной регистрации для отмены.
cancel.done= Регистрация отменена. Начните заново с /register.

# --- Стандартный ответ ---
error.unknown=Я не понял вашего сообщения.\n/register — начать, /status — статус, /cancel — отменить.

# --- Кнопки ---
btn.cancel= Отмена
btn.send.phone= Поделиться номером
```

### `messages/bot_uz.properties`

```properties
# --- Xush kelibsiz ---
welcome.title= Xush kelibsiz, {0}!
welcome.body=Men ro'yxatdan o'tkazish boti bo'lib, to'liq i18n qo'llab-quvvatlashga egaman.\n\nTil Telegram profilingizdagi til sozlamalaridan avtomatik aniqlanadi.
welcome.commands=Buyruqlar:\n /register — ro'yxatdan o'tish\n /status — jarayon holati\n /cancel — bekor qilish

# --- Ro'yxatdan o'tish ---
register.start= Ro'yxatdan o'tamiz!\n\n1/3-qadam — To'liq ismingizni kiriting:
register.name.saved= Ism saqlandi: {0}\n\n2/3-qadam — Telefon raqamingizni kiriting.\nFormat: +XXXXXXXXXXXX (mamlakat kodi bilan)
register.phone.invalid= Noto'g'ri telefon raqami. Xalqaro formatdan foydalaning, masalan: +998901234567
register.phone.saved= Telefon saqlandi: {0}\n\n3/3-qadam — Qaysi shaharda yashaysiz?
register.complete= Ro'yxatdan o'tish yakunlandi!\n\nMa'lumotlaringiz:\n• Ism: #{0}\n• Telefon: #{1}\n• Shahar: #{2}\n\nMa'lumotlarni yangilash uchun /register buyrug'ini ishlating.
register.cancelled= Ro'yxatdan o'tish bekor qilindi. Qayta boshlash uchun /register.

# --- Holat xabarlari ---
status.none=ℹ Faol jarayon yo'q. Boshlash uchun /register.
status.awaiting_name= Jarayon davom etmoqda — ismingiz kutilmoqda (1/3-qadam).
status.awaiting_phone= Jarayon davom etmoqda — telefon raqamingiz kutilmoqda (2/3-qadam).
status.awaiting_city= Jarayon davom etmoqda — shahringiz kutilmoqda (3/3-qadam).

# --- Bekor qilish ---
cancel.none=ℹ Bekor qilish uchun faol jarayon yo'q.
cancel.done= Ro'yxatdan o'tish bekor qilindi. Qayta boshlash uchun /register.

# --- Standart javob ---
error.unknown=Tushunmadim.\n/register — boshlash, /status — holat, /cancel — bekor qilish.

# --- Tugmalar ---
btn.cancel= Bekor qilish
btn.send.phone= Telefon raqamni ulashing
```

:::note Default bundle
`bot.properties` (no locale suffix) acts as the English fallback. Spring MessageSource falls back to this file when no locale-specific bundle matches.
:::

---

## Step 5 — Locale-aware keyboards (`RegistrationMarkups`)

Keyboards are declared in a `@BotConfiguration` class using `@BotMarkup`-annotated methods. `BotKeyboardFactory` resolves button labels from the message bundle using the request's locale, so button text is automatically translated.

```java
package uz.example.i18n.markup;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import uz.osoncode.easygram.core.annotation.BotConfiguration;
import uz.osoncode.easygram.core.annotation.BotMarkup;
import uz.osoncode.easygram.core.i18n.keyboard.BotKeyboardFactory;
import uz.osoncode.easygram.core.model.BotRequest;

@BotConfiguration
@RequiredArgsConstructor
public class RegistrationMarkups {

    private final BotKeyboardFactory keyboardFactory;

    /**
     * "kb_cancel" — single cancel button, shown at every wizard step.
     *
     * The button label resolves to the user's locale:
     * en → Cancel
     * uz → Bekor qilish
     * ru → Отмена
     */
    @BotMarkup("kb_cancel")
    public ReplyKeyboard cancelKeyboard(BotRequest request) {
        return keyboardFactory.reply(request)
                .row("btn.cancel")
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }

    /**
     * "kb_send_phone" — share-contact button, shown at the AWAITING_PHONE step.
     *
     * setRequestContact(true) makes Telegram display the native contact-sharing dialog.
     * The button label resolves to the user's locale:
     * en → Share phone number
     * uz → Telefon raqamni ulashing
     * ru → Поделиться номером
     */
    @BotMarkup("kb_send_phone")
    public ReplyKeyboard phone(BotRequest botRequest) {
        KeyboardButton keyboardButton = keyboardFactory.replyButton("btn.send.phone", botRequest);
        keyboardButton.setRequestContact(true);
        return keyboardFactory.reply(botRequest)
                .row(keyboardButton)
                .build();
    }
}
```

:::info BotMarkup method signature
`@BotMarkup` methods may take **no parameters** (static keyboard) or a **`BotRequest` parameter** (locale-aware keyboard). The framework detects the signature at startup and injects accordingly.
:::

---

## Step 6 — Registration controller

The core of the wizard. Notice the **class-level `@BotChatState`** — it restricts every handler in this class so that it only fires when the chat is in one of the three registration states.

```java
package uz.example.i18n;

import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.annotation.BotOrder;
import uz.osoncode.easygram.core.bind.annotation.*;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.i18n.LocalizedReply;
import uz.osoncode.easygram.core.i18n.LocalizedTemplate;
import uz.osoncode.easygram.core.stereotype.BotController;

@BotController
@BotChatState({"AWAITING_NAME", "AWAITING_PHONE", "AWAITING_CITY"})
public class RegistrationController {

    // Step 0: entry point

    /**
     * Starts the wizard from ANY chat state — including no state at all.
     *
     * The empty @BotChatState on this method overrides the class-level guard,
     * making /register reachable even when no registration is in progress.
     */
    @BotCommand("/register")
    @BotChatState // empty — overrides class guard, accepts any state
    @BotForwardChatState("AWAITING_NAME")
    @BotReplyMarkup("kb_cancel")
    public LocalizedReply startRegistration(User user) {
        return LocalizedReply.of("register.start");
    }

    // Step 1: collect name

    @BotTextDefault
    @BotChatState("AWAITING_NAME")
    @BotForwardChatState("AWAITING_PHONE")
    @BotReplyMarkup("kb_send_phone")
    public LocalizedReply collectName(@BotTextValue @NotBlank @Size(min = 2, max = 50) String name) {
        // {0} in the bundle string is replaced with the user's name
        return LocalizedReply.of("register.name.saved", name);
    }

    // Step 2a: collect phone — shared contact

    /**
     * Fires when the user taps the "Share phone number" button.
     * Telegram sends a Contact object; we extract the phone number from it.
     */
    @BotContact
    @BotChatState("AWAITING_PHONE")
    @BotForwardChatState("AWAITING_CITY")
    @BotReplyMarkup("kb_cancel")
    public LocalizedReply collectPhone(Contact contact) {
        return LocalizedReply.of("register.phone.saved", contact.getPhoneNumber());
    }

    // Step 2b: collect phone — typed E.164 number

    /**
     * Fires only when the user types a valid international phone number.
     *
     * @BotTextPattern matches before @BotTextDefault (the fallback below),
     * so no if/else is needed in application code.
     */
    @BotTextPattern("^\\+\\d{7,15}$")
    @BotChatState("AWAITING_PHONE")
    @BotForwardChatState("AWAITING_CITY")
    @BotReplyMarkup("kb_cancel")
    public LocalizedReply collectPhone(@BotTextValue String phone) {
        return LocalizedReply.of("register.phone.saved", phone);
    }

    // Step 2c: invalid phone — fallback

    /**
     * Catches any text that did NOT match the @BotTextPattern above.
     * State stays at AWAITING_PHONE (no @BotForwardChatState).
     */
    @BotTextDefault
    @BotChatState("AWAITING_PHONE")
    @BotReplyMarkup("kb_send_phone")
    public LocalizedReply invalidPhone() {
        return LocalizedReply.of("register.phone.invalid");
    }

    // Step 3: collect city

    /**
     * Receives the city name and completes the wizard.
     *
     * LocalizedTemplate demonstrates mixed ${key} + #{n} syntax:
     * ${register.complete} → resolved from the bundle
     * #{0}, #{1}, #{2} → positional args (name, phone, city)
     *
     * In a real app, name and phone would come from a database or session;
     * here they are represented by placeholder strings for brevity.
     */
    @BotTextDefault
    @BotChatState("AWAITING_CITY")
    @BotClearChatState
    @BotClearMarkup
    public LocalizedTemplate collectCity(@BotTextValue @NotBlank @Size(min = 2, max = 50) String city) {
        return LocalizedTemplate.of("${register.complete}", "(saved)", "(saved)", city);
    }

    // Validation error handler

    /**
     * Catches ConstraintViolationException thrown before the handler body runs when
     * @NotBlank / @Size constraints on collectName or collectCity fail.
     *
     * The framework's MethodInvocationFilter calls ExecutableValidator.validateParameters()
     * after argument resolution and throws ConstraintViolationException immediately,
     * so the handler method body is never entered.
     */
    @BotExceptionHandler(ConstraintViolationException.class)
    public LocalizedReply onValidationError(ConstraintViolationException ex) {
        String violations = ex.getConstraintViolations().stream()
                .map(v -> "• " + v.getMessage())
                .collect(Collectors.joining("\n"));
        return LocalizedReply.of("error.validation", violations);
    }

    // Cancel button

    /**
     * The "btn.cancel" bundle key is resolved in the user's locale at match time,
     * so this single annotation covers Cancel / Bekor qilish / Отмена.
     */
    @BotReplyButton("btn.cancel")
    @BotClearChatState
    @BotClearMarkup
    public LocalizedReply onCancelButton() {
        return LocalizedReply.of("register.cancelled");
    }
}
```

---

## Step 7 — Global controller

Global commands available at **any** chat state. No class-level `@BotChatState` means these handlers always fire regardless of wizard progress.

```java
package uz.example.i18n;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.bind.annotation.BotClearChatState;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.i18n.LocalizedReply;
import uz.osoncode.easygram.core.i18n.LocalizedTemplate;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.Locale;

@BotController
@RequiredArgsConstructor
public class GlobalController {

    private final BotChatStateService chatStateService;

    /**
     * /start — welcome message using LocalizedTemplate.
     *
     * The template mixes ${key} bundle lookups with #{0} positional args.
     * #{0} is replaced with the user's first name.
     */
    @BotCommand("/start")
    public LocalizedTemplate onStart(User user) {
        return LocalizedTemplate.of(
                "${welcome.title}\n\n${welcome.body}\n\n${welcome.commands}",
                user.getFirstName() // #{0}
        );
    }

    /**
     * /status — shows wizard progress using LocalizedReply.
     *
     * getStateAs() deserialises the stored string back to the RegistrationState enum.
     */
    @BotCommand("/status")
    public LocalizedReply onStatus(User user) {
        RegistrationState state =
                chatStateService.getStateAs(user.getId(), RegistrationState.class);
        if (state == null) {
            return LocalizedReply.of("status.none");
        }
        return switch (state) {
            case AWAITING_NAME -> LocalizedReply.of("status.awaiting_name");
            case AWAITING_PHONE -> LocalizedReply.of("status.awaiting_phone");
            case AWAITING_CITY -> LocalizedReply.of("status.awaiting_city");
        };
    }

    /**
     * /cancel — aborts the wizard from any step.
     *
     * Locale is injected directly as a parameter — useful for conditional logic
     * or logging, not just for the return value.
     * @BotClearChatState resets the state automatically after return.
     */
    @BotCommand("/cancel")
    @BotClearChatState
    public LocalizedReply onCancel(User user, Locale locale) {
        String current = chatStateService.getState(user.getId());
        if (current == null) {
            return LocalizedReply.of("cancel.none");
        }
        // locale is available here for logging or conditional logic
        return LocalizedReply.of("cancel.done");
    }

    /**
     * Catch-all for any update not matched by a more specific handler.
     */
    @BotDefaultHandler
    public LocalizedReply onUnknown(BotRequest request) {
        return LocalizedReply.of("error.unknown");
    }
}
```

---

## Step 8 — Application entry point

```java
package uz.example.i18n;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class I18nRegistrationBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(I18nRegistrationBotApplication.class, args);
    }
}
```

---

## Key concepts

### Class-level `@BotChatState` scopes the whole controller

Placing `@BotChatState({"AWAITING_NAME", "AWAITING_PHONE", "AWAITING_CITY"})` on `RegistrationController` means **every** handler method inside it is silently skipped if the chat is not currently in one of those states. This keeps the registration logic completely isolated from normal conversation traffic.

To make a single method break out of that restriction (like `/register` which needs to be reachable even with no active state), add an **empty `@BotChatState`** directly on the method — it overrides the class-level guard and accepts any state including none.

### `@BotTextPattern` + `@BotTextDefault` replace `if/else`

Instead of validating phone input inside a single handler, two handlers split the two outcomes:

```java
// Fires only when text matches E.164 — advances the wizard
@BotTextPattern("^\\+\\d{7,15}$")
@BotChatState("AWAITING_PHONE")
@BotForwardChatState("AWAITING_CITY")
public LocalizedReply collectPhone(@BotTextValue String phone) { ... }

// Fires for everything else — sends an error, stays in AWAITING_PHONE
@BotTextDefault
@BotChatState("AWAITING_PHONE")
public LocalizedReply invalidPhone() { ... }
```

The framework tries `@BotTextPattern` first; only unmatched text falls through to `@BotTextDefault`. No branching code needed.

### `@BotReplyButton("btn.cancel")` auto-matches in all languages

With `core-i18n` on the classpath, `@BotReplyButton` values are treated as **message bundle keys**, not literal strings. The framework resolves `btn.cancel` in the user's current locale before comparing it to the incoming text. A single annotation therefore matches:

- ` Cancel` (English)
- ` Bekor qilish` (Uzbek)
- ` Отмена` (Russian)

### `LocalizedReply` vs `LocalizedTemplate`

| Type | Syntax | Use when |
|---|---|---|
| `LocalizedReply` | `LocalizedReply.of("key", args...)` | The whole message is one bundle key. `{0}`, `{1}` are positional args resolved by `MessageFormat`. |
| `LocalizedTemplate` | `LocalizedTemplate.of("${key1} text ${key2}", args...)` | You need to **compose** multiple bundle keys into one string, or mix static text with keys. `${key}` is replaced with the resolved bundle value; `#{n}` is replaced with a positional arg. |

Example — `/start` uses `LocalizedTemplate` because the welcome message is assembled from three separate bundle keys:

```java
return LocalizedTemplate.of(
    "${welcome.title}\n\n${welcome.body}\n\n${welcome.commands}",
    user.getFirstName() // replaces #{0} inside any bundle value that contains it
);
```

The `welcome.title` bundle value itself can contain `{0}` (standard `MessageFormat` placeholder), which gets replaced with `user.getFirstName()` passed as `#{0}`.

### Locale injection

`Locale` can be injected directly as a handler parameter whenever you need it for logic beyond just the return value:

```java
@BotCommand("/cancel")
@BotClearChatState
public LocalizedReply onCancel(User user, Locale locale) {
    // locale resolved from user's Telegram languageCode
    log.debug("Cancel command from locale: {}", locale);
    ...
}
```

The locale is resolved by the built-in `BotLocaleArgumentResolver` using `UserLanguageCodeLocaleResolver`. No configuration required.

### Jakarta Bean Validation on wizard inputs

The `collectName` and `collectCity` handlers annotate their `@BotTextValue String` parameter
with Jakarta constraint annotations:

```java
public LocalizedReply collectName(
        @BotTextValue @NotBlank @Size(min = 2, max = 50) String name) { ... }

public LocalizedTemplate collectCity(
        @BotTextValue @NotBlank @Size(min = 2, max = 50) String city) { ... }
```

The framework's `MethodInvocationFilter` calls `ExecutableValidator.validateParameters()`
**after** argument resolution and **before** the method body runs. If any constraint is violated
a `ConstraintViolationException` is thrown and the exception handler below takes over:

```java
@BotExceptionHandler(ConstraintViolationException.class)
public LocalizedReply onValidationError(ConstraintViolationException ex) {
    String violations = ex.getConstraintViolations().stream()
            .map(v -> "• " + v.getMessage())
            .collect(Collectors.joining("\n"));
    return LocalizedReply.of("error.validation", violations);
}
```

This means the wizard never receives a 1-character "name" like `"A"` — the constraint
fires first and the reply is sent in the user's locale.

:::tip No explicit `@Validated` needed
Unlike Spring MVC, you do **not** need `@Validated` on the controller class. Easygram's
`MethodInvocationFilter` validates parameters unconditionally when a `Validator` bean is
present. `spring-boot-starter-validation` is already a transitive dependency of the
`longpolling` module.
:::

---



```bash
# 1. Build and install the library modules
mvn clean install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true

# 2. Run the sample
cd samples/i18n-registration-bot
TELEGRAM_BOT_TOKEN=<your-token> mvn spring-boot:run
```

Or set the token directly in `application.yml` (for local development only — don't commit tokens).

Once running, open a chat with your bot and try:

| Command / input | Expected behaviour |
|---|---|
| `/start` | Localised welcome in your language |
| `/register` | Starts wizard, shows step 1 prompt |
| Type your name (≥2 chars) | Advances to step 2, shows phone keyboard |
| Type `A` (1 char, at name step) | ` Invalid input: • size must be between 2 and 50` |
| Tap "Share phone number" | Advances to step 3 via shared contact |
| Type `+998901234567` | Advances to step 3 via typed phone |
| Type `abc` (at phone step) | Error message, stays on step 2 |
| Type your city (≥2 chars) | Completes wizard, shows summary |
| Type `X` (1 char, at city step) | ` Invalid input: • size must be between 2 and 50` |
| Tap "Cancel" button | Cancels wizard from any step |
| `/status` | Shows current wizard step |
| `/cancel` | Cancels wizard via command |

---

See also:
- [Jakarta Bean Validation](../advanced/validation) — full constraint reference and custom validator guide
- [i18n Setup](../advanced/i18n-setup) — LocalizedReply, LocalizedTemplate, and BotKeyboardFactory
- [Chat State](../core-concepts/chat-state) — `@BotChatState`, `@BotForwardChatState`, and state persistence
- [Exception Handling](../core-concepts/exception-handling) — `@BotExceptionHandler` reference
