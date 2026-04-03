package uz.osoncode.easygram.messaging.producer;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the {@code messaging-producer} module.
 *
 * <p>Properties are bound from the {@code telegram.bot.messaging.producer} prefix in the
 * application configuration.</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * telegram:
 *   bot:
 *     messaging:
 *       producer:
 *         producer-type: rabbit   # or: kafka
 * }</pre>
 *
 * @param producerType the messaging backend to use for publishing Telegram updates;
 *                     must be one of {@link ProducerType#KAFKA} or {@link ProducerType#RABBIT}
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Validated
@ConfigurationProperties("telegram.bot.messaging.producer")
public record MessagingProducerProperties(

        /**
         * The producer backend type. Determines which {@code BotUpdatePublisher}
         * implementation is registered.
         */
        @NotNull(message = "telegram.bot.messaging.producer.producer-type must not be null")
        ProducerType producerType
) {
}
