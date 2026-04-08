---
id: custom-argument-resolvers
title: Custom Argument Resolvers
description: Inject custom types into Telegram bot handler parameters by implementing BotArgumentResolver — resolve session data, user profiles, and domain objects directly from context.
keywords: [BotArgumentResolver, custom parameter injection telegram bot, spring boot bot custom resolver, easygram argument resolver, inject domain object telegram]
---

# Custom Argument Resolvers

Argument resolvers let you inject arbitrary objects into handler method parameters. Instead of
calling a service inside every handler, declare a parameter and have the framework produce its
value automatically — for every handler that needs it.

---

## The `BotArgumentResolver` Interface

```java
import java.lang.reflect.Parameter;

public interface BotArgumentResolver {

    /**
     * Called once at startup for each handler parameter.
     * Return true to claim ownership of this parameter.
     */
    boolean supportsParameter(Parameter parameter);

    /**
     * Called at invocation time. Return the resolved value (may be null).
     * Only called when supportsParameter returned true.
     */
    Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse);
}
```

:::caution Java reflection — not Spring's MethodParameter
`Parameter` here is `java.lang.reflect.Parameter`, **not** Spring's `MethodParameter`.

| Spring | Standard Java |
|---|---|
| `parameter.getParameterType()` | `parameter.getType()` |
| `parameter.hasParameterAnnotation(A.class)` | `parameter.isAnnotationPresent(A.class)` |
:::

---

## How the Factory Works

`BotArgumentResolverFactory` auto-collects every `BotArgumentResolver` bean via
`List<BotArgumentResolver>` injection. For each method parameter it streams through the list and
returns the value from the **first matching resolver**. Unmatched parameters receive `null`.

Use `@Order(n)` to control priority — **lower value = higher priority**. Built-in resolvers carry
no explicit order, so any `@Order` value below `Integer.MAX_VALUE` takes precedence over them.

```java
@Component
@Order(10) // runs before unordered built-in resolvers
public class MyResolver implements BotArgumentResolver { ... }
```

---

## Built-in Resolvers

All 16 resolvers are registered automatically by `CoreAutoConfiguration`.

| Resolver class | Resolved type / annotation | Notes |
|---|---|---|
| `BotUpdateArgumentResolver` | `Update` | Raw Telegram update object |
| `BotUserArgumentResolver` | `User` | Message sender (set by `BotContextSetterFilter`) |
| `BotChatArgumentResolver` | `Chat` | Chat context (set by `BotContextSetterFilter`) |
| `BotRequestArgumentResolver` | `BotRequest` | Current request object |
| `BotResponseArgumentResolver` | `BotResponse` | Mutable response accumulator |
| `BotTelegramClientArgumentResolver` | `TelegramClient` | Telegram API client |
| `BotMetadataArgumentResolver` | `BotMetadata` | Bot info: `id`, `username`, `token` |
| `BotThrowableArgumentResolver` | `Throwable` (or any subclass) | Exception — only populated in `@BotExceptionHandler` |
| `BotCallbackQueryDataArgumentResolver` | `@BotCallbackQueryData String` | Callback query data string |
| `BotCommandArgumentResolver` | `@BotCommandValue String` | Matched command, e.g. `/start` |
| `BotTextArgumentResolver` | `@BotTextValue String` | Full message text |
| `BotCommandQueryParamBotArgumentResolver` | `@BotCommandQueryParam T` | **Positional** second token after command, converted via Jackson |
| `BotContactArgumentResolver` | `Contact` | Contact — only in `@BotContact` handlers |
| `BotLocationArgumentResolver` | `Location` | Location — only in `@BotLocation` handlers |
| `BotMarkupContextArgumentResolver` | `BotMarkupContext` | Dynamic markup params — available **only** in `@BotMarkup` factory methods |
| `BotLocaleArgumentResolver` *(core-i18n)* | `Locale` | User locale — requires `core-i18n` on classpath |

### Notes on `@BotCommandQueryParam`

`@BotCommandQueryParam` extracts the **second whitespace-separated token** from the command
message text and converts it to the parameter type using Jackson's `ObjectMapper.convertValue`.
Only **one** such parameter is supported per method — it is purely positional, not named.

```java
// Message text: "/search 42"
@BotCommand("/search")
public String onSearch(@BotCommandQueryParam Integer productId) {
    return "Looking up product #" + productId;
}
```

---

## Examples

### 1. Inject a Domain Object

Resolve your application's `AppUser` entity by the Telegram user ID on every request:

```java
import java.lang.reflect.Parameter;

@Component
public class AppUserArgumentResolver implements BotArgumentResolver {

    private final AppUserRepository userRepository;

    public AppUserArgumentResolver(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return AppUser.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolveArgument(Parameter parameter,
                                  BotRequest botRequest,
                                  BotResponse botResponse) {
        long telegramId = botRequest.getUser().getId();
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() ->
                    new IllegalStateException("User not registered: " + telegramId));
    }
}
```

