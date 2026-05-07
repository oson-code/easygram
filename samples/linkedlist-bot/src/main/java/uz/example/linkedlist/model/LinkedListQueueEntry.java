package uz.example.linkedlist.model;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

/**
 * Carrier that wraps a Telegram {@link Update} together with the W3C trace context
 * captured at the moment the update was enqueued.
 *
 * <p>The {@code traceContext} map holds W3C propagation headers (e.g.
 * {@code traceparent}, {@code tracestate}) injected by
 * {@link uz.example.linkedlist.publisher.LinkedListBotUpdatePublisher} via the Micrometer
 * {@code Propagator} API.  The consumer thread restores these headers to reconstruct
 * the parent span before dispatching the update — bridging the trace across the
 * in-memory queue boundary.</p>
 *
 * <p>When tracing is not on the classpath {@code traceContext} is an empty map and the
 * consumer falls back to trace-free dispatch.</p>
 *
 * @param update       the Telegram update to process; never {@code null}
 * @param traceContext W3C propagation headers captured at enqueue time; may be empty
 * @since 0.0.7
 */
public record LinkedListQueueEntry(Update update, Map<String, String> traceContext) {
}
