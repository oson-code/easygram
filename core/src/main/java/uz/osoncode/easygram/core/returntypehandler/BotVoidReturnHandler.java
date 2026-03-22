package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;

/**
 * {@link BotReturnTypeHandler} implementation that handles handler methods declared with a
 * {@code void} return type.
 *
 * <p>This handler is a deliberate no-op: because {@code void} methods produce no return value,
 * nothing is added to the {@link BotResponse}. Any side-effects the handler needs to perform
 * must be executed directly within the handler method body, for example by injecting a
 * {@link BotResponse} parameter and calling its methods directly.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotVoidReturnHandler implements BotReturnTypeHandler {

    /**
     * Determines whether this handler supports the return type of the given method.
     *
     * <p>Returns {@code true} when the method's return type is the primitive {@code void} type
     * ({@link Void#TYPE}).</p>
     *
     * @param method the handler method whose return type is checked; must not be {@code null}
     * @return {@code true} if the return type is {@code void}
     */
    @Override
    public boolean supportsReturnType(Method method) {
        return Void.TYPE.isAssignableFrom(method.getReturnType());
    }

    /**
     * No-op implementation: does nothing because {@code void} methods produce no return value.
     *
     * @param botRequest  the current bot request; not used by this implementation
     * @param botResponse the mutable response object; not modified by this implementation
     * @param returnValue always {@code null} for {@code void} methods; ignored
     */
    @Override
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {

    }
}
