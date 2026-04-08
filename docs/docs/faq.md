---
id: faq
title: Frequently Asked Questions
description: Frequently asked questions about Easygram — transport configuration, missing beans, property key changes, consumer startup issues, and general Spring Boot Telegram bot troubleshooting.
keywords: [easygram faq, telegram bot troubleshooting spring boot, telegram bot not starting, easygram properties help]
---

# Frequently Asked Questions

## Getting Started

### Q: How do I get a Telegram bot token?
**A:** Message [@BotFather](https://t.me/BotFather) on Telegram:
1. Type `/newbot`
2. Choose a name and username
3. Copy the token provided

### Q: Can I run Easygram without Spring Boot?
**A:** No, Easygram is built on Spring Boot. You need Spring Boot 3.5.x.

### Q: Which version of Java is required?
**A:** Java 17 or higher is required.

## Handlers

### Q: How do I handle multiple update types in one method?
**A:** Use `@BotDefaultHandler` to catch anything, or handle specific types separately:

```java
@BotCommand("/help")
public String help() { ... }

@BotText("help")
public String helpText() { ... }
```

### Q: How do I prioritize handlers when multiple match?
**A:** Use `@BotOrder` with lower values = higher priority (default: `Integer.MAX_VALUE`).

## Chat State

### Q: Can I store state in a database?
**A:** Yes! Implement `BotChatStateService` and register as `@Bean`:

```java
@Bean
public BotChatStateService myStateService() {
    return new JdbcChatStateService();
}
```

### Q: How do I clear state?
**A:** Use `@BotClearChatState` annotation on handler methods.

### Q: What if user doesn't have a state?
**A:** State handlers are skipped; non-state handlers are tried instead. Tier 2 and Tier 3 handlers execute normally.

## Keyboard & Markup

### Q: Can I send inline buttons (keyboard)?
**A:** Yes! Use `PlainReply` with a directly-built keyboard:

```java
InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
    .keyboardRow(new InlineKeyboardRow(List.of(
        InlineKeyboardButton.builder().text("Yes").callbackData("yes").build(),
        InlineKeyboardButton.builder().text("No").callbackData("no").build()
    ))).build();

return PlainReply.of("Choose:").withKeyboard(kb);
```

Or register a keyboard factory in a `@BotConfiguration` class and attach it via `.withMarkup("my_keyboard")`.

### Q: Can I use dynamic keyboards?
**A:** Yes — two approaches:

**Approach 1 — inline (direct):** Build and attach the keyboard at return time:
```java
ReplyKeyboardMarkup kb = ReplyKeyboardMarkup.builder()
    .keyboardRow(new KeyboardRow(List.of(new KeyboardButton(option1))))
    .resizeKeyboard(true).build();
return PlainReply.of("Pick one:").withKeyboard(kb);
```

**Approach 2 — registry with params:** Pass runtime data to a registered factory:
```java
// In handler:
return PlainReply.of("Pick one:").withMarkup("item_list", Map.of("items", myItems));

// In @BotConfiguration:
@BotMarkup("item_list")
public InlineKeyboardMarkup itemList(BotMarkupContext ctx) {
    List<String> items = ctx.get("items");
    // build keyboard from items...
}
```

## Deployment

### Q: Should I use long-polling or webhook?
**A:**
- **Development/Testing:** Long-polling (simpler)
- **Production/High traffic:** Webhook (lower latency, scalable)

### Q: Can I run multiple bot instances?
**A:** Yes! Use persistent chat state (Redis) and load balancing:

```yaml
easygram:
  i18n:
    default-locale: en
```

Then deploy multiple instances pointing to same Redis.

### Q: Can I scale horizontally?
**A:** Yes! Use Kafka consumer transport and persistent state backend.

## Errors & Troubleshooting

### Q: My handlers aren't matching
**A:** Check:
1. Handler annotation and message content match exactly (case-sensitive)
2. State matches current user state (if using `@BotChatState`)
3. Handler order (`@BotOrder`) — higher priority may match first
4. `@BotController` is in a scanned package

### Q: "No qualifying bean of type BotChatStateService"
**A:** Add dependency:
```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>core-chatstate</artifactId>
    <version>0.0.5</version>
</dependency>
```

### Q: Bot doesn't respond
**A:** Check:
1. Token is correct (copy from @BotFather)
2. Spring Boot started without errors
3. Bot is listening on port 8080 (or configured port)
4. Firewall allows outbound HTTPS (Telegram API)
5. Telegram API status is healthy

### Q: Webhook returns 500 error
**A:** Check:
1. HTTPS certificate is valid
2. URL is correct and accessible from internet
3. Spring Boot is running
4. Check server logs for exception details

## Performance

### Q: How many updates per second can it handle?
**A:** Depends on:
- Handler complexity
- Database/external service calls
- Hardware
- Number of bot instances
- Transport type (webhook > long-polling)

Typical: 100-1000 updates/second per instance.

### Q: Should I use async/reactive programming?
**A:** Easygram is synchronous by default (fine for most use cases). For high-throughput async processing, consider Kafka consumer transport.

## API Calls

### Q: Can I send multiple messages?
**A:** Yes! Return a `Collection<BotApiMethod<?>>`:

```java
return Arrays.asList(
    SendMessage.builder()...build(),
    SendMessage.builder()...build()
);
```

### Q: Can I edit a message?
**A:** Yes! Return `EditMessageText` API call:

```java
return EditMessageText.builder()
    .chatId(chatId)
    .messageId(messageId)
    .text("Updated")
    .build();
```

## Internationalization

### Q: How do I add multiple languages?
**A:** Add message files:
```
resources/
  messages/
    bot.properties
    bot_en.properties
    bot_de.properties
    bot_fr.properties
```

See [i18n Setup](advanced/i18n-setup) for details.

---

Still have questions? Open an [issue on GitHub](https://github.com/oson-code/easygram/issues) or start a [discussion](https://github.com/oson-code/easygram/discussions).
