package uz.osoncode.easygram.messaging.rabbit.consumer;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the RabbitMQ consumer bot transport.
 *
 * <p>Properties are bound from the {@code easygram.rabbit-consumer} prefix.</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * easygram:
 *   rabbit-consumer:
 *     queue: easygram-updates
 *     exchange: easygram-exchange
 *     routing-key: easygram.updates
 *     create-if-absent: true   # auto-create exchange, queue and binding (default: true)
 * spring:
 *   rabbitmq:
 *     host: localhost
 *     port: 5672
 * }</pre>
 *
 * @param queue          the RabbitMQ queue to consume from
 * @param exchange       the exchange to bind the queue to during auto-creation;
 *                       defaults to {@code easygram-exchange}
 * @param routingKey     the routing key for the binding; defaults to {@code easygram.updates}
 * @param createIfAbsent auto-create the exchange, queue and binding if absent; defaults to {@code true}
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Validated
@ConfigurationProperties("easygram.rabbit-consumer")
public record RabbitConsumerBotProperties(

        @NotBlank(message = "easygram.rabbit-consumer.queue must not be blank")
        String queue,

        @DefaultValue("easygram-exchange")
        String exchange,

        @DefaultValue("easygram.updates")
        String routingKey,

        @DefaultValue("true")
        boolean createIfAbsent
) {
}
