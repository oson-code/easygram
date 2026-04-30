package uz.osoncode.easygram.messaging.rabbit.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.provider.EasygramObjectMapperProvider;

/**
 * Spring AMQP message listener that consumes Telegram {@link Update} JSON payloads from
 * a configured RabbitMQ queue and forwards them into the bot processing pipeline.
 *
 * <p>Registered programmatically with a
 * {@link org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer} in
 * {@link uz.osoncode.easygram.messaging.rabbit.consumer.autoconfigure.RabbitConsumerAutoConfiguration}.
 * Queue and connection factory are resolved at startup from
 * {@link uz.osoncode.easygram.messaging.rabbit.EasygramRabbitProperties} and
 * {@link uz.osoncode.easygram.messaging.rabbit.provider.EasygramRabbitConnectionFactoryProvider}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitBotUpdateListener implements MessageListener {

    private final RabbitConsumerBot rabbitConsumerBot;
    private final EasygramObjectMapperProvider objectMapperProvider;

    /**
     * Receives a raw AMQP message from the configured RabbitMQ queue, deserializes the body
     * as a Telegram {@link Update} JSON payload, and delegates processing to
     * {@link RabbitConsumerBot#handleUpdate(Update)}.
     *
     * @param message the raw AMQP message from RabbitMQ
     */
    @Override
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
