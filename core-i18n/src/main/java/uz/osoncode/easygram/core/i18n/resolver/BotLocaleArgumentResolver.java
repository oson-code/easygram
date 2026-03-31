package uz.osoncode.easygram.core.i18n.resolver;

import lombok.RequiredArgsConstructor;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.ParameterUtils;
import uz.osoncode.easygram.core.i18n.BotLocaleResolver;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Locale;

/**
 * {@link BotArgumentResolver} that injects the resolved {@link Locale} into
 * handler method parameters typed as {@link Locale}.
 *
 * <p>Registered automatically by {@link uz.osoncode.easygram.core.i18n.autoconfigure.BotI18nAutoConfiguration}.
 * To use, simply declare a {@link Locale} parameter in any {@code @BotController} method:</p>
 *
 * <pre>{@code
 * @BotCommand("/start")
 * public String onStart(User user, Locale locale) {
 *     // locale is auto-populated from user.getLanguageCode()
 *     return switch (locale.getLanguage()) {
 *         case "uz" -> "Assalomu alaykum!";
 *         case "ru" -> "Привет!";
 *         default  -> "Hello!";
 *     };
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotLocaleArgumentResolver implements BotArgumentResolver {

    private final BotLocaleResolver localeResolver;

    /**
     * Supports any parameter whose type is {@link Locale} or a subclass thereof.
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the parameter type is assignable from {@link Locale}
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return Locale.class.isAssignableFrom(ParameterUtils.effectiveType(parameter));
    }

    /**
     * Resolves the locale from the current bot request using the configured
     * {@link BotLocaleResolver}.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request
     * @param botResponse the mutable response accumulator (unused)
     * @return the resolved {@link Locale}; never {@code null}
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return localeResolver.resolve(botRequest);
    }
}