```java
@BotController
public class ProfileController {

    @BotCommand("/profile")
    public String onProfile(AppUser appUser) {
        return "Name: " + appUser.getFullName() + "\nEmail: " + appUser.getEmail();
    }

    @BotCommand("/settings")
    public String onSettings(AppUser appUser, BotRequest request) {
        return "Editing settings for " + appUser.getFullName();
    }
}
```

---

### 2. Custom Annotation — `@BotLanguage`

Inject the Telegram user's `languageCode` as a plain `String` using a custom annotation:

#### Define the annotation

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotLanguage {}
```

#### Create the resolver

```java
import java.lang.reflect.Parameter;

@Component
public class BotLanguageArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(BotLanguage.class)
                && String.class.equals(parameter.getType());
    }

    @Override
    public Object resolveArgument(Parameter parameter,
                                  BotRequest botRequest,
                                  BotResponse botResponse) {
        User user = botRequest.getUser();
        return (user != null && user.getLanguageCode() != null)
                ? user.getLanguageCode()
                : "en";
    }
}
```

#### Use in handlers

```java
@BotCommand("/start")
public String onStart(@BotLanguage String lang) {
    return switch (lang) {
        case "ru" -> "Привет! Я бот.";
        case "uz" -> "Salom! Men bot.";
        default -> "Hello! I'm a bot.";
    };
}
```

---

### 3. `BotMarkupContext` in `@BotMarkup` Factory Methods

`BotMarkupContext` carries runtime parameters from a handler's `.withMarkup("id", params)` call
into the keyboard factory method. It is **only available inside `@BotMarkup` methods** — it
resolves to `BotMarkupContext.empty()` everywhere else.

```java
// In a handler — pass parameters to the factory
@BotCommand("/items")
public PlainReply onItems(BotRequest request) {
    int currentPage = 0;
    return PlainReply.of("Items (page 1):")
            .withMarkup("item_list", Map.of("page", currentPage, "size", 5));
}
```

```java
// In a @BotConfiguration class — receive parameters via BotMarkupContext
@BotConfiguration
public class ItemMarkups {

    @BotMarkup("item_list")
    public InlineKeyboardMarkup itemList(BotMarkupContext ctx) {
        int page = ctx.get("page", Integer.class);
        int size = ctx.getOrDefault("size", 10);
        return buildPaginatedKeyboard(page, size);
    }

    private InlineKeyboardMarkup buildPaginatedKeyboard(int page, int size) {
        // ... build the keyboard
    }
}
```

`BotMarkupContext` API:

| Method | Description |
|---|---|
| `<T> T get(String key)` | Typed retrieval (unchecked cast) |
| `<T> T get(String key, Class<T> type)` | Typed retrieval with explicit type |
| `<T> T getOrDefault(String key, T defaultValue)` | Typed retrieval with fallback |
| `boolean has(String key)` | Key presence check |
| `Map<String, Object> asMap()` | Unmodifiable view of all parameters |
| `static BotMarkupContext empty()` | Empty context singleton |

---

### 4. Read Request Attributes Set by a Filter

Filters can attach arbitrary data to `BotRequest` via `setAttribute`. A resolver (or handler)
reads it back later in the same request lifecycle.

#### Filter stores the tenant ID

```java
@Component
@Order(BotFilterOrder.CONTEXT_SETTER + 10)
public class TenantContextFilter implements BotFilter {

    private final TenantResolver tenantResolver;

    public TenantContextFilter(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) {
        Long chatId = request.getChat() != null ? request.getChat().getId() : null;
        if (chatId != null) {
            String tenantId = tenantResolver.resolve(chatId);
            request.setAttribute("tenantId", tenantId);
        }
        chain.doFilter(request, response);
    }
}
```

#### Resolver reads the attribute

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantId {}
```

```java
@Component
public class TenantIdArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(TenantId.class)
                && String.class.equals(parameter.getType());
    }

    @Override
    public Object resolveArgument(Parameter parameter,
                                  BotRequest botRequest,
                                  BotResponse botResponse) {
        return botRequest.getAttribute("tenantId");
    }
}
```

#### Use in handlers

```java
@BotCommand("/dashboard")
public String onDashboard(@TenantId String tenantId, AppUser appUser) {
    return "Tenant: " + tenantId + " | User: " + appUser.getFullName();
}
```

---

### 5. Per-Chat Session Data

Store and retrieve arbitrary chat-scoped state using a `ConcurrentHashMap` keyed by chat ID:

```java
@Component
public class BotSessionArgumentResolver implements BotArgumentResolver {

    private final ConcurrentHashMap<Long, BotSession> sessions = new ConcurrentHashMap<>();

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return BotSession.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolveArgument(Parameter parameter,
                                  BotRequest botRequest,
                                  BotResponse botResponse) {
        long chatId = botRequest.getChat().getId();
        return sessions.computeIfAbsent(chatId, id -> new BotSession());
    }
}
```

