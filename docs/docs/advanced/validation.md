---
id: validation
title: Jakarta Bean Validation
---

# Jakarta Bean Validation

Easygram integrates with Jakarta Bean Validation (formerly Java EE Validation / Hibernate
Validator) to let you declare constraints directly on handler method parameters. When a
constraint fails, the framework throws a `ConstraintViolationException` before the handler
body runs — no validation boilerplate inside your methods.

## How it works

After argument resolvers populate the handler method parameters, `MethodInvocationFilter`
calls `ExecutableValidator.validateParameters()` on the resolved argument array:

```
@BotCommand / @BotTextDefault / … matched
   ↓
Argument resolvers — @BotTextValue, @BotCommandQueryParam, User, Chat, …
   ↓
MethodInvocationFilter:
  ExecutableValidator.validateParameters(bean, method, args)
   ↓ violation found?
     YES → throw ConstraintViolationException (handler body never runs)
     NO ↓
Handler method body executes
```

Validation is **conditional**: the `Validator` bean must be present in the application
context. If no `Validator` is configured, the step is silently skipped.

## Enabling validation

`spring-boot-starter-validation` is a **transitive dependency** of most Easygram transport
modules (`longpolling`, `webhook`, `messaging-kafka-consumer`, etc.). In practice you rarely
need to declare it explicitly. If your project only uses `core-api` you can add it manually:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Spring Boot autoconfigures a `Validator` bean automatically when this dependency is on the
classpath.

## Supported parameter types

Any handler parameter resolved by a built-in or custom argument resolver can carry Jakarta
constraint annotations:

| Resolver | Annotation | Example |
|---|---|---|
| Text message body | `@BotTextValue` | `@NotBlank @Size(max=200) String text` |
| Command query param | `@BotCommandQueryParam` | `@Min(1) @Max(100) int page` |
| Callback query data | `@BotCallbackQueryData` | `@NotBlank String data` |
| Full command string | `@BotCommandValue` | `@Pattern(regexp="^/\\w+") String cmd` |

Telegram-native objects injected by type (`User`, `Chat`, `Update`, `BotRequest`) are not
typically constrained — use `@Valid` on your own POJO parameters instead.

## Basic example

```java
@BotController
public class SearchController {

    @BotCommand("/search")
    public String search(
            @BotCommandQueryParam("q") @NotBlank @Size(min = 2, max = 100) String query,
            @BotCommandQueryParam("page") @Min(1) int page) {
        // Only reached when query is non-blank, between 2-100 chars, and page >= 1
        return "Searching for: " + query + " (page " + page + ")";
    }

    @BotExceptionHandler(ConstraintViolationException.class)
    public String onValidationError(ConstraintViolationException ex) {
        String messages = ex.getConstraintViolations().stream()
                .map(v -> "• " + v.getMessage())
                .collect(Collectors.joining("\n"));
        return " Invalid input:\n" + messages;
    }
}
```

:::tip Exception handler scope
`@BotExceptionHandler` methods are scoped per controller — the handler above only catches
violations thrown by methods in `SearchController`. Place a shared handler in a dedicated
`@BotController` class if you want to cover all controllers from one place.
:::

## Common constraints

| Annotation | Applies to | Example |
|---|---|---|
| `@NotNull` | Any | `@NotNull String text` |
| `@NotBlank` | `String` | `@NotBlank String name` |
| `@NotEmpty` | String, Collection | `@NotEmpty List<String> ids` |
| `@Size(min, max)` | String, Collection | `@Size(min=2, max=50) String city` |
| `@Min(value)` | Numeric | `@Min(1) int quantity` |
| `@Max(value)` | Numeric | `@Max(120) int age` |
| `@Pattern(regexp)` | String | `@Pattern(regexp="^\\+\\d{7,15}$") String phone` |
| `@Email` | String | `@Email String address` |
| `@Positive` | Numeric | `@Positive long chatId` |
| `@PositiveOrZero` | Numeric | `@PositiveOrZero int offset` |

All Jakarta Validation 3.x constraints are supported. Third-party constraints (Hibernate
Validator extras, custom annotations) also work as long as they implement `ConstraintValidator`.

