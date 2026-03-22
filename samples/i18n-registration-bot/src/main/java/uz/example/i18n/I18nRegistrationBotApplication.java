package uz.example.i18n;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the i18n Registration Bot sample.
 *
 * <p>This sample demonstrates:</p>
 * <ul>
 *   <li>Multi-step registration wizard with {@code @BotChatState} state management</li>
 *   <li>Fully localised responses using {@code LocalizedReply} and {@code LocalizedTemplate}</li>
 *   <li>Locale-aware keyboards built with {@code BotKeyboardFactory}</li>
 *   <li>Direct message resolution via {@code BotMessageSource}</li>
 *   <li>Phone-number routing with the new {@code @BotTextPattern} annotation</li>
 *   <li>Three supported languages: English, Uzbek and Russian</li>
 * </ul>
 *
 * <p>The user's locale is resolved automatically from their Telegram
 * {@code languageCode} field (e.g. {@code en}, {@code uz}, {@code ru}) by the built-in
 * {@code UserLanguageCodeLocaleResolver}. No extra configuration is required.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@SpringBootApplication
public class I18nRegistrationBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(I18nRegistrationBotApplication.class, args);
    }
}
