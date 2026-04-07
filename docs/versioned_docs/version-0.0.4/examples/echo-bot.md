---
id: echo-bot
title: Echo Bot Example
---

# Echo Bot Example

A minimal Telegram bot using the long-polling transport. It echoes every text message back,
handles `/start` and `/help` commands, and falls back gracefully for unknown input.

**Full source:** [`samples/longpolling-bot`](https://github.com/oson-code/easygram/tree/main/samples/longpolling-bot)

## What We Build

| User sends | Bot replies |
|---|---|
| `/start` | Greeting with the user's first name |
| `/help` | List of available commands |
| Any text message | Echoes the text back with the user's name |
| Any other update (sticker, photo, …) | "I don't know how to handle that" |

## Project Setup

### Maven dependency

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.3</version>
</dependency>
```

### application.yml

```yaml
easygram:
  token: "${BOT_TOKEN}"   # Set BOT_TOKEN environment variable

spring:
  application:
    name: longpolling-bot

logging:
  level:
    uz.osoncode: DEBUG
```

Long-polling is the default transport — no extra `transport` property needed.

## Application Class

```java
package uz.example.longpolling;

import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.provider.BotOkHttpClientProvider;
import java.time.Duration;

@SpringBootApplication
public class LongpollingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(LongpollingBotApplication.class, args);
    }

    /**
     * Override only the HTTP client timeout.
     * All other infrastructure beans use framework defaults — no override needed.
     */
    @Bean
    public BotOkHttpClientProvider botOkHttpClientProvider() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(50))
                .readTimeout(Duration.ofSeconds(50))
                .writeTimeout(Duration.ofSeconds(50))
                .build();
        return () -> client;
    }
}
```

:::tip
`BotOkHttpClientProvider` is shown here to demonstrate the provider pattern. For a minimal bot,
you can omit it entirely and use the framework's default HTTP client.
:::

## Bot Controller

```java
package uz.example.longpolling;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.bind.annotation.*;
import uz.osoncode.easygram.core.model.BotRequest;

@BotController
public class EchoBotController {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "! I am an echo bot. "
               + "Send me any message and I will repeat it.\n\nUse /help to see available commands.";
    }

    @BotCommand("/help")
    public String onHelp(@BotCommandValue String command) {
        return "Available commands:\n" +
               "/start — welcome message\n" +
               "/help  — this help text\n\n" +
               "Send any text message and I will echo it back.";
    }

    @BotTextDefault
    public String onText(@BotTextValue String text, User user) {
        return user.getFirstName() + " said: " + text;
    }

    @BotDefaultHandler
    public SendMessage onUnknown(BotRequest request) {
        return SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("I don't know how to handle that. Try /help.")
                .build();
    }
}
```

### Handler breakdown

| Method | Annotation | Triggered when |
|---|---|---|
| `onStart` | `@BotCommand("/start")` | User sends `/start` |
| `onHelp` | `@BotCommand("/help")` | User sends `/help` |
| `onText` | `@BotTextDefault` | User sends any plain text not matched elsewhere |
| `onUnknown` | `@BotDefaultHandler` | Any other update type (sticker, photo, callback, …) |

## Parameter Injection

### User

`User user` is resolved automatically by type — no annotation needed. `User` is extracted from
the `Update` and stored in `BotRequest` by `BotContextSetterFilter`.

### @BotCommandValue

`@BotCommandValue String command` injects the matched command string (e.g. `"/help"`). Useful
when multiple commands share one method or when you need to log the command.

### @BotTextValue

`@BotTextValue String text` injects the complete message text.

### BotRequest

`BotRequest request` gives access to the raw `Update`, resolved `User`, `Chat`, and
`TelegramClient`. Used here to build a `SendMessage` manually with explicit `chatId`.

## Return Types

| Handler | Returns | How it is sent |
|---|---|---|
| `onStart`, `onHelp`, `onText` | `String` | Wrapped in `SendMessage` → same chat |
| `onUnknown` | `SendMessage` | Enqueued and executed directly |

## Running the Bot

```bash
export BOT_TOKEN="your_token_here"
mvn spring-boot:run
```

Open Telegram and message your bot:
- `/start` → `Hello, Alice! I am an echo bot…`
- `hello there` → `Alice said: hello there`
- Send a sticker → `I don't know how to handle that. Try /help.`

## Extending the Echo Bot

### Add an inline keyboard

```java
@BotCommand("/menu")
public PlainReply onMenu() {
    InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(
                InlineKeyboardButton.builder().text("Option A").callbackData("opt_a").build(),
                InlineKeyboardButton.builder().text("Option B").callbackData("opt_b").build()
            ))
            .build();
    return PlainReply.of("Choose an option:").withMarkup(keyboard);
}

@BotCallbackQuery("opt_a")
public String onOptA() { return "You picked A!"; }

@BotCallbackQuery("opt_b")
public String onOptB() { return "You picked B!"; }
```

### Reply with a photo

```java
@BotCommand("/photo")
public SendPhoto onPhoto(BotRequest request) {
    return SendPhoto.builder()
            .chatId(request.getChat().getId())
            .photo(new InputFile("https://picsum.photos/400/300"))
            .caption("A random photo!")
            .build();
}
```

---

See also:
- [Quick Start](../quick-start) — set up a new project from scratch
- [Handlers (Core Concept)](../core-concepts/handlers) — all handler annotations
- [Return Types](../core-concepts/return-types) — all supported return types
- [Webhook Bot Example](./webhook-bot) — same bot with HTTPS webhook transport