## Custom constraint messages

Override the default message in the annotation:

```java
@NotBlank(message = "Name must not be empty")
@Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
String name
```

Or use `ValidationMessages.properties` (standard Jakarta Validation resource bundle):

```properties
# src/main/resources/ValidationMessages.properties
uz.example.constraint.name.notblank=Name must not be empty
uz.example.constraint.name.size=Name must be between {min} and {max} characters
```

```java
@NotBlank(message = "{uz.example.constraint.name.notblank}")
@Size(min = 2, max = 50, message = "{uz.example.constraint.name.size}")
String name
```

## Custom validators

Implement `ConstraintValidator<A, T>` to create reusable annotations:

```java
// 1. The annotation
@Documented
@Constraint(validatedBy = TelegramUsernameValidator.class)
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TelegramUsername {
    String message() default "Invalid Telegram username";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// 2. The validator
public class TelegramUsernameValidator
        implements ConstraintValidator<TelegramUsername, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true; // let @NotNull handle null
        return value.startsWith("@") && value.length() >= 6 && value.length() <= 33;
    }
}

// 3. Usage on a handler parameter
@BotCommand("/lookup")
public String lookup(@BotCommandQueryParam("username") @TelegramUsername String username) {
    return "Looking up: " + username;
}
```

## Exception handling patterns

### Minimal — plain text reply

```java
@BotExceptionHandler(ConstraintViolationException.class)
public String onValidationError(ConstraintViolationException ex) {
    String messages = ex.getConstraintViolations().stream()
            .map(v -> "• " + v.getMessage())
            .collect(Collectors.joining("\n"));
    return " Invalid input:\n" + messages;
}
```

### With i18n (`core-i18n`)

When using `core-i18n`, return a `LocalizedReply` with a format argument derived from the
violations:

```java
@BotExceptionHandler(ConstraintViolationException.class)
public LocalizedReply onValidationError(ConstraintViolationException ex) {
    String detail = ex.getConstraintViolations().stream()
            .map(v -> "• " + v.getMessage())
            .collect(Collectors.joining("\n"));
    // "error.validation= Invalid input:\n{0}" in messages/bot.properties
    return LocalizedReply.of("error.validation", detail);
}
```

### Structured — field-level feedback

```java
@BotExceptionHandler(ConstraintViolationException.class)
public String onValidationError(ConstraintViolationException ex) {
    return ex.getConstraintViolations().stream()
            .map(v -> {
                // extract the parameter name from the path
                String field = StreamSupport
                        .stream(v.getPropertyPath().spliterator(), false)
                        .reduce((a, b) -> b)
                        .map(Path.Node::getName)
                        .orElse("input");
                return String.format("• %s: %s", field, v.getMessage());
            })
            .collect(Collectors.joining("\n", " Validation errors:\n", ""));
}
```

## Disabling validation for a specific handler

If you need to bypass validation for one handler while keeping it everywhere else, declare a
no-op override by registering a custom `Validator` implementation just for that case — or
simply omit constraint annotations on that handler's parameters.

To **globally disable** validation, exclude the `Validator` bean:

```java
@SpringBootApplication(exclude = ValidationAutoConfiguration.class)
public class MyBotApplication { ... }
```

## Constraint groups

Use groups to run different validation rules depending on context:

```java
public interface OnRegistration {}
public interface OnUpdate {}

@BotCommand("/register")
public String register(
        @BotCommandQueryParam("email")
        @NotBlank(groups = OnRegistration.class)
        @Email(groups = {OnRegistration.class, OnUpdate.class})
        String email) { ... }
```

:::note Groups and sequential validation
The default group (`Default`) is always validated. Custom groups require explicit wiring
via a `@GroupSequence` or a custom `Validator` configuration — standard Jakarta Validation
behaviour.
:::

---

See also:
- [Parameter Injection](../core-concepts/parameter-injection) — all injectable parameter types
- [Exception Handling](../core-concepts/exception-handling) — `@BotExceptionHandler` reference
- [i18n Registration Bot](../examples/i18n-registration-bot) — live example with `@NotBlank` + `@Size`
