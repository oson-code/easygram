package uz.osoncode.easygram.messaging;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * SPI for publishing incoming Telegram {@link Update} objects to an external message broker.
 *
 * <p>Implement this interface and register it as a Spring bean to integrate with any
 * message broker (Kafka, RabbitMQ, Redis Streams, etc.). The framework provides
 * ready-made implementations in the {@code messaging-kafka} and {@code messaging-rabbit}
 * modules.</p>
 *
 * <p>The {@link BotUpdatePublishingFilter} calls {@link #publish(Update)} for every
 * incoming update before (or instead of) forwarding it to the bot's handler pipeline,
 * depending on the {@code easygram.messaging.forward-only} property.</p>
 *
 * <p>Example custom implementation:</p>
 * <pre>{@code
 * @Component
 * public class MyCustomPublisher implements BotUpdatePublisher {
 *     @Override
 *     public void publish(Update update) {
 *         // send to your broker
 *     }
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotUpdatePublisher {

    /**
     * Publishes the given Telegram {@link Update} to the configured message broker.
     *
     * @param update the incoming Telegram update; never {@code null}
     */
    void publish(Update update);
}
