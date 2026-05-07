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
 * <p><strong>Error handling policy:</strong> any exception during deserialization or processing
 * is logged (including the full stack trace) and then <em>silently dropped</em> — the message
 * is always ACK'd. Re-throwing the exception would cause the AMQP container to NACK and requeue
 * the message, which for permanent failures (e.g. a Telegram 4xx rejection) creates an infinite
 * requeue loop. If you need dead-letter routing for failed messages, configure a Dead-Letter
 * Exchange (DLX) on the broker side.</p>
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
     * <p><strong>Error handling:</strong> any exception during deserialization or processing is
     * logged with the full stack trace, and then <strong>silently dropped</strong> — the message
     * is always ACK'd. Re-throwing would cause the AMQP container to NACK and requeue the
     * same message, which for permanent failures (e.g. a Telegram 4xx rejection or a
     * malformed JSON payload) creates an infinite requeue loop. To route failed messages to
     * a dead-letter queue, configure a Dead-Letter Exchange (DLX) on the broker.</p>
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
            log.error("[messageId={}] Failed to process RabbitMQ message — ACKing to prevent requeue loop; check the error above",
                    message.getMessageProperties().getMessageId(), e);
            // Do NOT rethrow — NACKing would requeue the same broken message indefinitely.
            // Use a Dead-Letter Exchange on the broker if you need failed-message routing.
        }
    }
}
