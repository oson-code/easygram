package uz.osoncode.easygram.core.returntypehandler;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Factory that holds all registered {@link BotReturnTypeHandler} instances and selects
 * the correct one for processing a handler method's return value.
 *
 * <p>When a bot handler method has been invoked and produced a result, this factory
 * iterates over the registered handlers and returns the first one that
 * {@link BotReturnTypeHandler#supportsReturnType supports} the given method's return type.
 * If no matching handler is found, an {@link IllegalArgumentException} is thrown to signal
 * a configuration error.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotReturnTypeHandlerFactory {

    /** Ordered list of return-type handlers consulted when selecting a handler for a method. */
    private final List<BotReturnTypeHandler> returnTypeHandlers;

    /**
     * Returns the first {@link BotReturnTypeHandler} that supports the return type of the
     * given method.
     *
     * @param method the handler method whose return type needs to be processed
     * @return the matching {@link BotReturnTypeHandler}, never {@code null}
     * @throws IllegalArgumentException if no registered handler supports the method's return type
     */
    public BotReturnTypeHandler getReturnTypeHandler(Method method) {
        return returnTypeHandlers.stream()
                .filter(handler -> handler.supportsReturnType(method))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suitable return type handler found for method: " + method));
    }

    /**
     * Returns the first {@link BotReturnTypeHandler} that supports the given runtime value.
     *
     * <p>This method delegates to {@link BotReturnTypeHandler#supportsElement(Object)} to determine
     * support based on the actual object instance rather than the method signature. This is crucial
     * when the return value has been transformed (e.g. by {@code @BotReplyMarkup}) into a different
     * type (like {@code String} -> {@code PlainReply}).</p>
     *
     * @param value the runtime value to process; must not be null
     * @return the matching {@link BotReturnTypeHandler}, never {@code null}
     * @throws IllegalArgumentException if no registered handler supports the value type
     */
    public BotReturnTypeHandler getReturnTypeHandler(Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Handler method returned null. Declare the method as void, or return a supported type.");
        }
        return returnTypeHandlers.stream()
                .filter(handler -> handler.supportsElement(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suitable return type handler found for value type: " + value.getClass().getName()));
    }
}
