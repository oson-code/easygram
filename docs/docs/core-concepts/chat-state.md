---
id: chat-state
title: Chat State Management
description: Manage multi-step conversation flows in Telegram bots using Easygram chat state — @BotChatState, @BotForwardChatState, BotChatStateService, and pluggable state backends.
keywords: [telegram bot chat state java, BotChatState, multi-step telegram bot, spring boot conversation state, BotForwardChatState, easygram state management]
---

# Chat State Management

Build multi-step workflows and stateful conversations using chat state.

## The @BotChatState Annotation

Tag handler methods with state names. Only handlers matching the user's current state execute.

```java
@BotCommand("/register")
@BotChatState("REGISTRATION")
public String handleRegistrationStep() {
    return "Enter your name:";
}
```

## State Transitions

### @BotForwardChatState

Transition to a new state after handler executes:

```java
@BotCommand("/register")
@BotForwardChatState("WAITING_AGE") // Sets user's chat state to "WAITING_AGE"
public String askForAge() {
    return "Now enter your age:";
}

@BotText("\\d+")
@BotChatState("WAITING_AGE")
@BotForwardChatState("REGISTRATION_COMPLETE") // Transition when age is provided
public String processAge(String ageText) {
    return "Registration complete!";
}
```

### @BotClearChatState

Clear the user's state:

```java
@BotCommand("/cancel")
@BotClearChatState
public String cancelRegistration() {
    return "Registration cancelled.";
}
```

## Example: Registration Wizard

```java
@BotController
public class RegistrationBot {
    
    @BotCommand("/register")
    public String startRegistration() {
        // User is not in any state yet
        return "Welcome to registration! Please enter your name:";
    }
    
    @BotText("^[A-Za-z ]{3,}$") // Regex: 3+ letters
    @BotForwardChatState("WAITING_AGE")
    public String acceptName(@BotTextValue String name) {
        return "Thanks, " + name + "! Now enter your age:";
    }
    
    @BotTextDefault
    public String invalidName() {
        return "Please enter a valid name (3+ letters)";
    }
    
    @BotText("^\\d{1,3}$") // Regex: 1-3 digits
    @BotChatState("WAITING_AGE")
    @BotForwardChatState("WAITING_CONFIRMATION")
    public String acceptAge(@BotTextValue String age) {
        return "You are " + age + " years old. Confirm? (yes/no)";
    }
    
    @BotTextDefault
    @BotChatState("WAITING_AGE")
    public String invalidAge() {
        return "Please enter a valid age (1-3 digits)";
    }
    
    @BotText("yes")
    @BotChatState("WAITING_CONFIRMATION")
    @BotClearChatState
    public String confirmRegistration() {
        return " Registration complete! Your profile has been created.";
    }
    
    @BotText("no")
    @BotChatState("WAITING_CONFIRMATION")
    @BotClearChatState
    public String cancelRegistration() {
        return "Registration cancelled. Type /register to start over.";
    }
}
```

## How Handler Dispatch Works with State

When an update arrives:

1. **Tier 1: State Handlers** — Check if handler's `@BotChatState` matches user's current state
   - If user is in `"WAITING_AGE"`, only `@BotChatState("WAITING_AGE")` handlers are checked
   - Highest priority

2. **Tier 2: Non-State Handlers** — Check regular handlers (no `@BotChatState`)
   - Execute if no Tier 1 match
   - User can always use commands like `/cancel` even in any state

3. **Tier 3: Default Handlers** — `@BotDefaultHandler`
   - Execute if no Tier 1 or Tier 2 match

Example:

```java
@BotController
public class SmartBot {
    
    @BotCommand("/cancel")
    public String cancelAnywhere() {
        // No @BotChatState → always available
        return "Cancelled!";
    }
    
    @BotCommand("/status")
    @BotChatState("REGISTRATION")
    public String statusInRegistration() {
        // Only if user is in "REGISTRATION" state
        return "You are registering...";
    }
    
    @BotCommand("/status")
    public String statusDefault() {
        // If user is NOT in "REGISTRATION" state
        return "You are not registering.";
    }
}
```

