package uz.osoncode.easygram.messaging.kafka.provider;

import org.springframework.kafka.core.KafkaTemplate;

/**
 * Provider for the {@link KafkaTemplate} used to publish Telegram updates to Kafka.
 *
 * <p>Register a bean of this type to supply a custom {@link KafkaTemplate},
 * for example to use a specific producer factory or custom serializers.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface EasygramKafkaTemplateProvider {

    /**
     * Returns the {@link KafkaTemplate} to be used for publishing updates.
     *
     * @return a non-null {@link KafkaTemplate} instance
     */
    KafkaTemplate<String, String> provide();
}
