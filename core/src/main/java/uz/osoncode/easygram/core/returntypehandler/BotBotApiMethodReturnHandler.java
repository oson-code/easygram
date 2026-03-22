package uz.osoncode.easygram.core.returntypehandler;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;

/**
 * {@link BotReturnTypeHandler} implementation that handles handler methods returning
 * a single {@link BotApiMethod} instance.
 *
 * <p>When the return value is non-null, it is cast to {@code BotApiMethod<?>} and added
 * to the {@link BotResponse} for subsequent dispatch to the Telegram API.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotBotApiMethodReturnHandler implements BotReturnTypeHandler {

    /**
     * Determines whether this handler supports the return type of the given method.
     *
     * <p>Returns {@code true} if the method's return type is {@link BotApiMethod}
     * or any of its subclasses.</p>
     *
     * @param method the handler method whose return type is checked; must not be {@code null}
     * @return {@code true} if the return type is assignable from {@link BotApiMethod}
     */
    @Override
    public boolean supportsReturnType(Method method) {
        return BotApiMethod.class.isAssignableFrom(method.getReturnType());
    }

    /**
     * Processes the return value by adding it to the {@link BotResponse}.
     *
     * <p>If {@code returnValue} is non-null, it is cast to {@code BotApiMethod<?>}
     * and registered in {@code botResponse} so the framework can send it to Telegram.
     * A {@code null} return value is silently ignored.</p>
     *
     * @param botRequest  the current bot request providing context for the update; must not be {@code null}
     * @param botResponse the mutable response object to which the API method is added; must not be {@code null}
     * @param returnValue the value returned by the handler method; may be {@code null}
     */
    @Override
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {
        if (returnValue != null) {
            botResponse.addBotApiMethod((BotApiMethod<?>) returnValue);
        }
    }

    /**
     * Returns {@code true} when the element is a {@link BotApiMethod} instance.
     * Used by {@code BotMixedCollectionReturnTypeHandler} for per-element dispatch.
     */
    @Override
    public boolean supportsElement(Object element) {
        return element instanceof BotApiMethod<?>;
    }
}
