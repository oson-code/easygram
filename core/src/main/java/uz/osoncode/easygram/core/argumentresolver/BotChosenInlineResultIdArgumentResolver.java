package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.ChosenInlineQuery;
import uz.osoncode.easygram.core.bind.annotation.BotChosenInlineResultId;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the chosen inline result ID into a
 * {@link BotChosenInlineResultId}-annotated handler method parameter.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotChosenInlineResultIdArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(BotChosenInlineResultId.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getChosenInlineQuery)
                .map(ChosenInlineQuery::getResultId)
                .orElse(null);
    }
}
