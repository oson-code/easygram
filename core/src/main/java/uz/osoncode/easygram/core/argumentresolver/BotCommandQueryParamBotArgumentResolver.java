package uz.osoncode.easygram.core.argumentresolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.bind.annotation.BotCommandQueryParam;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that extracts and converts a typed query parameter from a bot command message.
 *
 * <p>When a Telegram message has the form {@code /command &lt;param&gt;}, this resolver
 * picks the second whitespace-delimited token and converts it to the declared parameter type
 * using the configured {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper} from
 * {@link BotConfigurer}.  The resolved value is then injected into any handler method
 * parameter annotated with {@link BotCommandQueryParam}.</p>
 *
 * <p>{@code null} is returned when the update contains no message, the message has no text,
 * or the text does not contain a second token.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class BotCommandQueryParamBotArgumentResolver implements BotArgumentResolver {

    /** Provides the {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper} used for type conversion. */
    private final BotConfigurer botConfigurer;

    /**
     * Determines whether this resolver supports the given method parameter.
     *
     * <p>Returns {@code true} when the parameter is annotated with {@link BotCommandQueryParam}.</p>
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter is annotated with {@link BotCommandQueryParam},
     *         {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(BotCommandQueryParam.class);
    }

    /**
     * Extracts the second token from the command message text and converts it to the
     * parameter's declared type.
     *
     * <p>For example, given the text {@code "/search 42"} and a parameter of type {@code int},
     * this method returns {@code 42}.  If the message text has fewer than two tokens, or if
     * any part of the chain (update, message, text) is absent, {@code null} is returned.</p>
     *
     * @param parameter   the method parameter being resolved; its type is the conversion target
     * @param botRequest  the current bot request containing the Telegram {@link Update}
     * @param botResponse the mutable response accumulator for the current request (unused here)
     * @return the converted query-parameter value, or {@code null} if not available
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getMessage)
                .map(Message::getText)
                .map(text -> {
                    String[] parts = text.trim().split("\\s+");
                    if (parts.length < 2) {
                        log.debug("BotCommandQueryParamResolver: no query param found in text '{}' for parameter '{}'",
                                text, parameter.getName());
                        return null;
                    }
                    return botConfigurer.objectMapper().convertValue(parts[1], ParameterUtils.effectiveType(parameter));
                })
                .orElse(null);
    }
}
