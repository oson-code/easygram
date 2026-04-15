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
 *     enabled: true
 *     default-locale: en
 * }</pre>
 *
 * @param enabled       whether to activate the i18n auto-configuration (registers locale-aware
 *                      matchers, {@code BotMessageSource}, {@code BotKeyboardFactory}, etc.).
 *                      Defaults to {@code false} — bots that do not use message bundles are
 *                      unaffected even when {@code core-i18n} is on the classpath (e.g. via
 *                      {@code spring-boot-starter}).
 * @param defaultLocale fallback locale used when the Telegram user's language code is absent
 *                      or unrecognised; defaults to {@link Locale#ENGLISH} ({@code "en"})
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "easygram.i18n")
public record EasygramI18nProperties(

        /**
         * Whether to activate Easygram i18n support.
         *
         * <p>When {@code false} (the default), {@code BotI18nAutoConfiguration} is skipped entirely
         * and the plain-text matchers from {@code core} are used for {@code @BotReplyButton} and
         * {@code @BotInlineQuery} routing. Set to {@code true} to register {@link BotMessageSource},
         * {@link uz.osoncode.easygram.core.i18n.keyboard.BotKeyboardFactory}, and locale-aware
         * matchers.</p>
         */
        @DefaultValue("false")
        boolean enabled,

        /**
         * Fallback locale used when the Telegram user's language code is absent or unrecognised.
         * Defaults to {@link Locale#ENGLISH}.
         */
        @DefaultValue("en")
        Locale defaultLocale
) {
}
