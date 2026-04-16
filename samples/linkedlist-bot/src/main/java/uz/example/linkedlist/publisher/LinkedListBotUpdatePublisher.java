package uz.example.linkedlist.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Custom {@link BotUpdatePublisher} that enqueues incoming Telegram updates into an
 * in-memory {@link LinkedBlockingDeque} instead of a real message broker.
 *
 * <p>Registering this bean as a {@code @Component} is enough to activate the
 * {@code BotUpdatePublishingFilter} auto-configuration — no Kafka or RabbitMQ
 * dependencies are required.</p>
 *
 * <p>With {@code easygram.messaging.forward-only=true} (configured in
 * {@code application.yml}), this is the <em>only</em> processing that happens on the
 * producer side: the update is placed in the queue and local bot handlers are skipped.
 * The {@link uz.example.linkedlist.consumer.LinkedListBotConsumer} then drains the queue
 * and dispatches each update to the handler pipeline.</p>
 *
 * @since 0.0.6
 */
@Component
public class LinkedListBotUpdatePublisher implements BotUpdatePublisher {

    private static final Logger log = LoggerFactory.getLogger(LinkedListBotUpdatePublisher.class);

    private final LinkedBlockingDeque<Update> botUpdateQueue;

    public LinkedListBotUpdatePublisher(LinkedBlockingDeque<Update> botUpdateQueue) {
        this.botUpdateQueue = botUpdateQueue;
    }

    /**
     * Offers the update to the tail of the deque, waiting up to 5 seconds for space.
     *
     * @param update the incoming Telegram update; never {@code null}
     */
    @Override
    public void publish(Update update) {
        try {
            boolean offered = botUpdateQueue.offerLast(update, 5, TimeUnit.SECONDS);
            if (offered) {
                log.debug("Enqueued update id={} (queue size={})", update.getUpdateId(), botUpdateQueue.size());
            } else {
                log.warn("Queue full — dropped update id={}", update.getUpdateId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while enqueuing update id={}", update.getUpdateId());
        }
    }
}
