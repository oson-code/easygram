package uz.example.linkedlist.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.example.linkedlist.model.LinkedListQueueEntry;

import java.util.concurrent.LinkedBlockingDeque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LinkedListBotUpdatePublisher}.
 *
 * <p>The {@link io.micrometer.tracing.Tracer} and {@link io.micrometer.tracing.propagation.Propagator}
 * are not wired in these tests (they are {@code null}), so no W3C headers are injected and
 * each {@link LinkedListQueueEntry#traceContext()} is an empty map.</p>
 */
class LinkedListBotUpdatePublisherTest {

    private LinkedBlockingDeque<LinkedListQueueEntry> deque;
    private LinkedListBotUpdatePublisher publisher;

    @BeforeEach
    void setUp() {
        deque = new LinkedBlockingDeque<>();
        publisher = new LinkedListBotUpdatePublisher(deque);
    }

    @Test
    void publish_enqueuesUpdateInDeque() {
        Update update = mockUpdate(1);
        publisher.publish(update);
        assertThat(deque).extracting(LinkedListQueueEntry::update).containsExactly(update);
    }

    @Test
    void publish_multipleUpdates_fifoOrder() {
        Update first = mockUpdate(1);
        Update second = mockUpdate(2);
        Update third = mockUpdate(3);

        publisher.publish(first);
        publisher.publish(second);
        publisher.publish(third);

        assertThat(deque).extracting(LinkedListQueueEntry::update).containsExactly(first, second, third);
    }

    @Test
    void publish_queueFull_dropsUpdateGracefully() {
        LinkedBlockingDeque<LinkedListQueueEntry> singleSlotDeque = new LinkedBlockingDeque<>(1);
        LinkedListBotUpdatePublisher boundedPublisher = new LinkedListBotUpdatePublisher(singleSlotDeque);

        Update first = mockUpdate(1);
        Update second = mockUpdate(2);

        boundedPublisher.publish(first);
        boundedPublisher.publish(second);  // should be dropped — no exception thrown

        assertThat(singleSlotDeque).extracting(LinkedListQueueEntry::update).containsExactly(first);
    }

    @Test
    void publish_dequeHasCorrectSizeAfterMultiplePublishes() {
        for (int i = 1; i <= 5; i++) {
            publisher.publish(mockUpdate(i));
        }
        assertThat(deque).hasSize(5);
    }

    @Test
    void publish_withoutTracer_traceContextIsEmpty() {
        // tracer/propagator are null (not autowired in unit tests) — traceContext must be empty
        Update update = mockUpdate(42);
        publisher.publish(update);

        assertThat(deque).hasSize(1);
        assertThat(deque.peek()).isNotNull();
        assertThat(deque.peek().traceContext()).isEmpty();
    }

    private static Update mockUpdate(int id) {
        Update update = mock(Update.class);
        when(update.getUpdateId()).thenReturn(id);
        return update;
    }
}
