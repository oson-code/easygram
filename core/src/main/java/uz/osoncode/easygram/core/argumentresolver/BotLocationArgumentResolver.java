package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.location.Location;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the {@link Location} attached to the current Telegram message.
 *
 * <p>When a user shares their live or static location, Telegram attaches a {@link Location}
 * object to the message.  This resolver extracts that object and injects it into any handler
 * method parameter typed as {@link Location}.</p>
 *
 * <p>{@code null} is returned when the update contains no message or the message carries
 * no location.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotLocationArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link Location}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link Location}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(Location.class);
    }

    /**
     * Extracts the {@link Location} from the current message in the incoming update.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request containing the Telegram {@link Update}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link Location} from the message, or {@code null} if no location is present
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getMessage)
                .map(Message::getLocation)
                .orElse(null);
    }
}
