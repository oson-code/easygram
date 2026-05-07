package uz.osoncode.easygram.core.i18n;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.Locale;

/**
 * Locale-aware wrapper around Spring's {@link MessageSource} for Telegram bot handlers.
 *
 * <p>Automatically resolves the locale from the incoming {@link BotRequest} using
 * the configured {@link BotLocaleResolver}, so handler code only needs to pass
 * the request context rather than managing locales manually.</p>
 *
 * <h2>Usage in a handler:</h2>
 * <pre>{@code
 * @BotController
 * public class MyHandler {
 *
 *     @Autowired
 *     private BotMessageSource messages;
 *
 *     @BotCommand("/start")
 *     public String onStart(BotRequest request, User user) {
 *         return messages.getMessage("welcome", request, user.getFirstName());
 *     }
 * }
 * }</pre>
 *
 * <p>Message bundle files should follow the standard Spring convention under
 * {@code src/main/resources/}, e.g.:</p>
 * <ul>
 *   <li>{@code messages/bot.properties}       — default/fallback</li>
 *   <li>{@code messages/bot_en.properties}    — English</li>
 *   <li>{@code messages/bot_uz.properties}    — Uzbek</li>
 *   <li>{@code messages/bot_ru.properties}    — Russian</li>
 * </ul>
 *
 * <p>Configure the basename via standard Spring Boot properties:</p>
 * <pre>
 * spring:
 *   messages:
 *     basename: messages/bot
 *     encoding: UTF-8
 * </pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class BotMessageSource {

    private final MessageSource messageSource;
    private final BotLocaleResolver localeResolver;

    /**
     * Resolves a message for the given code, automatically detecting the locale
     * from the bot request's user.
     *
     * <p>If no message is found for the resolved locale (and
     * {@code spring.messages.use-code-as-default-message} is {@code false}), the missing
     * key is logged at {@code WARN} level and the key itself is returned as a fallback.
     * This prevents a missing translation from crashing the handler with an unchecked
     * {@link NoSuchMessageException} in production.</p>
     *
     * @param code    the message key; must not be {@code null}
     * @param request the current bot request used to determine the locale
     * @param args    optional message arguments for placeholder substitution
     * @return the localised message string, or {@code code} if no message is found
     */
    public String getMessage(String code, BotRequest request, Object... args) {
        Locale locale = localeResolver.resolve(request);
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Missing i18n message key '{}' for locale '{}' — returning key as fallback. " +
                     "Add a translation to your message bundle to suppress this warning.", code, locale);
            return code;
        }
    }

    /**
     * Resolves a message for the given code with an explicit locale.
     *
     * <p>If no message is found, the missing key is logged at {@code WARN} and the key
     * itself is returned as a fallback (same behaviour as
     * {@link #getMessage(String, BotRequest, Object...)}).</p>
     *
     * @param code   the message key; must not be {@code null}
     * @param locale the target locale; must not be {@code null}
     * @param args   optional message arguments for placeholder substitution
     * @return the localised message string, or {@code code} if no message is found
     */
    public String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Missing i18n message key '{}' for locale '{}' — returning key as fallback. " +
                     "Add a translation to your message bundle to suppress this warning.", code, locale);
            return code;
        }
    }

    /**
     * Resolves a message, returning {@code defaultMessage} if no translation is found
     * for the resolved locale (silent fallback — no exception thrown).
     *
     * @param code           the message key; must not be {@code null}
     * @param defaultMessage the string to return when no message is found; may be {@code null}
     * @param request        the current bot request used to determine the locale
     * @param args           optional message arguments for placeholder substitution
     * @return the localised message, or {@code defaultMessage} if none found
     */
    public String getOrDefault(String code, String defaultMessage, BotRequest request, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, localeResolver.resolve(request));
    }

    /**
     * Resolves the locale for the given request using the configured {@link BotLocaleResolver}.
     *
     * @param request the current bot request
     * @return the resolved {@link Locale}
     */
    public Locale resolveLocale(BotRequest request) {
        return localeResolver.resolve(request);
    }
}
