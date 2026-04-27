package uz.osoncode.easygram.core.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional configuration properties for the Telegram Bot API base URL.
 *
 * <p>Bound from the {@code easygram.telegram-url} prefix. All fields are optional; when none
 * are set the framework defaults to {@link org.telegram.telegrambots.meta.TelegramUrl#DEFAULT_URL}.
 * Set at least {@code host} to redirect the bot to a local or self-hosted Bot API server.</p>
 *
 * <p>Example {@code application.yml}:</p>
 * <pre>{@code
 * easygram:
 *   token: ${BOT_TOKEN}
 *   telegram-url:
 *     host: my-local-bot-api.example.com
 *     port: 8443
 *     schema: https
 *     test-server: false
 * }</pre>
 *
 * <p>A user-defined {@code @Bean} of type
 * {@link uz.osoncode.easygram.core.provider.BotTelegramUrlProvider} always takes precedence
 * over these properties.</p>
 *
 * @param schema     URL schema, e.g. {@code https}; {@code null} keeps the default schema
 * @param host       hostname of the Bot API server; when {@code null} the default URL is used
 * @param port       port number; {@code null} keeps the default port
 * @param testServer whether to target the Telegram test environment; {@code null} defaults to {@code false}
 * @author Islom Mirsaburov
 * @since 0.0.6
 * @see uz.osoncode.easygram.core.provider.BotTelegramUrlProvider
 */
@ConfigurationProperties("easygram.telegram-url")
public record EasygramTelegramUrlProperties(
        String schema,
        String host,
        Integer port,
        Boolean testServer
) {
}
