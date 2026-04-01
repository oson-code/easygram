package uz.osoncode.easygram.core.argumentresolver;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that injects the current {@link BotResponse} accumulator into a handler method.
 *
 * <p>The {@link BotResponse} object is a mutable accumulator that collects Telegram API calls
 * (e.g. messages to send) produced during the handling of one update.  Injecting it into a
 * handler method allows the method to add or inspect pending responses before they are
 * dispatched.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotResponseArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link BotResponse}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link BotResponse}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(BotResponse.class);
    }

    /**
     * Returns the {@link BotResponse} accumulator, giving the handler method direct access
     * to the current response being built for this update.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request (unused here)
     * @param botResponse the mutable response accumulator to inject
     * @return the {@link BotResponse} for the current invocation, never {@code null}
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botResponse;
    }
}
