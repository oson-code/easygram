package uz.osoncode.easygram.core.argumentresolver;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Strategy interface for resolving individual handler method parameters from the current request context.
 * Each implementation handles a specific parameter type or annotation (e.g., injecting the
 * current {@link org.telegram.telegrambots.meta.api.objects.Update} or a custom value extracted
 * from the incoming message).
 * The framework queries all registered resolvers via {@link #supportsParameter(Parameter)} and
 * delegates to the first matching resolver's {@link #resolveArgument} to obtain the value.
 *
 * <h2>Optional parameter support</h2>
 * <p>Parameters declared as {@code Optional<T>} are automatically unwrapped by
 * {@code BotArgumentResolverFactory}: it calls
 * {@link ParameterUtils#effectiveType(Parameter)} to determine {@code T}, selects the
 * matching resolver, and wraps the resolved value in {@code Optional.ofNullable()}.
 * If no resolver matches the inner type, {@code Optional.empty()} is injected.</p>
 *
 * <p>Implementations that match by type should use
 * {@link ParameterUtils#effectiveType(Parameter)} instead of
 * {@code parameter.getType()} so that both {@code T} and {@code Optional<T>} declarations
 * are supported without any additional code in the resolver.
 * Annotation-based resolvers (those that call {@code parameter.isAnnotationPresent(...)})
 * do not need any changes — the annotation is present on the parameter regardless of wrapping.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotArgumentResolver {

    /**
     * Determines whether this resolver can provide a value for the given method parameter.
     *
     * @param parameter the method parameter to evaluate; must not be {@code null}
     * @return {@code true} if this resolver can resolve the parameter, {@code false} otherwise
     */
    boolean supportsParameter(Parameter parameter);

    /**
     * Resolves and returns the argument value to be injected for the given method parameter.
     * Called only when {@link #supportsParameter(Parameter)} returned {@code true}.
     *
     * @param parameter   the method parameter for which an argument is needed; must not be {@code null}
     * @param botRequest  the current request context providing access to the update and client; must not be {@code null}
     * @param botResponse the mutable response object for the current request; must not be {@code null}
     * @return the resolved argument value, or {@code null} if the parameter is optional and not present
     */
    Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse);
}
