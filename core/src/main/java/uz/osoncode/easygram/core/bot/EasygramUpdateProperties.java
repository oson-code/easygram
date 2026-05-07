package uz.osoncode.easygram.core.bot;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties that control how Telegram updates are delivered to the bot.
 *
 * <p>Bound from the {@code easygram.update} configuration prefix. Set
 * {@code easygram.update.transport} to select how updates arrive. The broker producer
 * side (forwarding updates to a broker) is controlled independently via
 * {@code easygram.messaging.producer.type} and does not affect this setting.</p>
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
 *
 * # Consume updates from Kafka (produced by a separate bot instance)
 * easygram:
 *   update:
 *     transport: KAFKA_CONSUMER
 *
 * # No direct transport (custom ingestion)
 * easygram:
 *   update:
 *     transport: NONE
 * }</pre>
 *
 * @param transport the update-delivery mechanism; defaults to {@link BotTransportType#LONG_POLLING}
 * @author Islom Mirsaburov
 * @since 0.0.5
 * @see BotTransportType
 */
@Validated
@ConfigurationProperties("easygram.update")
public record EasygramUpdateProperties(

        @NotNull(message = "easygram.update.transport must not be null")
        @DefaultValue("LONG_POLLING")
        BotTransportType transport
) {
}
