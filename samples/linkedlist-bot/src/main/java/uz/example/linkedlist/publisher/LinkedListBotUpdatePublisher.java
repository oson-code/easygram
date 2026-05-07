package uz.example.linkedlist.publisher;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.example.linkedlist.model.LinkedListQueueEntry;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;

import java.util.HashMap;
import java.util.Map;
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
 * <h2>Trace propagation</h2>
 * <p>When {@code micrometer-tracing-bridge-otel} is on the classpath, this publisher
 * captures the current W3C {@code traceparent} header via
 * {@link Propagator#inject(io.micrometer.tracing.TraceContext, Object, Propagator.Setter)}
 * and stores it in {@link LinkedListQueueEntry#traceContext()}.  The consumer then
 * restores the parent span from these headers so that the full trace tree is visible in
 * Grafana Tempo:
 * <pre>
 *   long-polling → easygram.update → linkedlist.consumer → easygram.update
 *   (transport)    (producer side)    (restored context)    (consumer side)
 * </pre>
 * When tracing is not on the classpath both fields are {@code null} and enqueue proceeds
 * without any W3C header injection.</p>
 *
 * @since 0.0.6
 */
@Component
public class LinkedListBotUpdatePublisher implements BotUpdatePublisher {

    private static final Logger log = LoggerFactory.getLogger(LinkedListBotUpdatePublisher.class);

    private final LinkedBlockingDeque<LinkedListQueueEntry> botUpdateQueue;

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private Propagator propagator;

    public LinkedListBotUpdatePublisher(LinkedBlockingDeque<LinkedListQueueEntry> botUpdateQueue) {
        this.botUpdateQueue = botUpdateQueue;
    }

    /**
     * Captures the current trace context, wraps the update in a {@link LinkedListQueueEntry},
     * and offers it to the tail of the deque, waiting up to 5 seconds for space.
     *
     * @param update the incoming Telegram update; never {@code null}
     */
    @Override
    public void publish(Update update) {
        Map<String, String> traceHeaders = buildTraceHeaders();
        try {
            LinkedListQueueEntry entry = new LinkedListQueueEntry(update, traceHeaders);
            boolean offered = botUpdateQueue.offerLast(entry, 5, TimeUnit.SECONDS);
            if (offered) {
                log.debug("Enqueued update id={} traceHeaders={} (queue size={})",
                        update.getUpdateId(), traceHeaders, botUpdateQueue.size());
            } else {
                log.warn("Queue full — dropped update id={}", update.getUpdateId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while enqueuing update id={}", update.getUpdateId());
        }
    }

    /**
     * Injects the current span's W3C context into a fresh map.
     * Returns an empty map when tracing is disabled or there is no active span.
     */
    private Map<String, String> buildTraceHeaders() {
        if (tracer == null || propagator == null) {
            return Map.of();
        }
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return Map.of();
        }
        Map<String, String> headers = new HashMap<>();
        propagator.inject(currentSpan.context(), headers, Map::put);
        return headers;
    }
}
