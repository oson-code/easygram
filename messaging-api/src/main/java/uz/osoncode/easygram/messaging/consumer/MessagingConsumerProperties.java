package uz.osoncode.easygram.messaging.consumer;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the {@code messaging-consumer} module.
 *
 * <p>Bind via the {@code easygram.messaging.consumer} prefix:</p>
 * <pre>{@code
 * easygram:
 *   messaging:
 *     consumer:
 *       consumer-type: kafka   # or rabbit
 * }</pre>
 *
 * @param consumerType the broker to activate; must not be null
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Validated
@ConfigurationProperties("easygram.messaging.consumer")
public record MessagingConsumerProperties(

        @NotNull(message = "easygram.messaging.consumer.consumer-type must not be null")
        ConsumerType consumerType
) {
}
