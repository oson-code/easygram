package uz.example.linkedlist.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.LinkedBlockingDeque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LinkedListBotUpdatePublisher}.
 */
class LinkedListBotUpdatePublisherTest {

    private LinkedBlockingDeque<Update> deque;
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
        assertThat(deque).containsExactly(update);
    }

    @Test
    void publish_multipleUpdates_fifoOrder() {
        Update first = mockUpdate(1);
        Update second = mockUpdate(2);
        Update third = mockUpdate(3);

        publisher.publish(first);
        publisher.publish(second);
        publisher.publish(third);

        assertThat(deque).containsExactly(first, second, third);
    }

    @Test
    void publish_queueFull_dropsUpdateGracefully() {
        LinkedBlockingDeque<Update> singleSlotDeque = new LinkedBlockingDeque<>(1);
        LinkedListBotUpdatePublisher boundedPublisher = new LinkedListBotUpdatePublisher(singleSlotDeque);

        Update first = mockUpdate(1);
        Update second = mockUpdate(2);

        boundedPublisher.publish(first);
        boundedPublisher.publish(second);  // should be dropped — no exception thrown

        assertThat(singleSlotDeque).containsExactly(first);
    }

    @Test
    void publish_dequeHasCorrectSizeAfterMultiplePublishes() {
        for (int i = 1; i <= 5; i++) {
            publisher.publish(mockUpdate(i));
        }
        assertThat(deque).hasSize(5);
    }

    private static Update mockUpdate(int id) {
        Update update = mock(Update.class);
        when(update.getUpdateId()).thenReturn(id);
        return update;
    }
}
