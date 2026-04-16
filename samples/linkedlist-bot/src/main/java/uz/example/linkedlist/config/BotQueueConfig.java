package uz.example.linkedlist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Defines the shared in-memory queue that acts as the "broker" for this sample.
 *
 * <p>Both the {@link uz.example.linkedlist.publisher.LinkedListBotUpdatePublisher producer}
 * and the {@link uz.example.linkedlist.consumer.LinkedListBotConsumer consumer} inject
 * this same {@link LinkedBlockingDeque} bean to exchange {@link Update} objects.</p>
 *
 * @since 0.0.6
 */
@Configuration
public class BotQueueConfig {

    /**
     * Creates the shared {@link LinkedBlockingDeque} used as the in-memory message queue.
     *
     * @return an unbounded blocking deque for {@link Update} objects
     */
    @Bean
    public LinkedBlockingDeque<Update> botUpdateQueue() {
        return new LinkedBlockingDeque<>();
    }
}
