package uz.osoncode.easygram.messaging.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.provider.EasygramObjectMapperProvider;

/**
 * Spring Kafka message listener that consumes Telegram {@link Update} JSON payloads
 * from a configured Kafka topic and forwards them into the bot processing pipeline.
 *
 * <p>Registered programmatically with a
 * {@link org.springframework.kafka.listener.ConcurrentMessageListenerContainer} in
 * {@link uz.osoncode.easygram.messaging.kafka.consumer.autoconfigure.KafkaConsumerAutoConfiguration}.
 * Topic, group ID, and consumer factory are resolved at startup from
 * {@link uz.osoncode.easygram.messaging.kafka.EasygramKafkaProperties} and
 * {@link uz.osoncode.easygram.messaging.kafka.provider.EasygramKafkaConsumerFactoryProvider}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaBotUpdateListener implements MessageListener<Object, Object> {

    private final KafkaConsumerBot kafkaConsumerBot;
    private final EasygramObjectMapperProvider objectMapperProvider;

    /**
     * Receives a raw JSON Telegram update from Kafka, deserializes it, and delegates
     * processing to {@link KafkaConsumerBot#handleUpdate(Update)}.
     *
     * <p><strong>Error handling:</strong> any exception during deserialization or processing is
     * logged and then rethrown so that the
     * {@link org.springframework.kafka.listener.ConcurrentMessageListenerContainer} can apply its
     * configured error handler (retry, DLQ routing, etc.) and <em>not</em> commit the offset.
     * If the exception is swallowed here, Spring Kafka commits the offset and the failed
     * message is permanently lost.</p>
     *
     * @param record the Kafka consumer record whose value contains the JSON payload
     * @throws RuntimeException wrapping the original cause so the Kafka container error-handler fires
     */
    @Override
    public void onMessage(ConsumerRecord<Object, Object> record) {
        String message = record.value().toString();
        log.debug("Received Kafka message: {}", message);
        try {
            Update update = objectMapperProvider.provide().readValue(message, Update.class);
            kafkaConsumerBot.handleUpdate(update);
        } catch (Exception e) {
            log.error("Failed to process Kafka message (offset will NOT be committed): topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), e);
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("Failed to process Kafka message", e);
        }
    }
}
