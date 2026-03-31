package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that injects the Telegram {@link User} associated with the current update.
 *
 * <p>The {@link User} object is pre-resolved by the framework and stored on the
 * {@link BotRequest}.  This resolver retrieves it and injects it into any handler method
 * parameter typed as {@link User}, providing convenient access to the sender's profile
 * information such as first name, username, and user ID.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotUserArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} only when the parameter type is exactly {@link User}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is {@link User}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(User.class);
    }

    /**
     * Resolves the {@link User} instance from the current {@link BotRequest}.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request from which the user is retrieved
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the {@link User} associated with the incoming update, or {@code null} if not set
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botRequest.getUser();
    }
}
