package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that injects the Telegram {@link Chat} associated with the current update.
 *
 * <p>The {@link Chat} object is pre-resolved by the framework and stored on the
 * {@link BotRequest}.  This resolver retrieves it and injects it directly into any handler
 * method parameter typed as {@link Chat}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotChatArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link Chat}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link Chat}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(Chat.class);
    }

    /**
     * Resolves the {@link Chat} instance from the current {@link BotRequest}.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request from which the chat is retrieved
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link Chat} associated with the incoming update, or {@code null} if not set
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botRequest.getChat();
    }
}
