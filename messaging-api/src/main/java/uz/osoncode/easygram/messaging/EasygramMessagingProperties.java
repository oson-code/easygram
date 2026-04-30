package uz.osoncode.easygram.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import uz.osoncode.easygram.messaging.producer.ProducerType;

/**
 * Top-level configuration properties for broker integration.
 *
 * <p>Bound from the {@code easygram.messaging} prefix. Omit this block entirely for standalone
 * bots that do not connect to any message broker.</p>
 *
 * <p>The update-source transport (long-polling, webhook, Kafka consumer, RabbitMQ consumer) is
 * controlled independently via {@code easygram.update.transport}. The producer side is activated
 * by setting {@code easygram.messaging.producer.type} — independently of which transport the bot
 * uses to receive updates.</p>
 *
 * <p>Example {@code application.yml} snippets:</p>
 * <pre>{@code
 * # Standalone bot — no broker integration needed
 * easygram:
 *   update:
 *     transport: LONG_POLLING
 *
 * # Bot that receives updates via long-polling and publishes them to Kafka
 * easygram:
 *   update:
 *     transport: LONG_POLLING
 *   messaging:
 *     forward-only: false
 *     producer:
 *       type: KAFKA
 *     kafka:
 *       topic: my-bot-updates
 *
 * # Bot that consumes updates from RabbitMQ
 * easygram:
 *   update:
 *     transport: RABBIT_CONSUMER
 *   messaging:
 *     rabbit:
 *       exchange: my-exchange
 *       queue: my-bot-updates
 * }</pre>
 *
 * @param forwardOnly when {@code true}, updates are published to the broker only — local
 *                    {@code @BotController} handlers are skipped. Only meaningful when a
 *                    producer type is configured. Defaults to {@code false}.
 * @param producer    producer-specific settings (broker type); activates when set
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Validated
@ConfigurationProperties("easygram.messaging")
public record EasygramMessagingProperties(

        /**
         * Whether to stop the filter chain after publishing.
         * {@code false} (default) = publish then continue processing;
         * {@code true} = publish only, skip bot handlers.
         */
        @DefaultValue("false")
        Boolean forwardOnly,

        /** Producer configuration — which broker to publish to. */
        ProducerConfig producer
) {

    /**
     * Nested producer configuration.
     *
     * @param type the broker type for publishing (KAFKA or RABBIT)
     */
    public record ProducerConfig(ProducerType type) {
    }
}

