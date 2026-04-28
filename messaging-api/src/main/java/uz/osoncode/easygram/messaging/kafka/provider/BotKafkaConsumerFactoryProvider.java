package uz.osoncode.easygram.messaging.kafka.provider;

import org.springframework.kafka.core.ConsumerFactory;

/**
 * SPI for supplying the {@link ConsumerFactory} used to create the Kafka listener
 * container factory that feeds updates into the bot.
 *
 * <p>Register a bean of this type to provide a custom {@link ConsumerFactory},
 * for example to configure specific deserializers, SSL, or a separate Kafka cluster.</p>
 *
 * <p>The default implementation wraps Spring Boot's auto-configured
 * {@code ConsumerFactory<Object, Object>} bean.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
@FunctionalInterface
public interface BotKafkaConsumerFactoryProvider {

    /**
     * Returns the {@link ConsumerFactory} to be used when building the listener container factory.
     *
     * @return a non-null {@link ConsumerFactory} instance
     */
    ConsumerFactory<Object, Object> provide();
}
