package uz.osoncode.easygram.core.argumentresolver;

import lombok.extern.slf4j.Slf4j;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that holds all registered {@link BotArgumentResolver} instances and selects
 * the correct resolver for each method parameter when a handler method is invoked.
 *
 * <p>During argument resolution, the factory iterates over the provided method parameters,
 * finds the first resolver that {@link BotArgumentResolver#supportsParameter supports} the
 * parameter, and delegates to it. If no resolver matches, {@code null} is injected for that
 * parameter position.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class BotArgumentResolverFactory {

    /** Ordered list of argument resolvers consulted during parameter resolution. */
    private final List<BotArgumentResolver> botArgumentResolvers;

    /**
     * Cache mapping each method {@link Parameter} to the resolver that supports it.
     * Parameters are static after startup so the scan needs to run only once per parameter.
     */
    private final ConcurrentHashMap<Parameter, BotArgumentResolver> resolverCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new factory with the given list of argument resolvers.
     *
     * @param botArgumentResolvers the list of {@link BotArgumentResolver} instances to consult,
     *                             evaluated in order until a supporting resolver is found
     */
    public BotArgumentResolverFactory(List<BotArgumentResolver> botArgumentResolvers) {
        this.botArgumentResolvers = botArgumentResolvers;
    }

    /**
     * Resolves the full argument array for a handler method by mapping each parameter to
     * the value produced by the first matching {@link BotArgumentResolver}.
     *
     * <p>Parameters for which no resolver claims support receive a {@code null} value.</p>
     *
     * @param parameters  the method parameters to resolve, in declaration order
     * @param botRequest  the current bot request carrying the Telegram {@link org.telegram.telegrambots.meta.api.objects.Update}
     *                    and derived context objects
     * @param botResponse the mutable response accumulator for the current request
     * @return an array of resolved argument values, one entry per element in {@code parameters}
     */
    public Object[] resolveArguments(Parameter[] parameters, BotRequest botRequest, BotResponse botResponse) {
        List<Object> objects = new ArrayList<>(parameters.length);
        for (Parameter parameter : parameters) {
            boolean optional = ParameterUtils.isOptional(parameter);

            // Use a sentinel to distinguish "no resolver found" from a resolver returning null.
            // The cache only stores resolvers for parameters that have a match; parameters with
            // no match are handled by the orElse(null) path below on every invocation (rare case).
            BotArgumentResolver matched = resolverCache.computeIfAbsent(parameter, p ->
                    botArgumentResolvers.stream()
                            .filter(r -> r.supportsParameter(p))
                            .findFirst()
                            .orElse(null));

            if (matched == null) {
                if (optional) {
                    objects.add(Optional.empty());
                } else {
                    throw new IllegalStateException(
                            "No BotArgumentResolver found for required parameter '" + parameter.getName()
                            + "' of type '" + parameter.getType().getName()
                            + "'. Register a custom BotArgumentResolver bean that supports this parameter type.");
                }
                continue;
            }

            log.trace("Resolving parameter '{}' type='{}' using resolver '{}'",
                    parameter.getName(), parameter.getType().getSimpleName(),
                    matched.getClass().getSimpleName());

            Object resolved = matched.resolveArgument(parameter, botRequest, botResponse);
            log.trace("Resolved parameter '{}' = {}", parameter.getName(), resolved);
            objects.add(optional ? Optional.ofNullable(resolved) : resolved);
        }
        return objects.toArray();
    }
}
