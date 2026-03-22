package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.bind.annotation.BotCommandValue;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the command token from the current message text.
 *
 * <p>When a Telegram message contains a command such as {@code /start param1}, this resolver
 * extracts the first whitespace-delimited token (i.e. {@code /start}) and injects it into any
 * handler method parameter annotated with {@link BotCommandValue}.</p>
 *
 * <p>{@code null} is returned when the update contains no message or the message has no text.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotCommandArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} when the parameter is annotated with {@link BotCommandValue}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter is annotated with {@link BotCommandValue},
     *         {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(BotCommandValue.class);
    }

    /**
     * Extracts the command token (the first word of the message text) from the incoming update.
     *
     * <p>For example, given the message text {@code "/help me"}, this method returns
     * {@code "/help"}.  If the update, message, or message text is absent, {@code null}
     * is returned.</p>
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request containing the Telegram {@link Update}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the command token string, or {@code null} if no text is available
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getMessage)
                .map(Message::getText)
                .map(text -> text.split(" ")[0])
                .orElse(null);
    }
}
