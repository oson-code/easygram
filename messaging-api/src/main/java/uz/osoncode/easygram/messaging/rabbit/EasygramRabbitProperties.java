package uz.osoncode.easygram.messaging.rabbit;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Shared RabbitMQ configuration for both producer and consumer bot integrations.
 *
 * <p>Bound from the {@code easygram.messaging.rabbit} prefix. These properties are used by
 * both {@code RabbitMessagingAutoConfiguration} (PRODUCER mode) and
 * {@code RabbitConsumerAutoConfiguration} (CONSUMER mode).</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * easygram:
 *   messaging:
 *     rabbit:
 *       exchange: my-exchange
 *       queue: my-bot-updates
 *       routing-key: easygram.updates
 *       create-if-absent: true   # auto-create exchange, queue and binding (default: true)
 * spring:
 *   rabbitmq:
 *     host: localhost
 *     port: 5672
 * }</pre>
 *
 * @param exchange       the RabbitMQ exchange to publish to or bind the queue to; must not be blank
 * @param queue          the queue name for consuming and auto-creation;
 *                       defaults to {@code easygram-updates}
 * @param routingKey     the routing key used when publishing or binding;
 *                       defaults to {@code easygram.updates}
 * @param createIfAbsent auto-create the exchange, queue and binding if they do not exist;
 *                       defaults to {@code true}
 * @author Islom Mirsaburov
 * @since 0.0.5
 */
@Validated
@ConfigurationProperties("easygram.messaging.rabbit")
public record EasygramRabbitProperties(

        /** The RabbitMQ exchange name. */
        @NotBlank(message = "easygram.messaging.rabbit.exchange must not be blank")
        String exchange,

        /** The queue bound to the exchange. Used for consuming and auto-creation. */
        @DefaultValue("easygram-updates")
        String queue,

        /** The routing key used when publishing messages to the exchange. */
        @DefaultValue("easygram.updates")
        String routingKey,

        /** Auto-create the exchange, queue and binding on startup. Defaults to {@code true}. */
        @DefaultValue("true")
        boolean createIfAbsent
) {
}
