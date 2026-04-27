package uz.osoncode.easygram.messaging.kafka.provider;

import org.springframework.kafka.core.ProducerFactory;

/**
 * SPI for supplying the {@link ProducerFactory} used to create the
 * {@link org.springframework.kafka.core.KafkaTemplate} that publishes Telegram updates to Kafka.
 *
 * <p>Register a bean of this type to provide a custom {@link ProducerFactory},
 * for example to use specific serializers, SSL configuration, or a separate Kafka cluster.</p>
 *
 * <p>The default implementation wraps Spring Boot's auto-configured
 * {@code ProducerFactory<String, String>} bean.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
@FunctionalInterface
public interface BotKafkaProducerFactoryProvider {

    /**
     * Returns the {@link ProducerFactory} to be used when building the Kafka template.
     *
     * @return a non-null {@link ProducerFactory} instance
     */
    ProducerFactory<String, String> provide();
}
