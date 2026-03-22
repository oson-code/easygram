package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;


/**
 * Strategy interface for processing the value returned by a handler method.
 * Implementations translate specific return types (e.g., {@link org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod},
 * {@code Collection}, or custom types) into {@link uz.osoncode.easygram.core.model.BotResponse}
 * entries that are later executed by the framework.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotReturnTypeHandler {

    /**
     * Determines whether this handler can process the return type of the given method.
     *
     * @param method the handler method whose return type is being inspected; must not be {@code null}
     * @return {@code true} if this handler supports the method's return type, {@code false} otherwise
     */
    boolean supportsReturnType(Method method);

    /**
     * Processes the return value produced by a handler method and records any resulting
     * API methods in the response.
     *
     * @param botRequest  the current request context; must not be {@code null}
     * @param botResponse the mutable response to which API calls should be added; must not be {@code null}
     * @param returnValue the value returned by the handler method; may be {@code null} for void methods
     */
    void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue);

    /**
     * Processes the return value produced by a handler method with access to the originating
     * {@link Method}, enabling annotation-driven behaviour (e.g. resolving
     * {@link uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup} or
     * {@link uz.osoncode.easygram.core.bind.annotation.BotClearMarkup}).
     *
     * <p>The default implementation ignores {@code method} and delegates to
     * {@link #handleReturnType(BotRequest, BotResponse, Object)}, so existing handler
     * implementations are unaffected unless they explicitly override this method.</p>
     *
     * @param botRequest  the current request context; must not be {@code null}
     * @param botResponse the mutable response to which API calls should be added; must not be {@code null}
     * @param returnValue the value returned by the handler method; may be {@code null} for void methods
     * @param method      the handler method that produced the return value; must not be {@code null}
     */
    default void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue, Method method) {
        handleReturnType(botRequest, botResponse, returnValue);
    }

    /**
     * Determines whether this handler can process a single runtime element value.
     *
     * <p>Used by {@code BotMixedCollectionReturnTypeHandler} to dispatch individual
     * elements of a heterogeneous collection to the appropriate handler.
     * Override this method to enable element-level dispatch for your handler type.</p>
     *
     * <p>The default implementation returns {@code false} — existing handlers are unaffected
     * unless they explicitly opt in.</p>
     *
     * @param element the runtime element value to inspect; may be {@code null}
     * @return {@code true} if this handler can process the given element, {@code false} otherwise
     */
    default boolean supportsElement(Object element) {
        return false;
    }
}
