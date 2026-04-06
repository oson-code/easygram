package uz.osoncode.easygram.messaging.consumer;

/**
 * Identifies the message-broker backend used when this bot consumes updates.
 *
 * <p>Set via {@code easygram.messaging.consumer.type}. Only evaluated when
 * {@code easygram.messaging.type=CONSUMER}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.5
 */
public enum ConsumerType {

    /** Consume Telegram updates from Apache Kafka. */
    KAFKA,

    /** Consume Telegram updates from RabbitMQ. */
    RABBIT
}
