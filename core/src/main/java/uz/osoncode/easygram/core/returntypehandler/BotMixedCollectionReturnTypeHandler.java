package uz.osoncode.easygram.core.returntypehandler;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link BotReturnTypeHandler} that handles handler methods returning a heterogeneous
 * {@link Collection} whose elements may be of different supported types
 * (e.g., {@link String}, {@link BotApiMethod}, or any custom type with a matching handler).
 *
 * <p>Each element is dispatched at runtime to the first {@link BotReturnTypeHandler} in the
 * registered handler list whose {@link BotReturnTypeHandler#supportsElement(Object)} returns
 * {@code true}. Elements with no matching handler are logged at WARN level.</p>
 *
 * <p>Handler selection is cached per element class to avoid re-scanning the handler list
 * for every element, reducing dispatch cost from O(n×m) to O(n) after the first occurrence
 * of each type in the collection.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @BotCommand("/demo")
 * public List<Object> onDemo(User user) {
 *     return List.of(
 *         "Hello " + user.getFirstName() + "!",         // → BotStringReturnHandler
 *         LocalizedReply.of("${welcome}"),               // → BotLocalizedReplyReturnTypeHandler
 *         SendMessage.builder()...build()                // → BotBotApiMethodReturnHandler
 *     );
 * }
 * }</pre>
 *
 * <h2>What qualifies as a "mixed" collection?</h2>
 * <p>Any method whose return type is a {@link Collection} with a non-{@link BotApiMethod}
 * element type qualifies — including raw {@code Collection}, {@code Collection<Object>},
 * {@code Collection<?>}, {@code List<Object>}, etc.
 * {@code Collection<BotApiMethod<?>>} is handled by the dedicated
 * {@link BotBotApiMethodsReturnHandler} and is excluded here.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class BotMixedCollectionReturnTypeHandler implements BotReturnTypeHandler {

    private final List<BotReturnTypeHandler> handlers;

    public BotMixedCollectionReturnTypeHandler(List<BotReturnTypeHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * Returns {@code true} when the method returns a {@link Collection} whose element type is
     * NOT {@link BotApiMethod} (or a subclass). Raw collections and {@code Collection<Object>}
     * / {@code Collection<?>} are all accepted.
     */
    @Override
    public boolean supportsReturnType(Method method) {
        if (!Collection.class.isAssignableFrom(method.getReturnType())) {
            return false;
        }
        Type genericType = method.getGenericReturnType();
        if (!(genericType instanceof ParameterizedType pt)) {
            // Raw Collection — accept as mixed
            return true;
        }
        Type elementType = pt.getActualTypeArguments()[0];

        // Unwrap wildcards: Collection<?>, Collection<? extends Foo>
        if (elementType instanceof WildcardType wt) {
            Type[] upper = wt.getUpperBounds();
            elementType = (upper.length > 0) ? upper[0] : Object.class;
        }

        // Unwrap parameterised element types: Collection<BotApiMethod<?>> → BotApiMethod
        if (elementType instanceof ParameterizedType ept) {
            elementType = ept.getRawType();
        }

        // Exclude Collection<BotApiMethod<...>> — handled by BotBotApiMethodsReturnHandler
        if (elementType instanceof Class<?> c && BotApiMethod.class.isAssignableFrom(c)) {
            return false;
        }

        return true;
    }

    /**
     * Iterates the collection and dispatches each non-null element to the first handler
     * whose {@link BotReturnTypeHandler#supportsElement(Object)} returns {@code true}.
     * Handler selection is cached per element class (O(1) after first occurrence).
     * Elements with no matching handler are logged at WARN level.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {
        if (Objects.isNull(returnValue)) {
            return;
        }
        // Cache handler lookups by element class to avoid re-scanning for each element.
        Map<Class<?>, BotReturnTypeHandler> handlerCache = new IdentityHashMap<>();
        for (Object element : (Collection<Object>) returnValue) {
            if (Objects.isNull(element)) {
                continue;
            }
            BotReturnTypeHandler handler = handlerCache.computeIfAbsent(element.getClass(), cls -> {
                for (BotReturnTypeHandler h : handlers) {
                    if (h != this && h.supportsElement(element)) {
                        return h;
                    }
                }
                return null;
            });
            if (handler != null) {
                handler.handleReturnType(botRequest, botResponse, element);
            } else {
                log.warn("No BotReturnTypeHandler found for collection element of type '{}' — element dropped. " +
                         "Register a custom BotReturnTypeHandler bean to support this type.",
                        element.getClass().getName());
            }
        }
    }
}
