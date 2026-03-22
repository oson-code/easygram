---
id: quick-start
title: Quick Start
---

# Quick Start Guide

Get up and running with Easygram in 5 minutes.

## Prerequisites

- Java 17 or higher
- Spring Boot 3.5.x
- Maven or Gradle

## 1. Create a Spring Boot Application

Create a new Spring Boot project (using Spring Initializr or your IDE) with dependencies:
- Spring Web (optional, needed only for webhook transport)
- Spring Boot Starter

## 2. Add Easygram Dependency

**Maven (pom.xml):**
```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

**Gradle (build.gradle.kts):**
```kotlin
implementation("uz.osoncode.easygram:spring-boot-starter:0.0.1")
```

## 3. Configure Your Bot Token

**application.yml:**
```yaml
telegram:
  bot:
    token: ${BOT_TOKEN}
```

Or **application.properties:**
```properties
telegram.bot.token=${BOT_TOKEN}
```

Set the environment variable `BOT_TOKEN` to your Telegram bot token from [@BotFather](https://t.me/BotFather).

## 4. Create Your First Handler

Create a `@BotController` class:

```java
package com.example.mybot;

import uz.osoncode.easygram.core.annotation.BotController;
import uz.osoncode.easygram.core.annotation.BotCommand;
import uz.osoncode.easygram.core.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.annotation.BotText;
import org.telegram.telegrambots.meta.api.objects.User;

@BotController
public class MyBotHandler {

    @BotCommand("/start")
    public String onStart(User user) {
        return "👋 Hello, " + user.getFirstName() + "!";
    }

    @BotText("hello")
    public String onHello() {
        return "Hey there! 👋";
    }

    @BotDefaultHandler
    public String onDefault() {
        return "I didn't understand that. Try /start or say hello!";
    }
}
```

## 5. Create Your Main Application Class

```java
package com.example.mybot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBotApplication.class, args);
    }
}
```

## 6. Run Your Bot

```bash
# Set your bot token
export BOT_TOKEN="your_telegram_bot_token_here"

# Run with Maven
mvn spring-boot:run

# Or run with Gradle
gradle bootRun
```

You should see output like:
```
Starting MyBotApplication
... your bot token is loaded ...
Bot started successfully!
```

## 7. Test Your Bot

Open Telegram and search for your bot (by username from @BotFather). Then:
- Send `/start` → Replies with "👋 Hello, [Your Name]!"
- Send `hello` → Replies with "Hey there! 👋"
- Send anything else → Replies with the default message

## Next Steps

### Add More Handlers
```java
@BotCommand("/help")
public String onHelp() {
    return "Available commands:\n/start - Start the bot\n/help - Show this help message";
}

@BotCallbackQuery("btn_ok")
public String onButtonClick() {
    return "You clicked OK!";
}
```

### Use Keyboards & Markups
```java
@BotCommand("/menu")
public PlainReply onMenu() {
    ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
        .keyboardRow(new KeyboardRow("Option 1"))
        .keyboardRow(new KeyboardRow("Option 2"))
        .build();
    return PlainReply.of("Choose an option:").withMarkup(keyboard);
}
```

### Build Stateful Workflows
```java
@BotCommand("/register")
@BotChatState("REGISTRATION")
@BotForwardChatState("REGISTERED")
public String handleRegistration() {
    return "Great! Please enter your name:";
}
```

### Learn More
- **[Core Concepts](core-concepts/handlers)** — Master handlers and routing
- **[Transports](transports/long-polling-guide)** — Deploy with different update sources
- **[Examples](examples/echo-bot)** — See complete working examples

## Troubleshooting

### Bot doesn't respond
- ✅ Verify bot token is correct in environment variables
- ✅ Check Telegram API status
- ✅ Ensure Spring Boot started without errors
- ✅ Verify `@BotController` class is in a scanned package

### Handler not matching
- ✅ Check handler annotation (`@BotCommand`, `@BotText`, etc.)
- ✅ Verify exact message text matches (case-sensitive for `@BotText`)
- ✅ Check `@BotOrder` if multiple handlers exist
- ✅ Ensure no earlier handler matches first (Tier 1 > Tier 2 > Tier 3)

### Port already in use (Webhook only)
- Change the port in `application.yml`:
```yaml
server:
  port: 8081
```

### "No qualifying bean of type BotChatStateService"
- Add `core-chatstate` dependency:
```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>core-chatstate</artifactId>
    <version>0.0.1</version>
</dependency>
```
(Already included in `spring-boot-starter`)

---

Congratulations! Your first Easygram bot is running! 🎉

