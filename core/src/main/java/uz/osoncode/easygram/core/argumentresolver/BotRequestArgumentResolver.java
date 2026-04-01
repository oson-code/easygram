package uz.osoncode.easygram.core.argumentresolver;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that injects the full {@link BotRequest} context object into a handler method.
 *
 * <p>Injecting {@link BotRequest} gives the handler direct access to the raw
 * {@link org.telegram.telegrambots.meta.api.objects.Update Update}, the resolved
 * {@link org.telegram.telegrambots.meta.api.objects.User User},
 * {@link org.telegram.telegrambots.meta.api.objects.chat.Chat Chat}, the
 * {@link org.telegram.telegrambots.meta.generics.TelegramClient TelegramClient}, and
 * any other contextual information accumulated for the current request.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotRequestArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link BotRequest}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link BotRequest}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(BotRequest.class);
    }

    /**
     * Returns the {@link BotRequest} instance directly, passing the full request context
     * to the handler method.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request to inject
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link BotRequest} for the current invocation, never {@code null}
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botRequest;
    }
}
