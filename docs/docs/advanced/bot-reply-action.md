---
id: bot-reply-action
title: Custom Reply Actions (BotReplyAction)
description: Extend Easygram's reply dispatch pipeline by implementing BotReplyAction — add custom Telegram Bot API calls (forward, pin, notify) that fire alongside the built-in send/edit/callback actions.
keywords: [BotReplyAction, custom telegram action spring boot, easygram reply extension, forward message bot, pin message bot, reply action chain easygram]
---

# Custom Reply Actions (`BotReplyAction`)

`BotReplyAction` is the SPI that controls **which Telegram Bot API method(s) are called**
when a handler returns a `PlainReply` or `LocalizedReply`. Each action decides independently
whether it should fire and what API call to enqueue.

## How It Works

When a `PlainReply` or `LocalizedReply` is returned from a handler, the framework invokes
`BotReplyActionChain.execute()`. The chain:

1. Collects all `BotReplyAction` beans from the Spring context
2. Sorts them by `getOrder()` (ascending — lower = runs first)
3. Calls `supports(options, request)` on each action
4. For every action that returns `true`, calls `execute(request, response, resolvedText, options)`

Multiple actions **can and do fire in the same chain pass**. For example, when
`answerCallbackQuery = true` and `callbackAlert = false`, both `SendMessageReplyAction`
(order 10) and `AnswerCallbackQueryReplyAction` (order 20) fire — sending a message
_and_ dismissing the callback spinner.

```
BotReplyActionChain
  │
  ├── SendMessageReplyAction      (order=10)  supports when: !callbackAlert && (!editMessage || !callbackQuery)
  ├── EditMessageReplyAction      (order=10)  supports when: !callbackAlert && editMessage && callbackQuery
  └── AnswerCallbackQueryReplyAction (order=20) supports when: answerCallbackQuery == true
```

## The `BotReplyAction` Interface

```java
public interface BotReplyAction {

    /**
     * Returns true if this action should fire for the current reply + request.
     */
    boolean supports(ReplyOptions options, BotRequest request);

    /**
     * Enqueues the Telegram API call(s) on botResponse.
     */
    void execute(BotRequest request, BotResponse response,
                 String resolvedText, ReplyOptions options);

    /**
     * Execution order. Lower = runs first. Default = 0 (before built-in actions).
     */
    default int getOrder() { return 0; }
}
```

---

## Built-in Actions

| Action class | Order | Fires when |
|---|---|---|
| `SendMessageReplyAction` | 10 | `!callbackAlert` AND (`!editMessage` OR update is not a callback query) |
| `EditMessageReplyAction` | 10 | `!callbackAlert` AND `editMessage == true` AND update **is** a callback query |
| `AnswerCallbackQueryReplyAction` | 20 | `answerCallbackQuery == true` |

All three are registered as `@ConditionalOnMissingBean` — replacing one does not affect
the others.

---

## Implementing a Custom Action

### Example 1 — Forward a copy to an audit channel

```java
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.ReplyOptions;
import uz.osoncode.easygram.core.returntypehandler.BotReplyAction;

@Component
public class AuditForwardReplyAction implements BotReplyAction {

    private static final String AUDIT_CHAT_ID = "-100123456789";

    @Override
    public boolean supports(ReplyOptions options, BotRequest request) {
        // fire for every non-callback-alert send
        return !options.callbackAlert();
    }

    @Override
    public void execute(BotRequest request, BotResponse response,
                        String resolvedText, ReplyOptions options) {
        // BotApiMethod calls are queued; forwarding happens after the main message is sent
        response.addBotApiMethod(ForwardMessage.builder()
                .chatId(AUDIT_CHAT_ID)
                .fromChatId(String.valueOf(request.getChat().getId()))
                .messageId(request.getMessage() != null
                        ? request.getMessage().getMessageId() : 0)
                .build());
    }

    @Override
    public int getOrder() {
        return 30; // after built-ins at 10 and 20
    }
}
```

### Example 2 — Pin message when a `ReplyOptions` flag is set

Add a custom option by extending `ReplyOptions` is not currently possible (it is a record),
so use the simplest approach: a thread-local flag or a custom `PlainReply` subclass.
Alternatively, reserve a `ReplyOptions` field you control at the call site:

```java
@Component
public class PinMessageReplyAction implements BotReplyAction {

    // Convention: use callbackUrl field with a "pin:" prefix as a side-channel flag.
    // A cleaner approach is to create a custom ReplyWrapper that wraps ReplyOptions.

    @Override
    public boolean supports(ReplyOptions options, BotRequest request) {
        return options.callbackUrl() != null && options.callbackUrl().startsWith("pin:");
    }

    @Override
    public void execute(BotRequest request, BotResponse response,
                        String resolvedText, ReplyOptions options) {
        // PIN logic — messageId is not yet known here; use a BotStartTrigger or
        // post-send hook for actual pinning.
    }
}
```

:::tip
For fully custom options (e.g., `pinMessage`, `deleteAfterSeconds`), the cleanest approach
is to create your own reply wrapper class and a corresponding `BotReturnTypeHandler` that
constructs the `ReplyOptions` + calls `BotReplyActionChain.execute()`.
:::

---

## Registration

Register the action as a Spring bean. No additional setup is required — the framework
auto-discovers all `BotReplyAction` beans in the context:

```java
// Option 1 — Spring component scan
@Component
public class MyReplyAction implements BotReplyAction { … }

// Option 2 — @Configuration bean (useful when constructor needs injection)
@Bean
public BotReplyAction auditForwardAction(AuditService auditService) {
    return new AuditForwardReplyAction(auditService);
}
```

---

## Replacing a Built-in Action

To replace `SendMessageReplyAction` entirely (e.g., to add a mandatory parse-mode):

```java
@Bean
@Primary
public SendMessageReplyAction sendMessageReplyAction(Optional<BotMarkupRegistry> registry) {
    return new SendMessageReplyAction(registry) {
        @Override
        public void execute(BotRequest request, BotResponse response,
                            String resolvedText, ReplyOptions options) {
            // your override — call super or reimplement
            super.execute(request, response, resolvedText,
                    options.withParseMode("HTML"));
        }
    };
}
```

Or provide your own class that implements `BotReplyAction` with the same `getOrder()`.

---

## Interaction with `ReplyOptions`

Every `PlainReply` or `LocalizedReply` carries a `ReplyOptions` instance. The options object
drives every `supports()` decision:

| `ReplyOptions` field | Controls which action fires |
|---|---|
| `callbackAlert = true` | Only `AnswerCallbackQueryReplyAction` (shows an alert popup) |
| `editMessage = true` + callback query | `EditMessageReplyAction` |
| `answerCallbackQuery = true` | `AnswerCallbackQueryReplyAction` in addition to send/edit |
| `parseMode`, `disableNotification`, etc. | Forwarded verbatim to `SendMessage` / `EditMessageText` |

---

## See Also

- [Return Types](../core-concepts/return-types) — `PlainReply` and `LocalizedReply` usage
- [Custom Return-Type Handlers](./custom-return-handlers) — handle entirely new return types
- [Custom Filters](./custom-filters) — cross-cutting concerns around every update
