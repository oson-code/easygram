package uz.osoncode.easygram.core.provider;

import org.telegram.telegrambots.meta.TelegramUrl;

/**
 * Provider for the Telegram API base URL used by the bot's HTTP client.
 *
 * <p>Register a Spring bean of this type to point the bot at a non-standard endpoint (e.g. a
 * local Bot API server) without touching any transport-specific configuration:</p>
 *
 * <pre>{@code
 * @Bean
 * public EasygramTelegramUrlProvider botTelegramUrlProvider() {
 *     return () -> new TelegramUrl("https://my-local-bot-api.example.com/");
 * }
 * }</pre>
 *
 * <p>When no custom bean is present the framework defaults to {@link TelegramUrl#DEFAULT_URL}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface EasygramTelegramUrlProvider {

    /**
     * Returns the {@link TelegramUrl} to use for all Telegram API requests.
     *
     * @return a non-null {@link TelegramUrl}
     */
    TelegramUrl provide();
}
