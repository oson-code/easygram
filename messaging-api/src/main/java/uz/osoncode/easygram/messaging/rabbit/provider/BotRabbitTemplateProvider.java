package uz.osoncode.easygram.messaging.rabbit.provider;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Provider for the {@link RabbitTemplate} used to publish Telegram updates to RabbitMQ.
 *
 * <p>Register a bean of this type to supply a custom {@link RabbitTemplate},
 * for example to use a specific connection factory or custom message converters.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface BotRabbitTemplateProvider {

    /**
     * Returns the {@link RabbitTemplate} to be used for publishing updates.
     *
     * @return a non-null {@link RabbitTemplate} instance
     */
    RabbitTemplate provide();
}