When user types `/cancel` in ANY state, the first handler executes (no state restriction). When user types `/status`:
- If in `"REGISTRATION"` state → first handler (with state) executes
- Otherwise → second handler (without state) executes

## Custom BotChatStateService

Replace the in-memory implementation with Redis or database:

```java
@Component
public class RedisChatStateService implements BotChatStateService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Override
    public void setState(Long chatId, String state) {
        redisTemplate.opsForValue().set(
            "chat_state:" + chatId,
            state,
            Duration.ofDays(7) // Expire after 7 days of inactivity
        );
    }
    
    @Override
    public String getState(Long chatId) {
        return redisTemplate.opsForValue().get("chat_state:" + chatId);
        // Returns null when no state is set — NOT an empty string
    }
    
    @Override
    public void clearState(Long chatId) {
        redisTemplate.delete("chat_state:" + chatId);
    }
}
```

The framework detects and uses your implementation (due to `@ConditionalOnMissingBean`).

## BotChatStateService API

`BotChatStateService` is the interface Easygram uses internally for all state reads and writes. You can inject it into any Spring bean (services, filters, handlers) to manage state programmatically.

| Method | Description |
|---|---|
| `setState(Long chatId, String state)` | Sets the state for a chat. Throws `IllegalArgumentException` if `state` is `null` (since 0.0.2) — use `clearState` instead |
| `setState(Long chatId, Enum<?> state)` | Convenience overload — delegates to `setState(Long, String)` using `state.name()` |
| `getState(Long chatId)` | Returns the current state string, or `null` if no state is set |
| `getStateAs(Long chatId, Class<E> enumType)` | Returns the current state parsed as an enum constant, or `null` if no state is set |
| `clearState(Long chatId)` | Explicitly removes the state for a chat without a null check |

```java
@BotController
@RequiredArgsConstructor
public class RegistrationController {

    private final BotChatStateService chatStateService;

    @BotCommand("/register")
    public String startRegistration(Chat chat) {
        chatStateService.setState(chat.getId(), RegistrationState.WAITING_NAME);
        return "Enter your name:";
    }

    @BotTextDefault
    @BotChatState("WAITING_NAME")
    @BotForwardChatState("WAITING_AGE")
    public String acceptName(@BotTextValue String name, Chat chat) {
        return "Thanks, " + name + "! Now enter your age:";
    }

    @BotCommand("/cancel")
    public String cancel(Chat chat) {
        chatStateService.clearState(chat.getId()); // explicitly remove state
        return "Registration cancelled.";
    }

    @BotCommand("/status")
    public String status(Chat chat) {
        RegistrationState state = chatStateService.getStateAs(chat.getId(), RegistrationState.class);
        return state != null ? "Current step: " + state.name() : "Not in registration flow.";
    }
}
```

:::warning setState(chatId, null) removed in 0.0.2
`setState(chatId, null)` throws `IllegalArgumentException` since 0.0.2 — use `clearState(chatId)` instead.
:::

### In-Memory vs Redis: State Expiration

The built-in `InMemoryBotChatStateService` keeps state indefinitely in a `ConcurrentHashMap`.
States only clear when `@BotClearChatState` or `clearState(chatId)` is called. A Redis
backend lets you attach a TTL (e.g., `Duration.ofDays(7)`) so stale wizard states
automatically expire without manual cleanup.

## Enum-Based States

Use enum constants instead of raw strings for compile-time safety and IDE autocompletion.
`BotChatStateService` has built-in overloads for enums:

