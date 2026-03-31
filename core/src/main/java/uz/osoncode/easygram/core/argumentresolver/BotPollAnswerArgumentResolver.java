package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the {@link PollAnswer} from the current update.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPollAnswerArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(PollAnswer.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getPollAnswer)
                .orElse(null);
    }
}
