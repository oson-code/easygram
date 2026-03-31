package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.ChatJoinRequest;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the {@link ChatJoinRequest} from the current update.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotChatJoinRequestArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(ChatJoinRequest.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getChatJoinRequest)
                .orElse(null);
    }
}
