# Easygram — Extension Guide

## Overview

Every Easygram component is an SPI interface backed by a `@ConditionalOnMissingBean`
default. Declare your own Spring `@Bean` to replace any default or add new behaviour.

---

## Custom BotFilter

`BotFilter` intercepts every `Update` before and after handler dispatch.
Use it for authentication, rate limiting, metrics, or cross-cutting concerns.

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.BotFilterChain;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

@Slf4j
@Component
public class RateLimitFilter implements BotFilter {

    @Override
    public int getOrder() {
        return 100; // lower = higher priority; runs after MDC / context filters
    }

    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) {
        Long userId = request.getUser() == null ? null : request.getUser().getId();
        if (isRateLimited(userId)) {
            log.warn("Rate limit exceeded for userId={}", userId);
            return; // drop the request without calling chain
        }
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(Long userId) { /* your logic */ return false; }
}
```

**Key rules:**
- Always call `chain.doFilter(request, response)` unless you intentionally short-circuit.
- `getOrder()` follows Spring's `Ordered` convention: lower integer = higher priority.
- Filter beans are auto-discovered; no manual registration needed.

---

## Custom BotArgumentResolver

Add support for injecting a custom type into handler method parameters.

```java
import org.springframework.stereotype.Component;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolver;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

@Component
public class CurrentUserResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.getType() == MyAppUser.class;
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest request, BotResponse response) {
        Long telegramUserId = request.getUser().getId();
        return userRepository.findByTelegramId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));
    }
}
```

The resolver is auto-discovered and evaluated in registration order. The first resolver
whose `supportsParameter()` returns `true` wins.

---

## Custom BotReplyAction

Add a new Telegram Bot API call to the `PlainReply`/`LocalizedReply` dispatch pipeline —
for example, forwarding every reply to an audit channel, or pinning a message automatically.

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
        return !options.callbackAlert(); // fire on every regular send
    }

    @Override
    public void execute(BotRequest request, BotResponse response,
                        String resolvedText, ReplyOptions options) {
        response.addBotApiMethod(ForwardMessage.builder()
                .chatId(AUDIT_CHAT_ID)
                .fromChatId(String.valueOf(request.getChat().getId()))
                .messageId(request.getMessage().getMessageId())
                .build());
    }

    @Override
    public int getOrder() {
        return 30; // after built-in actions at 10 and 20
    }
}
```

**Key rules:**
- Returning `true` from `supports()` does not stop other actions — the chain always runs all matching actions.
- Use `getOrder()` to position the action relative to the built-ins (10 = send/edit, 20 = callback answer).
- Actions are auto-discovered as Spring beans; no manual registration is needed.

---

## Custom BotReturnTypeHandler

Add support for a new handler method return type.

```java
import org.springframework.stereotype.Component;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;

@Component
public class MyDtoReturnTypeHandler implements BotReturnTypeHandler {

    @Override
    public boolean supportsReturnType(Class<?> returnType) {
        return MyDto.class.isAssignableFrom(returnType);
    }

    @Override
    public void handleReturnType(BotRequest request, BotResponse response,
                                  Object returnValue, Method method) {
        MyDto dto = (MyDto) returnValue;
        // queue a Telegram API call on the response
        response.addApiMethod(new SendMessage(
                String.valueOf(request.getChat().getId()), dto.toText()));
    }
}
```

---

## Custom BotChatStateService

Replace the default in-memory implementation with a Redis- or database-backed one:

```java
import org.springframework.stereotype.Component;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;

@Component
public class RedisBotChatStateService implements BotChatStateService {

    private final RedisTemplate<String, String> redis;

    public RedisBotChatStateService(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    @Override
    public String getState(Long chatId) {
        return redis.opsForValue().get("bot:state:" + chatId);
    }

    @Override
    public void setState(Long chatId, String state) {
        redis.opsForValue().set("bot:state:" + chatId, state);
    }

    @Override
    public void clearState(Long chatId) {
        redis.delete("bot:state:" + chatId);
    }
}
```

Because the autoconfigured `InMemoryBotChatStateService` is `@ConditionalOnMissingBean`,
your `@Component` takes precedence automatically.

---

## Custom BotHandler (Low-level)

For update types not covered by `@BotController` annotations, implement `BotHandler`:

```java
import org.springframework.stereotype.Component;
import uz.osoncode.easygram.core.handler.BotHandler;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

@Component
public class PollUpdateHandler implements BotHandler {

    @Override
    public boolean supports(BotRequest request) {
        return request.getUpdate().getPoll() != null;
    }

    @Override
    public void handle(BotRequest request, BotResponse response) {
        var poll = request.getUpdate().getPoll();
        // handle poll update
    }
}
```

---

## BotConfigurer

For programmatic framework customisation at startup (e.g. registering markups or
configuring transport settings), implement `BotConfigurer`:

```java
import org.springframework.stereotype.Component;
import uz.osoncode.easygram.core.config.BotConfigurer;

@Component
public class MyBotConfigurer implements BotConfigurer {

    @Override
    public TransportType transportType() {
        return TransportType.LONG_POLLING; // or read from properties
    }
}
```
