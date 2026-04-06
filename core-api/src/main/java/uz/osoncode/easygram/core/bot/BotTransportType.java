package uz.osoncode.easygram.core.bot;

/**
 * Enumerates the supported direct update-delivery transports for a Telegram bot.
 *
 * <p>These values represent how the bot <em>directly</em> receives updates from Telegram.
 * Broker-based update consumption (Kafka, RabbitMQ) is configured separately via
 * {@code easygram.messaging.type} and does not appear here.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public enum BotTransportType {
    /** Long-polling transport — the bot repeatedly calls getUpdates. */
    LONG_POLLING,
    /** Webhook transport — Telegram pushes updates to an HTTPS endpoint. */
    WEBHOOK
}
