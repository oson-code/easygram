package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import uz.osoncode.easygram.core.bind.annotation.BotInlineQueryValue;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the inline query text into a {@link BotInlineQueryValue}-annotated
 * handler method parameter.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotInlineQueryValueArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(BotInlineQueryValue.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getInlineQuery)
                .map(InlineQuery::getQuery)
                .orElse(null);
    }
}