```java
// Simple mutable session value object
public class BotSession {
    private int stepIndex = 0;
    private final Map<String, String> data = new HashMap<>();

    public int getStepIndex() { return stepIndex; }
    public void nextStep() { stepIndex++; }
    public void reset() { stepIndex = 0; data.clear(); }
    public void put(String key, String value) { data.put(key, value); }
    public String get(String key) { return data.get(key); }
}
```

```java
@BotController
public class WizardController {

    @BotCommand("/wizard")
    public String start(BotSession session) {
        session.reset();
        return "Step 1: Enter your name.";
    }

    @BotText
    public String onText(BotSession session, @BotTextValue String text) {
        session.put("step_" + session.getStepIndex(), text);
        session.nextStep();
        return "Got: " + text + ". Continue or /wizard to restart.";
    }
}
```

:::note
For production use, prefer `core-chatstate` or a Redis-backed store. The in-memory map above
is lost on restart and not distributed-safe.
:::

---

### 6. Override a Built-in Resolver

Register a `@Bean` with the same resolved type as a built-in resolver and give it a lower
`@Order` value to run first. All beans registered as `@Bean` in a `@Configuration` class also
override `@ConditionalOnMissingBean`-guarded beans of the same type.

The example below replaces the built-in `BotUserArgumentResolver` with one that enriches
`User` from a cache before returning it:

```java
@Configuration
public class ResolverConfig {

    @Bean
    @Order(1) // runs before the built-in resolver (no @Order = Integer.MAX_VALUE)
    public BotArgumentResolver enrichedUserResolver(UserCacheService cache) {
        return new BotArgumentResolver() {

            @Override
            public boolean supportsParameter(Parameter parameter) {
                return User.class.isAssignableFrom(parameter.getType());
            }

            @Override
            public Object resolveArgument(Parameter parameter,
                                          BotRequest botRequest,
                                          BotResponse botResponse) {
                User raw = botRequest.getUser();
                return raw != null ? cache.enrich(raw) : null;
            }
        };
    }
}
```

:::warning
If you override a built-in resolver by type, the built-in resolver **still runs** for any
other parameter that matched it first (factory returns the first match). Use `@Order` carefully
to control which resolver wins for each parameter type.
:::

---

## Testing Argument Resolvers

Resolvers are plain Java objects — no Spring context required:

```java
class AppUserArgumentResolverTest {

    @Test
    void supportsAppUserType() throws Exception {
        AppUserRepository repo = mock(AppUserRepository.class);
        var resolver = new AppUserArgumentResolver(repo);

        Parameter param = SampleHandler.class
                .getMethod("handle", AppUser.class)
                .getParameters()[0];

        assertTrue(resolver.supportsParameter(param));
    }

    @Test
    void resolvesAppUserByTelegramId() throws Exception {
        AppUserRepository repo = mock(AppUserRepository.class);
        when(repo.findByTelegramId(42L)).thenReturn(Optional.of(new AppUser("Alice")));

        var resolver = new AppUserArgumentResolver(repo);

        User telegramUser = new User();
        telegramUser.setId(42L);

        BotRequest request = new BotRequest();
        request.setUser(telegramUser);

        Parameter param = SampleHandler.class
                .getMethod("handle", AppUser.class)
                .getParameters()[0];

        AppUser result = (AppUser) resolver.resolveArgument(param, request, null);

        assertEquals("Alice", result.getFullName());
    }

    // Minimal class for reflective parameter lookup
    static class SampleHandler {
        public void handle(AppUser appUser) {}
    }
}
```

Key points:
- Obtain a real `java.lang.reflect.Parameter` via reflection — mock it only when the resolver
  does not inspect its type or annotations directly.
- Pass `null` for arguments the resolver under test does not use.
- No `@SpringBootTest` or application context needed.

---

## Summary

| What you need | How to do it |
|---|---|
| Inject any type by class | `parameter.getType().isAssignableFrom(...)` in `supportsParameter` |
| Inject by annotation | `parameter.isAnnotationPresent(MyAnnotation.class)` |
| Access Telegram data | Use `botRequest.getUser()`, `.getChat()`, `.getUpdate()` |
| Access cross-component data | `botRequest.getAttribute("key")` (set by a filter) |
| Run before built-in resolvers | `@Order(n)` with `n < Integer.MAX_VALUE` |
| Dynamic markup parameters | Declare `BotMarkupContext` in `@BotMarkup` factory method |

---

See also:
- [Parameter Injection (Core Concept)](../core-concepts/parameter-injection)
- [i18n Setup](./i18n-setup) — built-in `Locale` parameter injection
- [Custom Filters](./custom-filters) — storing and reading request-scoped attributes
