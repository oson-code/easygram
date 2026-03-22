package uz.osoncode.easygram.messaging.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.provider.BotObjectMapperProvider;

/**
 * Spring Kafka message listener that consumes Telegram {@link Update} JSON payloads
 * from a configured Kafka topic and forwards them into the bot processing pipeline.
 *
 * <p>The topic is resolved from the {@code telegram.bot.kafka-consumer.topic} property at
 * startup. Each received message is deserialized into an {@link Update} using
 * {@link BotObjectMapperProvider} and forwarded to
 * {@link KafkaConsumerBot#handleUpdate(Update)}.</p>
 *
 * <p>Kafka consumer settings (bootstrap servers, group ID, deserializers, etc.) are
 * configured via the standard {@code spring.kafka.consumer.*} properties.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaBotUpdateListener {

    private final KafkaConsumerBot kafkaConsumerBot;
    private final BotObjectMapperProvider objectMapperProvider;

    /**
     * Receives a raw JSON Telegram update from Kafka, deserializes it, and delegates
     * processing to {@link KafkaConsumerBot#handleUpdate(Update)}.
     *
     * @param message the raw JSON string payload from Kafka
     */
    @KafkaListener(topics = "${telegram.bot.kafka-consumer.topic}", containerFactory = "botKafkaListenerContainerFactory")
    @SneakyThrows
    public void onMessage(String message) {
        log.debug("Received Kafka message: {}", message);
        Update update = objectMapperProvider.provide().readValue(message, Update.class);
        kafkaConsumerBot.handleUpdate(update);
    }
}
