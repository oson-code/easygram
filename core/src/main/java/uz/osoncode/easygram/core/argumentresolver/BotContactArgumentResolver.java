package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the {@link Contact} shared in the current Telegram message.
 *
 * <p>When a user shares their phone contact (or another contact), Telegram attaches a
 * {@link Contact} object to the message.  This resolver extracts that object and injects it
 * into any handler method parameter typed as {@link Contact}.</p>
 *
 * <p>{@code null} is returned when the update contains no message or the message carries
 * no contact.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotContactArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link Contact}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link Contact}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.getType().equals(Contact.class);
    }

    /**
     * Extracts the {@link Contact} from the current message in the incoming update.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request containing the Telegram {@link Update}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link Contact} from the message, or {@code null} if no contact is present
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getMessage)
                .map(Message::getContact)
                .orElse(null);
    }
}
