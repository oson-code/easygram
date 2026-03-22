package uz.osoncode.easygram.messaging.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.rabbit.provider.BotRabbitTemplateProvider;

/**
 * {@link BotUpdatePublisher} implementation that publishes Telegram {@link Update} objects
 * to a RabbitMQ exchange as JSON messages.
 *
 * <p>The update is serialized with the provided {@link ObjectMapper} and sent to the exchange
 * configured by {@link RabbitBotPublisherProperties#exchange()} using the routing key from
 * {@link RabbitBotPublisherProperties#routingKey()}. The AMQP message content-type is set to
 * {@code application/json}.</p>
 *
 * <p>Example {@code application.yml}:</p>
 * <pre>{@code
 * telegram:
 *   bot:
 *     messaging:
 *       rabbit:
 *         exchange: telegram-exchange
 *         routing-key: telegram.updates
 * spring:
 *   rabbitmq:
 *     host: localhost
 *     port: 5672
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class RabbitBotUpdatePublisher implements BotUpdatePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitBotPublisherProperties properties;
    private final ObjectMapper objectMapper;

    public RabbitBotUpdatePublisher(BotRabbitTemplateProvider templateProvider,
                                    RabbitBotPublisherProperties properties,
                                    ObjectMapper objectMapper) {
        this.rabbitTemplate = templateProvider.provide();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes the {@link Update} to JSON and publishes it to the configured RabbitMQ exchange.
     *
     * @param update the incoming Telegram update; never {@code null}
     */
    @Override
    @SneakyThrows
    public void publish(Update update) {
        byte[] payload = objectMapper.writeValueAsBytes(update);
        var message = MessageBuilder.withBody(payload)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(String.valueOf(update.getUpdateId()))
                .build();
        rabbitTemplate.send(properties.exchange(), properties.routingKey(), message);
        log.debug("Sent update id={} to RabbitMQ exchange '{}' with routing key '{}'",
                update.getUpdateId(), properties.exchange(), properties.routingKey());
    }
}
