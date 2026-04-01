package uz.osoncode.easygram.core.argumentresolver;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that injects the exception captured during bot-handler execution.
 *
 * <p>This resolver is primarily used in exception-handler methods (analogous to Spring MVC's
 * {@code @ExceptionHandler}).  It supports any parameter whose declared type is
 * {@link Throwable} or one of its subclasses, and injects the exception that was stored on
 * the current {@link BotRequest}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotThrowableArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} when the parameter type is {@link Throwable} or any subclass
     * (e.g. {@link RuntimeException}, {@link Exception}, custom exception types).</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is assignable from {@link Throwable},
     *         {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return Throwable.class.isAssignableFrom(ParameterUtils.effectiveType(parameter));
    }

    /**
     * Retrieves the throwable captured in the current {@link BotRequest} and injects it
     * into the exception-handler method parameter.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request carrying the captured {@link Throwable}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link Throwable} stored on the request, or {@code null} if none was recorded
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botRequest.getThrowable();
    }
}
