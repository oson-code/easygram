package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that injects the raw Telegram {@link Update} object into a handler method.
 *
 * <p>Providing the full {@link Update} gives a handler method unrestricted access to every
 * field in the Telegram update payload, which is useful for advanced use-cases that are not
 * covered by more specific resolvers.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotUpdateArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link Update}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link Update}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.getType().equals(Update.class);
    }

    /**
     * Retrieves the raw {@link Update} from the current {@link BotRequest} and injects it
     * into the handler method parameter.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request containing the Telegram {@link Update}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link Update} for the current invocation, or {@code null} if not set
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botRequest.getUpdate();
    }
}