```java
public enum RegistrationState {
    WAITING_NAME, WAITING_AGE, WAITING_CONFIRMATION
}

@BotCommand("/register")
@BotForwardChatState("WAITING_NAME")
public String startRegistration() {
    return "Enter your name:";
}

// Read state as enum in a filter or service:
RegistrationState current = chatStateService.getStateAs(chatId, RegistrationState.class);

// Set state from enum:
chatStateService.setState(chatId, RegistrationState.WAITING_AGE);
```

The `@BotChatState` and `@BotForwardChatState` annotations still use string values — match them
to `RegistrationState.WAITING_NAME.name()` (i.e., the exact enum constant name in ALL_CAPS).

## Class-Level @BotChatState

`@BotChatState` can be placed on an entire `@BotController` class to restrict **every handler in that class** to the declared state — equivalent to annotating every method individually.

```java
@BotController
@BotChatState("REGISTRATION")          // applies to all methods below
public class RegistrationFlow {

    @BotText
    public String handleName(@BotTextValue String name) { ... }   // requires REGISTRATION

    @BotTextPattern("^\\d+$")
    public String handleAge(@BotTextValue String age) { ... }     // requires REGISTRATION

    @BotCommand("/cancel")
    @BotChatState                       // empty value overrides class-level → matches ANY state
    public String cancel() { return "Cancelled."; }
}
```

A method-level `@BotChatState` always overrides the class-level default. An empty method-level annotation (or `@BotChatState(BotChatState.ANY)`) opts the method out of the class restriction entirely.

## BotChatState.ANY

`BotChatState.ANY` is a constant (`""`) that explicitly signals "match any chat state". Use it instead of an empty `@BotChatState` annotation to make the intent clear:

```java
@BotController
@BotChatState("CHECKOUT")
public class CheckoutFlow {

    @BotText
    public String handleItem(@BotTextValue String item) { ... }    // requires CHECKOUT

    @BotCommand("/cancel")
    @BotChatState(BotChatState.ANY)   // fires in any state — explicit, self-documenting
    public String cancel() { ... }
}
```

Both `@BotChatState` and `@BotChatState(BotChatState.ANY)` are functionally identical — the constant just makes code easier to read in class-level override scenarios.

## Re-Entry Behavior

If a user re-sends `/register` while already in the `WAITING_AGE` state:

- The dispatch falls into **Tier 2** (non-state handlers) because `/register` has no `@BotChatState`
- The existing state is **not automatically cleared** — the user is still in `WAITING_AGE`
- Add an explicit `@BotForwardChatState("WAITING_NAME")` on `/register` to restart the flow:

```java
@BotCommand("/register")
@BotForwardChatState("WAITING_NAME") // Always resets flow to beginning
public String startRegistration() {
    return "Starting over. Enter your name:";
}
```

## Group Chat Caution

Chat state is keyed by `chatId` (not `userId`). In group chats, **all members share the same
state**. If two users simultaneously send messages to a group bot, their updates race through
the same state value. For group-aware bots, key state on a composite `chatId + userId` string,
or restrict stateful handlers to private chats only:

```java
@BotCommand("/register")
public String startRegistration(Chat chat) {
    if (!chat.isUserChat()) {
        return "Registration is only available in private chat.";
    }
    // ... proceed
}
```

## State Naming Conventions

- Use UPPERCASE_WITH_UNDERSCORES: `"WAITING_NAME"`, `"PAYMENT_PENDING"`
- Keep names descriptive but concise
- Document state transitions in comments

## Testing States

Mock the state service in tests:

```java
@SpringBootTest
public class RegistrationBotTest {
    
    @MockBean
    private BotChatStateService chatStateService;
    
    @Autowired
    private RegistrationBot bot;
    
    @Test
    public void testAgeValidation() {
        // Mock user is in WAITING_AGE state
        when(chatStateService.getState(123L)).thenReturn("WAITING_AGE");
        
        String response = bot.acceptAge("25");
        
        assertThat(response).contains("Confirm?");
    }
}
```

---

Next: Learn about all [return types](return-types) supported by handlers.
