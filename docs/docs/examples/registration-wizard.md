---
id: registration-wizard
title: Registration Wizard Example
description: Multi-step user registration wizard with Easygram chat state — @BotChatState, @BotForwardChatState, stateful conversation flows, and data collection across multiple messages.
keywords: [telegram bot registration wizard, spring boot chatbot multi-step, BotChatState example, stateful telegram bot java, easygram wizard]
---

# Registration Wizard Example

A multi-step registration form implemented as a chat-state machine. The bot guides the user
through three steps (name → age → city), validates input at each step, and provides a cancel
button at every stage. This example demonstrates the full `@BotChatState` / `@BotForwardChatState`
/ `@BotClearChatState` state machine.

**Full source:** [`samples/chatstate-bot`](https://github.com/oson-code/easygram/tree/main/samples/chatstate-bot)

## State Machine Diagram

```
/register
    ↓ @BotForwardChatState("AWAITING_NAME")
[state: AWAITING_NAME] "Step 1/3 — What is your full name?"
    ↓ user sends any text → @BotForwardChatState("AWAITING_AGE")
[state: AWAITING_AGE] "Step 2/3 — How old are you?"
    ↓ user sends valid number → manual setState("AWAITING_CITY")
    ↓ invalid number → stay in AWAITING_AGE, show error
[state: AWAITING_CITY] "Step 3/3 — Which city do you live in?"
    ↓ user sends any text → @BotClearChatState
[no state] " Registration complete!"
```

Cancel via `/cancel` or Cancel button is available at every step.

## Project Setup

```xml
<dependency>
    <groupId>uz.osoncode.easygram</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>0.0.7</version>
</dependency>
```

```yaml
easygram:
  token: "${BOT_TOKEN}"
spring:
  application:
    name: chatstate-bot
```

## State Enum

Using an enum rather than raw strings gives compile-time safety and IDE autocompletion:

```java
package uz.example.chatstate;

public enum RegistrationState {
    /** Bot has asked for the user's full name. */
    AWAITING_NAME,

    /** Bot has asked for the user's age. */
    AWAITING_AGE,

    /** Bot has asked for the user's city. */
    AWAITING_CITY
}
```

`BotChatStateService` stores enum values as their `name()` string internally, and `getStateAs`
parses them back with `Enum.valueOf`.

## Cancel Keyboard Markup

Declare a reusable keyboard using `@BotConfiguration` and `@BotMarkup`:

```java
@BotConfiguration
@RequiredArgsConstructor
@Slf4j
public class RegistrationMarkups {

    private final BotKeyboardFactory botKeyboardFactory;

    /**
     * @BotMarkup methods support any parameter resolvable by the argument resolver system.
     * User, BotRequest, Locale, and custom types are all valid — resolved at creation time.
     */
    @BotMarkup("kb_cancel")
    public ReplyKeyboard cancelKeyboard(User user) {
        log.info("Building cancel keyboard for user: {}", user.getUserName());
        return ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(" Cancel"))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }
}
```

## Global Commands Controller

Commands available at any point, regardless of chat state:

```java
@BotController
public class GlobalCommandController {

    private final BotChatStateService chatStateService;

    public GlobalCommandController(BotChatStateService chatStateService) {
        this.chatStateService = chatStateService;
    }

    @BotCommand("/start")
    public String onStart(User user) {
        return " Hello, " + user.getFirstName() + "! I'm a registration bot.\n\n" +
               "Commands:\n" +
               " /register — start the registration wizard\n" +
               " /status — show your current step\n" +
               " /cancel — cancel the wizard at any step";
    }

    /** Shows where the user is in the wizard by reading state directly. */
    @BotCommand("/status")
    public String onStatus(User user) {
        RegistrationState state = chatStateService.getStateAs(user.getId(), RegistrationState.class);
        if (state == null) {
            return "ℹ No active wizard. Use /register to start.";
        }
        return switch (state) {
            case AWAITING_NAME -> " Waiting for your name (step 1/3).";
            case AWAITING_AGE -> " Waiting for your age (step 2/3).";
            case AWAITING_CITY -> " Waiting for your city (step 3/3).";
        };
    }

    /**
     * @BotClearChatState automatically clears state after this method returns.
     * Injecting chatStateService here allows checking if there's something to cancel.
     */
    @BotCommand("/cancel")
    @BotClearChatState
    public String onCancel(User user) {
        String current = chatStateService.getState(user.getId());
        if (current == null) {
            return "ℹ No active wizard to cancel.";
        }
        return " Wizard cancelled. Use /register to start again.";
    }

    @BotDefaultHandler
    public SendMessage onDefault(BotRequest request) {
        return SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("Use /register to start, /status to check, or /cancel to stop.")
                .build();
    }
}
```

## Registration Flow Controller

The class-level `@BotChatState` restricts **all** methods to run only when the user is in one
of the three registration states:

```java
@BotController
@BotChatState({"AWAITING_NAME", "AWAITING_AGE", "AWAITING_CITY"})
public class RegistrationFlowController {

    private final BotChatStateService chatStateService;

    public RegistrationFlowController(BotChatStateService chatStateService) {
        this.chatStateService = chatStateService;
    }

    // Step 0: entry point

    /**
     * @BotChatState with an empty array overrides the class-level restriction,
     * making /register reachable at any point (including when there is no state).
     * @BotForwardChatState advances to AWAITING_NAME automatically after return.
     */
    @BotCommand("/register")
    @BotChatState // empty array = override class guard, accept any state
    @BotForwardChatState("AWAITING_NAME")
    @BotReplyMarkup("kb_cancel")
    public String startRegistration(User user) {
        return " Let's get you registered!\n\nStep 1/3 — What is your full name?";
    }

    // Step 1: collect name

    @BotTextDefault
    @BotChatState("AWAITING_NAME")
    @BotForwardChatState("AWAITING_AGE")
    @BotReplyMarkup("kb_cancel")
    public String collectName(@BotTextValue String name) {
        // In production, persist the name to your database here
        return " Name saved: " + name + "\n\nStep 2/3 — How old are you? (enter a number)";
    }

    // Step 2: collect age

    /**
     * State advance is conditional on valid input, so it is done manually.
     * @BotForwardChatState cannot be used here because it always fires unconditionally.
     */
    @BotTextDefault
    @BotChatState("AWAITING_AGE")
    @BotReplyMarkup("kb_cancel")
    public String collectAge(@BotTextValue String ageText, User user) {
        try {
            int age = Integer.parseInt(ageText.trim());
            if (age <= 0 || age > 120) {
                return " Please enter a valid age (1–120).";
            }
            chatStateService.setState(user.getId(), RegistrationState.AWAITING_CITY);
            return " Age saved: " + age + "\n\nStep 3/3 — Which city do you live in?";
        } catch (NumberFormatException e) {
            return " That doesn't look like a number. Please enter digits only.";
        }
    }

    // Step 3: collect city

    @BotTextDefault
    @BotChatState("AWAITING_CITY")
    @BotClearMarkup
    @BotClearChatState
    public String collectCity(@BotTextValue String city) {
        return " Registration complete!\n\n" +
               "Summary:\n" +
               "• Name: (saved in step 1)\n" +
               "• Age: (saved in step 2)\n" +
               "• City: " + city + "\n\n" +
               "Use /status to check your registration or /register to start over.";
    }

    // Cancel button

    /** " Cancel" button tap during any registration step. */
    @BotReplyButton(" Cancel")
    @BotClearChatState
    @BotClearMarkup
    public String onCancel() {
        return " Registration cancelled. Use /register to start again.";
    }
}
```

## How State Transitions Work

### Annotation-driven: @BotForwardChatState

Applies a fixed state transition after the handler returns — used when the transition is
unconditional:

```java
@BotForwardChatState("AWAITING_AGE") // always runs after return
public String collectName() { ... }
```

### Manual transition

Used when the next state depends on runtime data:

```java
if (validAge) {
    chatStateService.setState(userId, RegistrationState.AWAITING_CITY);
    return " Age saved...";
} else {
    return " Invalid age..."; // state unchanged — user stays in AWAITING_AGE
}
```

### @BotClearChatState

Clears the state (sets to null) after the handler returns. The keyboard removal version
`@BotClearMarkup` removes the reply keyboard.

## Dispatch Priority

When the user is in `AWAITING_NAME` and sends text:

1. **Tier 1 (state handlers)** — `collectName` matches → executed
2. Tier 2 and Tier 3 never reached

When the user has no state and sends `/register`:

1. **Tier 1** — `startRegistration` has `@BotChatState` with empty array, meaning "any state" → matches

When the user has no state and sends text:

1. **Tier 1** — no match (RegistrationFlowController requires a state)
2. **Tier 3** — `GlobalCommandController.onDefault` matches

## Running the Bot

```bash
export BOT_TOKEN="your_token_here"
mvn spring-boot:run
```

Test the wizard:
1. `/register` → "Let's get you registered! Step 1/3…"
2. `Alice Johnson` → " Name saved. Step 2/3…"
3. `abc` → " That doesn't look like a number."
4. `30` → " Age saved. Step 3/3…"
5. `New York` → " Registration complete!"

---

See also:
- [Chat State (Core Concept)](../core-concepts/chat-state) — annotations and flow
- [Chat State Backends](../advanced/chat-state-backends) — Redis / JDBC persistent backends
- [Handlers (Core Concept)](../core-concepts/handlers) — dispatch tier priorities
