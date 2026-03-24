---
id: exception-handling
title: Exception Handling
---

# Exception Handling

Gracefully handle errors with `@BotExceptionHandler`.

## @BotExceptionHandler

Catch specific exceptions in a handler:

```java
@BotExceptionHandler(IllegalArgumentException.class)
public String handleValidationError(IllegalArgumentException e) {
    return "Validation failed: " + e.getMessage();
}

@BotExceptionHandler(SQLException.class)
public String handleDatabaseError(SQLException e) {
    log.error("Database error", e);
    return "Sorry, a database error occurred.";
}
```

Scope: The containing `@BotController` class only.

## Multiple Exception Handlers

```java
@BotController
public class MyBot {
    
    @BotExceptionHandler(IllegalArgumentException.class)
    public String handleValidation(IllegalArgumentException e) {
        return "Invalid input: " + e.getMessage();
    }
    
    @BotExceptionHandler(NullPointerException.class)
    public String handleNull(NullPointerException e) {
        return "Null error occurred";
    }
    
    @BotExceptionHandler(Exception.class)
    public String handleGeneric(Exception e) {
        return "An error occurred";
    }
}
```

Most specific exception handler is tried first. If no match, generic handlers catch it.

## @BotControllerAdvice

Global exception handling across all `@BotController` classes:

```java
@BotControllerAdvice
public class GlobalExceptionHandler {
    
    @BotExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorized(UnauthorizedException e) {
        return "You don't have permission!";
    }
    
    @BotExceptionHandler(Exception.class)
    public String handleAll(Exception e) {
        log.error("Unhandled exception", e);
        return "Sorry, an error occurred!";
    }
}
```

Optional scope by packages, types, or annotations:

```java
// Scope to specific packages
@BotControllerAdvice(basePackages = "com.example.payment")
public class PaymentExceptionHandler { ... }

// Scope to specific classes
@BotControllerAdvice(assignableTypes = PaymentController.class)
public class PaymentHandler { ... }

// Scope to classes with specific annotation
@BotControllerAdvice(annotations = RequiresAuth.class)
public class AuthHandler { ... }
```

## Exception Resolution Order

1. Most specific, state-matched handler in the same `@BotController`
2. Generic handler in the same `@BotController`
3. Most specific, state-matched handler in `@BotControllerAdvice` (matching scope)
4. Generic handler in `@BotControllerAdvice`
5. Default error handling (logs unhandled exception)

## @BotChatState on Exception Handlers

`@BotExceptionHandler` methods respect `@BotChatState` — a handler annotated with a state restriction is only selected when the current chat is in one of the declared states at the time the exception occurs.

This lets you show context-appropriate error messages depending on which flow the user is currently in.

### Method-level state restriction

```java
@BotController
public class RegistrationBot {

    @BotChatState("AWAITING_NAME")
    @BotText
    public String handleName(@BotTextValue String name) {
        if (name.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
        // ...
    }

    @BotChatState("AWAITING_EMAIL")
    @BotText
    public String handleEmail(@BotTextValue String email) {
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email");
        // ...
    }

    // Only fires when chat is in AWAITING_NAME state
    @BotChatState("AWAITING_NAME")
    @BotExceptionHandler(IllegalArgumentException.class)
    public String handleNameError(IllegalArgumentException e) {
        return "Please enter a valid name: " + e.getMessage();
    }

    // Only fires when chat is in AWAITING_EMAIL state
    @BotChatState("AWAITING_EMAIL")
    @BotExceptionHandler(IllegalArgumentException.class)
    public String handleEmailError(IllegalArgumentException e) {
        return "Please enter a valid email address.";
    }

    // No @BotChatState — catches any remaining IllegalArgumentException in any state
    @BotExceptionHandler(IllegalArgumentException.class)
    public String handleGenericValidation(IllegalArgumentException e) {
        return "Invalid input: " + e.getMessage();
    }
}
```

State-specific handlers are evaluated before state-agnostic ones, so the most specific match always wins.

### Class-level state restriction

Placing `@BotChatState` on the controller class applies the restriction to all `@BotExceptionHandler` methods in that class, just like it does for regular handlers:

```java
@BotController
@BotChatState("PAYMENT_FLOW")
public class PaymentController {

    @BotText
    public String handleAmount(@BotTextValue String amount) {
        // ...
    }

    // Inherited class-level state — only active during PAYMENT_FLOW
    @BotExceptionHandler(PaymentException.class)
    public String handlePaymentError(PaymentException e) {
        return "Payment failed: " + e.getMessage();
    }

    // Method-level @BotChatState with empty value overrides class-level → matches any state
    @BotChatState
    @BotExceptionHandler(Exception.class)
    public String handleAnyState(Exception e) {
        return "An error occurred.";
    }
}
```

## Example: Complete Error Handling

```java
@BotController
public class OrderBot {
    
    @Autowired
    private OrderService orderService;
    
    @BotCommand("/order")
    public String placeOrder(@BotCommandQueryParam("id") String orderId) {
        Order order = orderService.getOrder(orderId);  // May throw OrderNotFoundException
        return "Order: " + order.getName() + ", Price: $" + order.getPrice();
    }
    
    @BotExceptionHandler(OrderNotFoundException.class)
    public String handleOrderNotFound(OrderNotFoundException e) {
        return "Order not found: " + e.getOrderId();
    }
    
    @BotExceptionHandler(IllegalArgumentException.class)
    public String handleValidation(IllegalArgumentException e) {
        return "Invalid order ID format";
    }
}

@BotControllerAdvice
public class GlobalHandlers {
    
    @BotExceptionHandler(DatabaseException.class)
    public String handleDatabase(DatabaseException e) {
        log.error("Database error", e);
        return "Database error - please try again later";
    }
    
    @BotExceptionHandler(Exception.class)
    public String handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return "Sorry, an unexpected error occurred!";
    }
}
```

## Logging Errors

```java
@BotExceptionHandler(Exception.class)
public String handleError(Exception e) {
    log.error("Handler failed", e);  // Log before responding
    return "An error occurred. Please try again.";
}
```

## Testing Exception Handlers

```java
@SpringBootTest
public class ErrorHandlingTest {
    
    @Autowired
    private MyBot bot;
    
    @Test
    public void testExceptionHandling() {
        assertThrows(
            IllegalArgumentException.class,
            () -> bot.processOrder(null)
        );
        
        String response = bot.handleValidation(
            new IllegalArgumentException("Invalid")
        );
        
        assertThat(response).contains("Validation failed");
    }
}
```

---

Ready to learn about transports? Check out [Transport Guides](../transports/long-polling-guide).
