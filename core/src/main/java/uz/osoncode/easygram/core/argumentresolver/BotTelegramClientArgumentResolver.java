package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that injects the {@link TelegramClient} into a handler method parameter.
 *
 * <p>The {@link TelegramClient} provides direct access to the Telegram Bot API, enabling
 * the handler to perform arbitrary API calls (e.g. sending messages, editing media) outside
 * the standard response accumulator flow.  The client is retrieved from the current
 * {@link BotRequest}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotTelegramClientArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link TelegramClient}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link TelegramClient}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.getType().equals(TelegramClient.class);
    }

    /**
     * Retrieves the {@link TelegramClient} from the current {@link BotRequest} and injects it
     * into the handler method.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request from which the client is retrieved
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link TelegramClient} stored on the request, or {@code null} if not set
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botRequest.getTelegramClient();
    }
}
