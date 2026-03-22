package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.bind.annotation.BotCallbackQueryData;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that handles parameters related to Telegram callback queries.
 *
 * <p>This resolver supports two parameter forms:</p>
 * <ul>
 *   <li>Parameters annotated with {@link BotCallbackQueryData} — the raw callback data
 *       string (e.g. {@code "action:42"}) is extracted from the incoming update and injected.</li>
 *   <li>Parameters typed as {@link CallbackQuery} — the full {@link CallbackQuery} object
 *       is injected directly, giving access to all callback metadata.</li>
 * </ul>
 *
 * <p>If the current update does not contain a callback query, {@code null} is returned
 * for both forms.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotCallbackQueryDataArgumentResolver implements BotArgumentResolver {

    /**
     * Determines whether this resolver can handle the given method parameter.
     *
     * <p>Returns {@code true} when the parameter is either annotated with
     * {@link BotCallbackQueryData} or typed as {@link CallbackQuery}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if this resolver supports the parameter, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.getType().equals(CallbackQuery.class)
                || parameter.isAnnotationPresent(BotCallbackQueryData.class);
    }

    /**
     * Resolves the argument value for a callback-query-related parameter.
     *
     * <p>When the parameter carries {@link BotCallbackQueryData}, this method extracts and
     * returns the callback data string from the update's {@link CallbackQuery}.  Otherwise
     * the complete {@link CallbackQuery} object is returned.  In either case, {@code null} is
     * returned if the update does not contain a callback query.</p>
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request containing the Telegram {@link Update}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the callback data string, the {@link CallbackQuery} object, or {@code null}
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        if (parameter.isAnnotationPresent(BotCallbackQueryData.class)) {
            return Optional.ofNullable(botRequest.getUpdate())
                    .map(Update::getCallbackQuery)
                    .map(CallbackQuery::getData)
                    .orElse(null);
        }
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getCallbackQuery)
                .orElse(null);
    }
}
