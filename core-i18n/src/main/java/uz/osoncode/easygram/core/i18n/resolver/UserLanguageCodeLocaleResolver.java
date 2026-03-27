package uz.osoncode.easygram.core.i18n.resolver;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.i18n.BotLocaleResolver;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.util.Strings;

import java.util.Locale;
import java.util.Objects;

/**
 * Default {@link BotLocaleResolver} that determines the locale from the Telegram
 * user's declared language code ({@code user.getLanguageCode()}).
 *
 * <p>Telegram reports the user's client language as an IETF BCP-47 tag
 * (e.g. {@code "en"}, {@code "uz"}, {@code "ru"}, {@code "zh-hans"}).
 * This resolver converts that tag to a {@link Locale} via
 * {@link Locale#forLanguageTag(String)}.</p>
 *
 * <p>Falls back to the configured {@code defaultLocale} when:</p>
 * <ul>
 *   <li>the update has no associated user (e.g. channel posts)</li>
 *   <li>{@code user.getLanguageCode()} is {@code null} or blank</li>
 *   <li>the language tag cannot be parsed</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class UserLanguageCodeLocaleResolver implements BotLocaleResolver {

    private final Locale defaultLocale;

    /**
     * Resolves the locale from {@code request.getUser().getLanguageCode()}.
     *
     * @param request the current bot request; must not be {@code null}
     * @return the user's locale, or {@code defaultLocale} if it cannot be determined
     */
    @Override
    public Locale resolve(BotRequest request) {
        User user = request.getUser();
        if (Objects.nonNull(user)) {
            String langCode = user.getLanguageCode();
            if (Strings.isNotBlank(langCode)) {
                try {
                    Locale locale = Locale.forLanguageTag(langCode);
                    // forLanguageTag returns Locale.ROOT for unknown tags — treat as missing
                    if (!locale.getLanguage().isBlank()) {
                        return locale;
                    }
                } catch (Exception ignored) {
                    // fall through to default
                }
            }
        }
        return defaultLocale;
    }
}
