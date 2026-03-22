package uz.osoncode.easygram.core.argumentresolver;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

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
public class BotArgumentResolverFactory {

    /** Ordered list of argument resolvers consulted during parameter resolution. */
    private final List<BotArgumentResolver> botArgumentResolvers;

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
            Object o = botArgumentResolvers
                    .stream()
                    .filter(argumentResolver -> argumentResolver.supportsParameter(parameter))
                    .findFirst()
                    .map(argumentResolver -> argumentResolver.resolveArgument(parameter, botRequest, botResponse))
                    .orElse(null);
            objects.add(o);
        }
        return objects.toArray();
    }
}
