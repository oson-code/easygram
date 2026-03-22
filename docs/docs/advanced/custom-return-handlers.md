---
id: custom-return-handlers
title: Custom Return-Type Handlers
---

# Custom Return-Type Handlers

Return-type handlers convert handler method return values into `BotApiMethod` calls that are
enqueued in `BotResponse` and sent to Telegram. Add your own handler to support any custom
return type without modifying framework code.

## The BotReturnTypeHandler Interface

```java
public interface BotReturnTypeHandler {

    /**
     * Called once at startup — return true to claim this method's return type.
     * Matched against the declared return type of the handler method.
     */
    boolean supportsReturnType(Method method);

    /**
     * Called at runtime when dispatching individual elements inside a
     * Collection<Object>. Return true if this handler can process the element.
     */
    default boolean supportsElement(Object returnValue) {
        return false;
    }

    /** Main dispatch entry point. */
    void handleReturnType(BotRequest request, BotResponse response, Object returnValue);
}
```

Register any `BotReturnTypeHandler` as a Spring `@Bean`. The framework collects them all and
returns the **first matching handler** — registration order matters.

## Built-in Handlers

| Return type | Handler class |
|---|---|
| `void` | `BotVoidReturnHandler` — no response sent |
| `String` | `BotStringReturnHandler` → `SendMessage` |
| `PlainReply` | `BotPlainReplyReturnTypeHandler` |
| `PlainTextTemplate` | `BotPlainTextTemplateReturnTypeHandler` |
| `BotApiMethod<?>` | `BotBotApiMethodReturnHandler` — enqueued directly |
| `Collection<BotApiMethod<?>>` | `BotBotApiMethodsReturnHandler` — all enqueued |
| `Collection<Object>` (mixed) | `BotMixedCollectionReturnTypeHandler` — per-element dispatch |
| `LocalizedReply` *(core-i18n)* | `BotLocalizedReplyReturnTypeHandler` |
| `LocalizedTemplate` *(core-i18n)* | `BotLocalizedTemplateReturnTypeHandler` |

## Example: Custom Reply Type

Suppose you have a `RichReply` that bundles a text message and an optional photo:

### 1. Define the type

```java
public record RichReply(String text, String photoUrl, String photoCaption) {

    public static RichReply of(String text) {
        return new RichReply(text, null, null);
    }

    public RichReply withPhoto(String url, String caption) {
        return new RichReply(this.text, url, caption);
    }
}
```

### 2. Implement the handler

```java
@Component
public class RichReplyReturnTypeHandler implements BotReturnTypeHandler {

    @Override
    public boolean supportsReturnType(Method method) {
        return RichReply.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public boolean supportsElement(Object returnValue) {
        return returnValue instanceof RichReply; // needed for Collection<Object> dispatch
    }

    @Override
    public void handleReturnType(BotRequest request, BotResponse response, Object returnValue) {
        RichReply reply  = (RichReply) returnValue;
        long      chatId = request.getChat().getId();

        response.addBotApiMethod(SendMessage.builder()
                .chatId(chatId)
                .text(reply.text())
                .parseMode("HTML")
                .build());

        if (reply.photoUrl() != null) {
            response.addBotApiMethod(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(reply.photoUrl()))
                    .caption(reply.photoCaption())
                    .build());
        }
    }
}
```

### 3. Use in handlers

```java
@BotController
public class ProductController {

    @BotCommand("/product")
    public RichReply onProduct() {
        return RichReply.of("<b>🛍 Product</b>\n\nPrice: $19.99")
                .withPhoto("https://example.com/product.jpg", "Front view");
    }

    @BotCommand("/text-only")
    public RichReply onTextOnly() {
        return RichReply.of("Just text, no photo.");
    }
}
```

## Example: Template Engine Integration

Render messages with Thymeleaf or FreeMarker:

```java
public record TemplateReply(String templateName, Map<String, Object> model) {}

@Component
public class TemplateReplyReturnTypeHandler implements BotReturnTypeHandler {

    private final TemplateEngine templateEngine;

    public TemplateReplyReturnTypeHandler(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean supportsReturnType(Method method) {
        return TemplateReply.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public void handleReturnType(BotRequest request, BotResponse response, Object returnValue) {
        TemplateReply reply = (TemplateReply) returnValue;
        Context ctx = new Context();
        ctx.setVariables(reply.model());
        String rendered = templateEngine.process(reply.templateName(), ctx);

        response.addBotApiMethod(SendMessage.builder()
                .chatId(request.getChat().getId())
                .text(rendered)
                .parseMode("HTML")
                .build());
    }
}
```

```java
@BotCommand("/invoice")
public TemplateReply onInvoice(AppUser user) {
    return new TemplateReply("invoice", Map.of("user", user, "amount", "29.99"));
}
```

## Using supportsElement for Mixed Collections

Implement `supportsElement` to participate in `Collection<Object>` dispatch:

```java
@BotCommand("/order")
public List<Object> onOrder() {
    return List.of(
        "Here is your order:",         // → BotStringReturnHandler
        RichReply.of("Product A"),     // → RichReplyReturnTypeHandler
        RichReply.of("Product B")      // → RichReplyReturnTypeHandler
    );
}
```

## Registration Order

```java
@Component
@Order(10) // Register before built-in handlers if you need priority
public class MyReturnTypeHandler implements BotReturnTypeHandler { ... }
```

:::warning
Keep `supportsReturnType` precise. Returning `true` for `Object` captures every handler and
prevents built-in handlers from running.
:::

## When to Use Return-Type Handlers vs. BotApiMethod

Use a custom handler when:
- The same response structure is produced across many handlers
- You want to separate presentation logic from business logic
- The response involves multiple API calls (text + photo, text + keyboard)

Return `BotApiMethod<?>` directly when:
- You need exact control over a single API call
- The response is unique to that handler

---

See also:
- [Return Types (Core Concept)](../core-concepts/return-types) — built-in return types
- [i18n Setup](./i18n-setup) — `LocalizedReply` and `LocalizedTemplate`
- [Custom Argument Resolvers](./custom-argument-resolvers) — complementary extension point
