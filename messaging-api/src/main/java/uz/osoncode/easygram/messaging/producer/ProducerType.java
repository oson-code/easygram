package uz.osoncode.easygram.messaging.producer;

/**
 * Identifies the message-broker backend used when this bot publishes updates.
 *
 * <p>Set via {@code easygram.messaging.producer.type}. Only evaluated when
 * {@code easygram.messaging.type=PRODUCER}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.5
 */
public enum ProducerType {

    /** Publish Telegram updates to Apache Kafka. */
    KAFKA,

    /** Publish Telegram updates to RabbitMQ. */
    RABBIT
}
