package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.bind.annotation.BotTextValue;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the full text of the current Telegram message.
 *
 * <p>Parameters annotated with {@link BotTextValue} receive the complete
 * {@link Message#getText() message text} from the incoming update.  This is useful when
 * the handler needs the raw text without any command-prefix splitting.</p>
 *
 * <p>{@code null} is returned when the update contains no message or the message has no text.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotTextArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} when the parameter is annotated with {@link BotTextValue}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter is annotated with {@link BotTextValue},
     *         {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(BotTextValue.class);
    }

    /**
     * Extracts the full message text from the incoming update.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request containing the Telegram {@link Update}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the message text string, or {@code null} if the message or its text is absent
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);
    }
}
