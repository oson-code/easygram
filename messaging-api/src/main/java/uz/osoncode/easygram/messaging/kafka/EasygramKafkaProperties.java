package uz.osoncode.easygram.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Shared Kafka configuration for both producer and consumer bot integrations.
 *
 * <p>Bound from the {@code easygram.messaging.kafka} prefix. These properties are used by
 * both {@code KafkaMessagingAutoConfiguration} (PRODUCER mode) and
 * {@code KafkaConsumerAutoConfiguration} (CONSUMER mode).</p>
 *
 * <p>If {@code easygram.messaging.kafka.topic} is not set it defaults to
 * {@code easygram-updates}. All other fields also have defaults, so the minimal
 * configuration is zero additional properties.</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * easygram:
 *   messaging:
 *     kafka:
 *       topic: my-bot-updates
 *       group-id: my-bot-group           # consumer group ID (default: easygram-bot)
 *       create-if-absent: true           # auto-create topic if not present (default: true)
 *       partitions: 1
 *       replication-factor: 1
 * spring:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 * }</pre>
 *
 * @param topic             the Kafka topic to publish to or consume from;
 *                          defaults to {@code easygram-updates}
 * @param groupId           the Kafka consumer group ID; defaults to {@code easygram-bot}
 * @param createIfAbsent    auto-create the topic if it does not exist (requires broker admin permissions);
 *                          defaults to {@code true}
 * @param partitions        number of partitions for auto-created topic; defaults to {@code 1}
 * @param replicationFactor replication factor for auto-created topic; defaults to {@code 1}
 * @author Islom Mirsaburov
 * @since 0.0.5
 */
@ConfigurationProperties("easygram.messaging.kafka")
public record EasygramKafkaProperties(

        /** The Kafka topic name used for publishing or consuming Telegram updates. */
        @DefaultValue("easygram-updates")
        String topic,

        /** The Kafka consumer group ID. Defaults to {@code easygram-bot}. */
        @DefaultValue("easygram-bot")
        String groupId,

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
