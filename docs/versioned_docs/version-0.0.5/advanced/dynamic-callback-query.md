---
id: dynamic-callback-query
title: Dynamic Callback Queries
description: Handle dynamic inline keyboard callbacks in Easygram — @BotCallbackQuery with runtime data extraction, AnswerCallbackQuery toast/alert support, URL payloads, and cache time.
keywords: [BotCallbackQuery, telegram inline keyboard java, AnswerCallbackQuery spring boot, dynamic callback data telegram, easygram callback query]
---

# Dynamic Callback Queries

Telegram limits inline button callback data to **64 bytes**. When you need to pass richer
payloads through a button click — product IDs, form state, multi-field context — you can
store the data server-side and put only a lookup key in the button. Easygram provides
first-class support for this pattern via `@BotDynamicCallbackQuery`.

---

## How it works

1. **Build the button** — store a `BotDynamicCallbackData` payload via
   `BotDynamicCallbackQueryService` and put a generated UUID key as the button's
   `callbackData`. `BotKeyboardFactory` does this in one call.
2. **User clicks the button** — Telegram sends a callback query containing the UUID key.
3. **Framework resolves the payload** — `BotDynamicCallbackQueryMetaDataResolver` looks up
   the key, checks the payload's `type` field, and routes to the matching handler.
4. **Handler receives the data** — `BotDynamicCallbackData` is injected as a method parameter.

```
Button callbackData = "a3f2b1c0-…"  (UUID key)
        ↓
BotDynamicCallbackQueryService.resolve("a3f2b1c0-…")
        ↓
BotDynamicCallbackData { type = "product_buy", data = { id: 42, currency: "USD" } }
        ↓
@BotDynamicCallbackQuery("product_buy")  handler invoked
```

---

## `BotDynamicCallbackData`

An immutable model with a **type** discriminator and a flexible `Map<String, Object>` payload.

```java
BotDynamicCallbackData payload = BotDynamicCallbackData.builder()
    .type("product_buy")
    .put("id", 42L)
    .put("currency", "USD")
    .build();

String type = payload.getType();               // "product_buy"
Long   id   = (Long) payload.getData().get("id"); // 42
```

---

## `BotDynamicCallbackQueryService`

SPI interface for storing and retrieving payloads. The default in-memory implementation is
registered automatically. Replace it with your own `@Bean` for Redis or database persistence.

```java
public interface BotDynamicCallbackQueryService {
    BotDynamicCallbackData resolve(String callbackData);
    void store(String callbackData, BotDynamicCallbackData payload);
    void remove(String callbackData);
}
```

### Custom implementation (e.g., Redis)

```java
@Bean
public BotDynamicCallbackQueryService redisDynamicCallbackService(RedisTemplate<String, Object> redis) {
    return new RedisBotDynamicCallbackQueryService(redis);
}
```

---

## `@BotDynamicCallbackQuery`

Method-level annotation that routes callback queries by payload type.

```java
@BotController
public class ProductHandler {

    @BotDynamicCallbackQuery("product_buy")
    public String onBuy(BotDynamicCallbackData data) {
        Long id = (Long) data.getData().get("id");
        return "You bought product #" + id + "!";
    }

    @BotDynamicCallbackQuery("product_view")
    public String onView(BotDynamicCallbackData data) {
        return "Viewing product #" + data.getData().get("id");
    }
}
```

Multiple types can be matched in one handler:

```java
@BotDynamicCallbackQuery({"product_buy", "product_view"})
public String onProductAction(BotDynamicCallbackData data) {
    return "Action: " + data.getType();
}
```

---

## Building dynamic buttons with `BotKeyboardFactory`

### Direct helper

```java
@Autowired BotKeyboardFactory keyboardFactory;

// Single button — UUID key generated automatically, payload stored in service
InlineKeyboardButton btn = keyboardFactory.dynamicInlineButton(
    "btn.buy",
    BotDynamicCallbackData.builder().type("product_buy").put("id", productId).build(),
    request
);
```

### Fluent builder — `dynamicRow`

```java
InlineKeyboardMarkup keyboard = keyboardFactory.inline(request)
    .dynamicRow("btn.buy",
        BotDynamicCallbackData.builder().type("product_buy").put("id", productId).build())
    .dynamicRow(
        BotKeyboardFactory.entry("btn.view",
            BotDynamicCallbackData.builder().type("product_view").put("id", productId).build()),
        BotKeyboardFactory.entry("btn.cancel",
            BotDynamicCallbackData.builder().type("product_cancel").put("id", productId).build()))
    .build();
```

---

## Cleaning up after handling

Call `remove()` after processing to prevent unbounded memory growth (especially with the
default in-memory service):

```java
@Autowired BotDynamicCallbackQueryService dynamicService;

@BotDynamicCallbackQuery("product_buy")
public String onBuy(BotDynamicCallbackData data, @BotCallbackQueryData String callbackKey) {
    dynamicService.remove(callbackKey);   // clean up the stored entry
    Long id = (Long) data.getData().get("id");
    return "Bought product #" + id;
}
```

---

## Full example

```java
@BotController
public class ShopHandler {

    @Autowired private BotKeyboardFactory keyboardFactory;
    @Autowired private BotDynamicCallbackQueryService dynamicService;

    @BotCommand("/shop")
    public SendMessage showProducts(BotRequest request) {
        long productId = 42L;

        InlineKeyboardMarkup keyboard = keyboardFactory.inline(request)
            .dynamicRow(
                BotKeyboardFactory.entry("btn.buy",
                    BotDynamicCallbackData.builder()
                        .type("product_buy").put("id", productId).build()),
                BotKeyboardFactory.entry("btn.details",
                    BotDynamicCallbackData.builder()
                        .type("product_view").put("id", productId).build()))
            .build();

        return SendMessage.builder()
            .chatId(request.getChat().getId())
            .text("Choose an action:")
            .replyMarkup(keyboard)
            .build();
    }

    @BotDynamicCallbackQuery("product_buy")
    public String onBuy(BotDynamicCallbackData data, @BotCallbackQueryData String key) {
        dynamicService.remove(key);
        return "You bought product #" + data.getData().get("id") + "!";
    }

    @BotDynamicCallbackQuery("product_view")
    public String onView(BotDynamicCallbackData data) {
        return "Details for product #" + data.getData().get("id");
    }
}
```

---

## Memory management

The default `InMemoryBotDynamicCallbackQueryService` is an unbounded `ConcurrentHashMap`.
For long-running bots, either:
- Call `dynamicService.remove(key)` in every handler, or
- Use a TTL-backed store (e.g., Redis with key expiry).
