---
id: whatsnew-0.0.6
title: What's New in 0.0.6
description: Summary of all new features, breaking changes, and migration notes for Easygram 0.0.6 — @BotParseMode, delivery options, BotReplyAction SPI, template removal, and messaging factory providers.
keywords: [easygram 0.0.6, telegram bot java 0.0.6 changelog, BotParseMode, BotReplyAction, disableNotification, LocalizedTemplate removed, KafkaProducerFactoryProvider]
---

# What's New in 0.0.6

A summary of every new feature and breaking change in Easygram **0.0.6**. For step-by-step migration instructions see the [0.0.5 → 0.0.6 migration guide](./migration/0.0.5-to-0.0.6.md).

---

## 1. `@BotParseMode` — parse mode annotation

Set Telegram's `parse_mode` field declaratively with a method-level annotation.

```java
@BotCommand("/start")
@BotParseMode("HTML")
public String start(User user) {
    return "<b>Hello, " + user.getFirstName() + "!</b>";
}
```

Works with `String`, `PlainReply`, and `LocalizedReply` return types. Valid values: `"HTML"`, `"MarkdownV2"`, `"Markdown"` (legacy).

All `MarkupAware` reply types also expose `.withParseMode(String)` for runtime control:

```java
return PlainReply.of("<b>Bold text</b>").withParseMode("HTML");
return LocalizedReply.of("msg.formatted").withParseMode("MarkdownV2");
```

📖 [Return Types — @BotParseMode](./core-concepts/return-types.md#bot-parse-mode)

---

## 2. Delivery options on `PlainReply` and `LocalizedReply`

Five new wither methods and builder setters align with Telegram's `sendMessage` fields:

| Method | Telegram field | Description |
|---|---|---|
| `.withDisableNotification(Boolean)` | `disable_notification` | Send silently — no sound or vibration |
| `.withProtectContent(Boolean)` | `protect_content` | Disable forwarding and saving |
| `.withMessageThreadId(Integer)` | `message_thread_id` | Post into a forum topic |
| `.withReplyParameters(ReplyParameters)` | `reply_parameters` | Reply to a specific message |
| `.withLinkPreviewOptions(LinkPreviewOptions)` | `link_preview_options` | Control inline link preview |

```java
return PlainReply.of("Silent update")
    .withDisableNotification(true)
    .withProtectContent(true);

return LocalizedReply.of("forum.post")
    .withMessageThreadId(topicId);
```

📖 [Return Types — Delivery Options](./core-concepts/return-types.md#delivery-options)

---

## 3. `BotReplyAction` SPI — extensible dispatch pipeline

The `PlainReply` and `LocalizedReply` dispatch pipeline is now driven by a chain of
`BotReplyAction` beans instead of if/else branches. Three default actions ship in `core`:

| Action | When it fires |
|---|---|
| `SendMessageReplyAction` | Normal send (not edit, not callback) |
| `EditMessageReplyAction` | `editMessage=true` with an active callback query |
| `AnswerCallbackQueryReplyAction` | `answerCallbackQuery=true` |

Register your own bean to add new dispatch behaviour (e.g. pin a message, forward to a channel) **without modifying any existing code**:

```java
@Bean
public BotReplyAction pinMessageAction() {
    return new PinMessageReplyAction();
}
```

📖 [Custom Reply Actions](./advanced/bot-reply-action.md)

---

## 4. `PlainTextTemplate` and `LocalizedTemplate` removed

Both types were removed in 0.0.6. Migrate to the standard reply types:

### PlainTextTemplate → PlainReply with MessageFormat args

```java
// Before (0.0.5)
PlainTextTemplate.of("Hello, #{0}! You have #{1} messages.", name, count)

// After (0.0.6)
PlainReply.of("Hello, {0}! You have {1} messages.", name, count)
```

### LocalizedTemplate → LocalizedReply with a single bundle key

```java
// Before (0.0.5) — mixed ${key} + #{n} syntax
LocalizedTemplate.of("${welcome.title}\n\n${welcome.body}", user.getFirstName())

// After (0.0.6) — single key with {n} MessageFormat tokens
LocalizedReply.of("welcome.full", user.getFirstName())
```

```properties
# bot_en.properties — standard MessageFormat {n} tokens
welcome.full= Welcome, {0}!\n\nThis is the full welcome message.
```

:::warning Bundle token change
Change `#{n}` → `{n}` in all message bundle files. The old `#{n}` syntax was specific to
the removed template types and will no longer be substituted.
:::

📖 [0.0.5 → 0.0.6 migration guide](./migration/0.0.5-to-0.0.6.md)

---

## 5. Messaging factory providers

Three new `@FunctionalInterface` SPI beans let you fully customise the Kafka and RabbitMQ
connections without touching `application.yml`:

| Interface | Module | Replaces |
|---|---|---|
| `BotKafkaProducerFactoryProvider` | `messaging-api` | `ProducerFactory` for publishing updates |
| `BotKafkaConsumerFactoryProvider` | `messaging-api` | `ConsumerFactory` for the listener container |
| `BotRabbitConnectionFactoryProvider` | `messaging-api` | `ConnectionFactory` for publish + consume |

All three are `@ConditionalOnMissingBean` — the defaults use `easygram.kafka.*` / `easygram.rabbit.*` properties.

```java
@Bean
public BotKafkaProducerFactoryProvider botKafkaProducerFactoryProvider() {
    return () -> new DefaultKafkaProducerFactory<>(customProducerProps());
}
```

📖 [Broker Publishing — Factory Providers](./advanced/broker-publishing.md)

---

## Dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.6</version>
</dependency>
```
