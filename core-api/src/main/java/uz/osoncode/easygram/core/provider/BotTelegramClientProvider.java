package uz.osoncode.easygram.core.provider;

import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Provider for the {@link TelegramClient} that sends API replies back to Telegram.
 *
 * <p>The method receives the bot token so that implementations can build a token-specific client.
 * This design also future-proofs multi-bot setups where different bots may require different
 * client configurations.</p>
 *
 * <p>Register a Spring bean of this type to supply a fully custom {@link TelegramClient}
 * (e.g. one backed by a different HTTP library or a test stub):</p>
 *
 * <pre>{@code
 * @Bean
 * public BotTelegramClientProvider botTelegramClientProvider() {
 *     return botToken -> new OkHttpTelegramClient(
 *             customObjectMapper(),
 *             customHttpClient(),
 *             botToken,
 *             TelegramUrl.DEFAULT_URL);
 * }
 * }</pre>
 *
 * <p>When no custom bean is present the framework registers a default implementation that builds
 * an {@link org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient} using the other
 * provider beans ({@link BotObjectMapperProvider}, {@link BotOkHttpClientProvider},
 * {@link BotTelegramUrlProvider}).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface BotTelegramClientProvider {

    /**
     * Creates (or returns) a {@link TelegramClient} for the given bot token.
     *
     * @param botToken the bot's API token; never {@code null}
     * @return a non-null, ready-to-use {@link TelegramClient}
     */
    TelegramClient provide(String botToken);
}
