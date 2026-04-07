package uz.osoncode.easygram.messaging;

/**
 * Declares the role of this bot instance in the broker integration.
 *
 * <p>Set via {@code easygram.messaging.type}. Omit the {@code messaging} block entirely for
 * standalone bots that do not connect to any message broker.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.5
 */
public enum MessagingType {

    /**
     * The bot receives updates directly from Telegram (via long-polling or webhook) and
     * publishes them to a broker. Local {@code @BotController} handlers run unless
     * {@code easygram.messaging.forward-only=true}.
     */
    PRODUCER,

    /**
     * The bot consumes updates from a broker topic/queue and dispatches them to local
     * {@code @BotController} handlers. No direct Telegram update transport is started.
     */
    CONSUMER
}
