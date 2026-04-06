package uz.osoncode.easygram.messaging.rabbit;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the RabbitMQ {@code BotUpdatePublisher} implementation.
 *
 * <p>Properties are bound from the {@code easygram.messaging.rabbit} prefix in the
 * application configuration.</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * easygram:
 *   messaging:
 *     forward-only: false
 *     rabbit:
 *       exchange: easygram-exchange
 *       routing-key: easygram.updates
 *       queue: easygram-updates
 *       create-if-absent: true   # auto-create exchange, queue and binding (default: true)
 * spring:
 *   rabbitmq:
 *     host: localhost
 *     port: 5672
 * }</pre>
 *
 * @param exchange       the RabbitMQ exchange to which updates are published; must not be blank
 * @param routingKey     the routing key used when publishing to the exchange;
 *                       defaults to {@code easygram.updates}
 * @param queue          the queue bound to the exchange for auto-creation;
 *                       defaults to {@code easygram-updates}
 * @param createIfAbsent auto-create the exchange, queue and binding if they do not exist;
 *                       defaults to {@code true}
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Validated
@ConfigurationProperties("easygram.messaging.rabbit")
public record RabbitBotPublisherProperties(

        /** The RabbitMQ exchange name. */
        @NotBlank(message = "easygram.messaging.rabbit.exchange must not be blank")
        String exchange,

        /** The routing key used when publishing messages to the exchange. */
        @DefaultValue("easygram.updates")
        String routingKey,

        /** The queue bound to the exchange during auto-creation. */
        @DefaultValue("easygram-updates")
        String queue,

        /** Auto-create the exchange, queue and binding on startup. Defaults to {@code true}. */
        @DefaultValue("true")
        boolean createIfAbsent
) {
}
