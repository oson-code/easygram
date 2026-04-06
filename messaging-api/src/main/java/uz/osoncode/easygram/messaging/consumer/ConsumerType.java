package uz.osoncode.easygram.messaging.consumer;

/**
 * Enumerates the supported broker types for the {@code messaging-consumer} module.
 *
 * <p>Set via {@code easygram.messaging.consumer.consumer-type} to select which
 * broker consumer is activated:</p>
 * <ul>
 *   <li>{@link #KAFKA} — activates the Kafka consumer transport.</li>
 *   <li>{@link #RABBIT} — activates the RabbitMQ consumer transport.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public enum ConsumerType {

    /** Activate the Kafka consumer transport. */
    KAFKA,

    /** Activate the RabbitMQ consumer transport. */
    RABBIT
}
