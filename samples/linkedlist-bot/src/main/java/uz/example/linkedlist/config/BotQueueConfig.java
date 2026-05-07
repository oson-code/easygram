package uz.example.linkedlist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uz.example.linkedlist.model.LinkedListQueueEntry;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Defines the shared in-memory queue that acts as the "broker" for this sample.
 *
 * <p>Both the {@link uz.example.linkedlist.publisher.LinkedListBotUpdatePublisher producer}
 * and the {@link uz.example.linkedlist.consumer.LinkedListBotConsumer consumer} inject
 * this same {@link LinkedBlockingDeque} bean to exchange
 * {@link LinkedListQueueEntry} objects.</p>
 *
 * <p>Each entry carries both the Telegram update <em>and</em> the W3C trace context
 * captured at enqueue time, so the consumer can restore the distributed trace across
 * the in-memory queue boundary.</p>
 *
 * @since 0.0.6
 */
@Configuration
public class BotQueueConfig {

    /**
     * Creates the shared {@link LinkedBlockingDeque} used as the in-memory message queue.
     *
     * @return an unbounded blocking deque for {@link LinkedListQueueEntry} objects
     */
    @Bean
    public LinkedBlockingDeque<LinkedListQueueEntry> botUpdateQueue() {
        return new LinkedBlockingDeque<>();
    }
}
