package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the Telegram {@link Message} from the current update
 * into a handler method parameter.
 *
 * <p>Resolves the {@link Message} contained in the incoming {@link Update} for
 * handler methods that need to inspect message-level fields without the full update wrapper.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.4
 */
public class BotMessageArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link Message}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link Message}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(Message.class);
    }

    /**
     * Retrieves the {@link Message} from the current {@link BotRequest} and injects it
     * into the handler method parameter.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request containing the Telegram {@link Update}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link Message} for the current invocation, or {@code null} if not present
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest)
                .map(BotRequest::getUpdate)
                .map(Update::getMessage)
                .orElse(null);
    }
}
