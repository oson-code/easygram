package uz.osoncode.easygram.core.bot;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties that control how Telegram updates are delivered to the bot.
 *
 * <p>Bound from the {@code easygram.update} configuration prefix. When
 * {@code easygram.messaging.type=CONSUMER} is set, the bot receives updates from a
 * message broker instead of polling Telegram directly — in that case this property has no
 * effect because the relevant transport auto-configuration does not activate.</p>
 *
 * <p>Example {@code application.yml} snippets:</p>
 * <pre>{@code
 * # Long-polling (default — omit this block entirely)
 * easygram:
 *   update:
 *     transport: LONG_POLLING
 *
 * # Webhook
 * easygram:
 *   update:
 *     transport: WEBHOOK
 *     webhook:
 *       url: "https://example.com/bot"
 * }</pre>
 *
 * @param transport the update-delivery mechanism; defaults to {@link BotTransportType#LONG_POLLING}
 * @author Islom Mirsaburov
 * @since 0.0.5
 * @see BotTransportType
 */
@Validated
@ConfigurationProperties("easygram.update")
public record BotUpdateProperties(

        @NotNull(message = "easygram.update.transport must not be null")
        @DefaultValue("LONG_POLLING")
        BotTransportType transport
) {
}
