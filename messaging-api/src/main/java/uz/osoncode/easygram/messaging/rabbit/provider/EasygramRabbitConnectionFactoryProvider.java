package uz.osoncode.easygram.messaging.rabbit.provider;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;

/**
 * SPI for supplying the RabbitMQ {@link ConnectionFactory} used to create both the
 * {@link org.springframework.amqp.rabbit.core.RabbitTemplate} (producer side) and the
 * {@link org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory}
 * (consumer side).
 *
 * <p>Register a bean of this type to provide a custom {@link ConnectionFactory},
 * for example to configure a separate vhost, TLS, or a different RabbitMQ cluster.</p>
 *
 * <p>The default implementation wraps Spring Boot's auto-configured
 * {@link ConnectionFactory} bean.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
@FunctionalInterface
public interface EasygramRabbitConnectionFactoryProvider {

    /**
     * Returns the {@link ConnectionFactory} to be used for both publishing and consuming.
     *
     * @return a non-null {@link ConnectionFactory} instance
     */
    ConnectionFactory provide();
}
