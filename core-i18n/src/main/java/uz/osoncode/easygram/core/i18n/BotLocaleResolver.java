package uz.osoncode.easygram.core.i18n;

import uz.osoncode.easygram.core.model.BotRequest;

import java.util.Locale;

/**
 * Strategy interface for resolving the {@link Locale} from an incoming bot request.
 *
 * <p>The resolved locale is used by {@link BotMessageSource} to look up localised
 * message strings, and by {@link uz.osoncode.easygram.core.i18n.filter.BotLocaleSetterFilter}
 * to populate {@link org.springframework.context.i18n.LocaleContextHolder} for the
 * duration of the request.</p>
 *
 * <p>The default implementation is
 * {@link uz.osoncode.easygram.core.i18n.resolver.UserLanguageCodeLocaleResolver},
 * which reads {@code user.getLanguageCode()} from the Telegram update.
 * Override by declaring a {@code @Bean} of this type.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotLocaleResolver {

    /**
     * Resolves the locale for the given bot request.
     *
     * @param request the current request context; must not be {@code null}
     * @return the resolved {@link Locale}; never {@code null}
     */
    Locale resolve(BotRequest request);
}
