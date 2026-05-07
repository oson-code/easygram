package uz.osoncode.easygram.core.bot;

/**
 * Enumerates all supported update-delivery transports for a Telegram bot.
 *
 * <p>Set via {@code easygram.update.transport}. Defaults to {@link #LONG_POLLING} when omitted.
 * This single property fully controls how updates arrive: direct Telegram transports
 * ({@link #LONG_POLLING}, {@link #WEBHOOK}), broker consumer transports
 * ({@link #KAFKA_CONSUMER}, {@link #RABBIT_CONSUMER}), and the explicit no-transport
 * mode ({@link #NONE}).</p>
 *
 * <p>The broker <em>producer</em> side (forwarding processed updates to a broker) is
 * controlled independently via {@code easygram.messaging.producer.type} and is fully
 * orthogonal to this setting.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public enum BotTransportType {

    /**
     * Long-polling transport — the bot repeatedly calls {@code getUpdates} on the Telegram API.
     * This is the default when {@code easygram.update.transport} is not set.
     */
    LONG_POLLING,

    /**
     * Webhook transport — Telegram pushes updates to an HTTPS endpoint exposed by this bot.
     * Requires {@code easygram.update.webhook.url} to be configured.
     */
    WEBHOOK,

    /**
     * Kafka consumer transport — updates are consumed from a Kafka topic instead of receiving
     * them directly from Telegram. A separate producer bot must publish to the same topic.
     * Requires {@code easygram.messaging.kafka.*} broker settings.
     *
     * @since 0.0.7
     */
    KAFKA_CONSUMER,

    /**
     * RabbitMQ consumer transport — updates are consumed from a RabbitMQ queue instead of
     * receiving them directly from Telegram. A separate producer bot must publish to the same
     * exchange. Requires {@code easygram.messaging.rabbit.*} broker settings.
     *
     * @since 0.0.7
     */
    RABBIT_CONSUMER,

    /**
     * No direct transport — neither long-polling, webhook, nor any broker consumer is started
     * automatically. Use this when the application manages its own update ingestion
     * (e.g. an in-memory queue as in {@code linkedlist-bot}).
     *
     * @since 0.0.7
     */
    NONE
}
