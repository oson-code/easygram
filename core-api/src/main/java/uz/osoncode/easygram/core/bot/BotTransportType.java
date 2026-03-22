package uz.osoncode.easygram.core.bot;

/**
 * Enumerates the supported update-delivery transports for a Telegram bot.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public enum BotTransportType {
    /** Long-polling transport — the bot repeatedly calls getUpdates. */
    LONG_POLLING,
    /** Webhook transport — Telegram pushes updates to an HTTPS endpoint. */
    WEBHOOK,
    /** Kafka consumer transport — updates arrive via a Kafka topic. */
    KAFKA_CONSUMER,
    /** RabbitMQ consumer transport — updates arrive via a RabbitMQ queue. */
    RABBIT_CONSUMER
}
