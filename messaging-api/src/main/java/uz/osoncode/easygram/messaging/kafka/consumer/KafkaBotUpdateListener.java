package uz.osoncode.easygram.messaging.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.provider.BotObjectMapperProvider;

/**
 * Spring Kafka message listener that consumes Telegram {@link Update} JSON payloads
 * from a configured Kafka topic and forwards them into the bot processing pipeline.
 *
 * <p>Registered programmatically with a
 * {@link org.springframework.kafka.listener.ConcurrentMessageListenerContainer} in
 * {@link uz.osoncode.easygram.messaging.kafka.consumer.autoconfigure.KafkaConsumerAutoConfiguration}.
 * Topic, group ID, and consumer factory are resolved at startup from
 * {@link uz.osoncode.easygram.messaging.kafka.EasygramKafkaProperties} and
 * {@link uz.osoncode.easygram.messaging.kafka.provider.BotKafkaConsumerFactoryProvider}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaBotUpdateListener implements MessageListener<Object, Object> {

    private final KafkaConsumerBot kafkaConsumerBot;
    private final BotObjectMapperProvider objectMapperProvider;

    /**
     * Receives a raw JSON Telegram update from Kafka, deserializes it, and delegates
     * processing to {@link KafkaConsumerBot#handleUpdate(Update)}.
     *
     * @param record the Kafka consumer record whose value contains the JSON payload
     */
    @Override
    public void onMessage(ConsumerRecord<Object, Object> record) {
        String message = record.value().toString();
        log.debug("Received Kafka message: {}", message);
        try {
            Update update = objectMapperProvider.provide().readValue(message, Update.class);
            kafkaConsumerBot.handleUpdate(update);
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", message, e);
        }
    }
}
