package uz.osoncode.easygram.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import uz.osoncode.easygram.messaging.consumer.ConsumerType;
import uz.osoncode.easygram.messaging.producer.ProducerType;

/**
 * Top-level configuration properties for broker integration.
 *
 * <p>Bound from the {@code easygram.messaging} prefix. Omit this block entirely for standalone
 * bots that do not connect to any message broker.</p>
 *
 * <p>Example {@code application.yml} snippets:</p>
 * <pre>{@code
 * # Bot that publishes updates to Kafka
 * easygram:
 *   messaging:
 *     type: PRODUCER
 *     forward-only: false
 *     producer:
 *       type: KAFKA
 *     kafka:
 *       topic: my-bot-updates
 *
 * # Bot that consumes updates from RabbitMQ
 * easygram:
 *   messaging:
 *     type: CONSUMER
 *     consumer:
 *       type: RABBIT
 *     rabbit:
 *       exchange: my-exchange
 *       queue: my-bot-updates
 * }</pre>
 *
 * @param type        the role of this bot in the broker integration; required when the
 *                    {@code messaging} block is present
 * @param forwardOnly when {@code true}, updates are published to the broker only — local
 *                    {@code @BotController} handlers are skipped. Only meaningful for
 *                    {@link MessagingType#PRODUCER}. Defaults to {@code false}.
 * @param producer    producer-specific settings (broker type); used when {@code type=PRODUCER}
 * @param consumer    consumer-specific settings (broker type); used when {@code type=CONSUMER}
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see MessagingType
 */
@ConfigurationProperties("easygram.messaging")
public record BotPublishingProperties(

        /** Messaging role: PRODUCER or CONSUMER. */
        MessagingType type,

        /**
         * Whether to stop the filter chain after publishing.
         * {@code false} (default) = publish then continue processing;
         * {@code true} = publish only, skip bot handlers.
         */
        @DefaultValue("false")
        Boolean forwardOnly,

        /** Producer configuration — which broker to publish to. */
        ProducerConfig producer,

        /** Consumer configuration — which broker to consume from. */
        ConsumerConfig consumer
) {

    /**
     * Nested producer configuration.
     *
     * @param type the broker type for publishing; required when {@code messaging.type=PRODUCER}
     */
    public record ProducerConfig(ProducerType type) {
    }

    /**
     * Nested consumer configuration.
     *
     * @param type the broker type for consuming; required when {@code messaging.type=CONSUMER}
     */
    public record ConsumerConfig(ConsumerType type) {
    }
}

