package uz.osoncode.easygram.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.kafka.provider.BotKafkaTemplateProvider;

/**
 * {@link BotUpdatePublisher} implementation that publishes Telegram {@link Update} objects
 * to an Apache Kafka topic as JSON strings.
 *
 * <p>The update is serialized with the provided {@link ObjectMapper} and sent to the topic
 * configured by {@link BotKafkaProperties#topic()} using a
 * {@link KafkaTemplate KafkaTemplate&lt;String, String&gt;}. The Kafka record key is the
 * {@code updateId} converted to a string.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class KafkaBotUpdatePublisher implements BotUpdatePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BotKafkaProperties properties;
    private final ObjectMapper objectMapper;

    public KafkaBotUpdatePublisher(BotKafkaTemplateProvider templateProvider,
                                   BotKafkaProperties properties,
                                   ObjectMapper objectMapper) {
        this.kafkaTemplate = templateProvider.provide();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes the {@link Update} to JSON and sends it to the configured Kafka topic.
     *
     * <p>The Kafka record key is set to {@code String.valueOf(update.getUpdateId())} so that
     * updates from the same chat can be routed to the same partition when a custom partitioner
     * is configured.</p>
     *
     * @param update the incoming Telegram update; never {@code null}
     */
    @Override
    @SneakyThrows
    public void publish(Update update) {
        String payload = objectMapper.writeValueAsString(update);
        kafkaTemplate.send(properties.topic(), String.valueOf(update.getUpdateId()), payload);
        log.debug("Sent update id={} to Kafka topic '{}'", update.getUpdateId(), properties.topic());
    }
}
