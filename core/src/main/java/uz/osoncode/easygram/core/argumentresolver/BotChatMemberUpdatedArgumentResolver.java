package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;

/**
 * Argument resolver that injects the {@link ChatMemberUpdated} from the current update.
 *
 * <p>Returns {@code my_chat_member} if present, falling back to {@code chat_member}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotChatMemberUpdatedArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(ChatMemberUpdated.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        Update update = botRequest.getUpdate();
        if (Objects.isNull(update)) {
            return null;
        }
        ChatMemberUpdated myChatMember = update.getMyChatMember();
        if (Objects.nonNull(myChatMember)) {
            return myChatMember;
        }
        return update.getChatMember();
    }
}
