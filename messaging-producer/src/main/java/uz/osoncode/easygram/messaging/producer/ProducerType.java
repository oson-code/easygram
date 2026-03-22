package uz.osoncode.easygram.messaging.producer;

/**
 * Supported messaging-producer backend types.
 *
 * <p>Used as the value of {@code telegram.bot.messaging.producer.producer-type} to select
 * which {@code BotUpdatePublisher} implementation is registered by
 * {@code MessagingProducerAutoConfiguration}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public enum ProducerType {

    /** Apache Kafka publisher. */
    KAFKA,

    /** RabbitMQ publisher. */
    RABBIT
}
