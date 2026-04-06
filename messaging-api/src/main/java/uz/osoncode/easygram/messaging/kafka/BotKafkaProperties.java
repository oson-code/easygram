package uz.osoncode.easygram.messaging.kafka;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Shared Kafka configuration for both producer and consumer bot integrations.
 *
 * <p>Bound from the {@code easygram.messaging.kafka} prefix. These properties are used by
 * both {@code KafkaMessagingAutoConfiguration} (PRODUCER mode) and
 * {@code KafkaConsumerAutoConfiguration} (CONSUMER mode).</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * easygram:
 *   messaging:
 *     kafka:
 *       topic: my-bot-updates
 *       create-if-absent: true   # auto-create topic if not present (default: true)
 *       partitions: 1
 *       replication-factor: 1
 * spring:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 * }</pre>
 *
 * @param topic             the Kafka topic to publish to or consume from; must not be blank
 * @param createIfAbsent    auto-create the topic if it does not exist (requires broker admin permissions);
 *                          defaults to {@code true}
 * @param partitions        number of partitions for auto-created topic; defaults to {@code 1}
 * @param replicationFactor replication factor for auto-created topic; defaults to {@code 1}
 * @author Islom Mirsaburov
 * @since 0.0.5
 */
@Validated
@ConfigurationProperties("easygram.messaging.kafka")
public record BotKafkaProperties(

        /** The Kafka topic name used for publishing or consuming Telegram updates. */
        @NotBlank(message = "easygram.messaging.kafka.topic must not be blank")
        String topic,

        /** Auto-create the topic when it does not exist. Defaults to {@code true}. */
        @DefaultValue("true")
        boolean createIfAbsent,

        /** Number of partitions for the auto-created topic. Defaults to {@code 1}. */
        @DefaultValue("1")
        int partitions,

        /** Replication factor for the auto-created topic. Defaults to {@code 1}. */
        @DefaultValue("1")
        short replicationFactor
) {
}
