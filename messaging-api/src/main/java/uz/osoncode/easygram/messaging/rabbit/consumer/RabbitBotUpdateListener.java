package uz.osoncode.easygram.messaging.rabbit.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.provider.BotObjectMapperProvider;

/**
 * Spring AMQP message listener that consumes Telegram {@link Update} JSON payloads from
 * a configured RabbitMQ queue and forwards them into the bot processing pipeline.
 *
 * <p>The queue is resolved from the {@code telegram.bot.rabbit-consumer.queue} property at
 * startup. Each received AMQP {@link Message} body is deserialized into an {@link Update}
 * using {@link BotObjectMapperProvider} and forwarded to
 * {@link RabbitConsumerBot#handleUpdate(Update)}.</p>
 *
 * <p>RabbitMQ connection settings are configured via the standard
 * {@code spring.rabbitmq.*} properties. The listener container factory is provided
 * automatically by Spring Boot's AMQP auto-configuration.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitBotUpdateListener {

    private final RabbitConsumerBot rabbitConsumerBot;
    private final BotObjectMapperProvider objectMapperProvider;

    /**
     * Receives a raw AMQP message from the configured RabbitMQ queue, deserializes the body
     * as a Telegram {@link Update} JSON payload, and delegates processing to
     * {@link RabbitConsumerBot#handleUpdate(Update)}.
     *
     * @param message the raw AMQP message from RabbitMQ
     */
    @RabbitListener(queues = "${telegram.bot.rabbit-consumer.queue}", containerFactory = "botRabbitListenerContainerFactory")
    public void onMessage(Message message) {
        log.debug("Received RabbitMQ message: messageId={}", message.getMessageProperties().getMessageId());
        try {
            Update update = objectMapperProvider.provide().readValue(message.getBody(), Update.class);
            rabbitConsumerBot.handleUpdate(update);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ message: messageId={}", message.getMessageProperties().getMessageId(), e);
        }
    }
}
