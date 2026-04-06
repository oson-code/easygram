package uz.osoncode.easygram.core.i18n;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Locale;

/**
 * Configuration properties for the Easygram i18n support.
 *
 * <p>Controls locale resolution behaviour when the user's language code cannot
 * be determined from the Telegram {@code User} object.</p>
 *
 * <p>Message bundles are configured via standard Spring Boot properties:</p>
 * <pre>{@code
 * spring:
 *   messages:
 *     basename: messages/bot
 *     encoding: UTF-8
 * }</pre>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * easygram:
 *   i18n:
 *     default-locale: en
 * }</pre>
 *
 * @param defaultLocale fallback locale used when the Telegram user's language code is absent
 *                      or unrecognised; defaults to {@link Locale#ENGLISH} ({@code "en"})
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "easygram.i18n")
public record BotI18nProperties(

        /**
         * Fallback locale used when the Telegram user's language code is absent or unrecognised.
         * Defaults to {@link Locale#ENGLISH}.
         */
        @DefaultValue("en")
        Locale defaultLocale
) {
}
