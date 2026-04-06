package uz.osoncode.easygram.messaging.kafka.consumer;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Kafka consumer bot transport.
 *
 * <p>Properties are bound from the {@code easygram.kafka-consumer} prefix.</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * easygram:
 *   kafka-consumer:
 *     topic: easygram-updates
 *     create-if-absent: true   # auto-create topic if not present (default: true)
 *     partitions: 1
 *     replication-factor: 1
 * spring:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 *     consumer:
 *       group-id: my-bot-group
 * }</pre>
 *
 * @param topic             the Kafka topic to consume from
 * @param createIfAbsent    auto-create the topic if it does not exist; defaults to {@code true}
 * @param partitions        number of partitions for auto-created topic; defaults to {@code 1}
 * @param replicationFactor replication factor for auto-created topic; defaults to {@code 1}
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Validated
@ConfigurationProperties("easygram.kafka-consumer")
public record KafkaConsumerBotProperties(

        @NotBlank(message = "easygram.kafka-consumer.topic must not be blank")
        String topic,

        @DefaultValue("true")
        boolean createIfAbsent,

        @DefaultValue("1")
        int partitions,

        @DefaultValue("1")
        short replicationFactor
) {
}
