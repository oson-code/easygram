package uz.example.linkedlist.consumer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.example.linkedlist.model.LinkedListQueueEntry;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.messaging.BotUpdatePublishingFilter;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Consumer component that drains the shared in-memory queue and dispatches each
 * {@link Update} to the bot handler pipeline.
 *
 * <p>A single daemon thread is started in {@link #afterPropertiesSet()} and stopped
 * cleanly in {@link #destroy()}.  The consumer runs each update through a
 * {@link DefaultBotFilterChain} containing all framework filters <em>except</em>
 * {@link BotUpdatePublishingFilter} — which would otherwise re-enqueue the update
 * and create an infinite loop.</p>
 *
 * <p>Keeping the remaining filters active (particularly {@code BotContextSetterFilter}
 * and {@code BotApiMethodsSenderFilter}) is essential:</p>
 * <ul>
 *   <li>{@code BotContextSetterFilter} — populates {@link BotRequest#getUser()} and
 *       {@link BotRequest#getChat()} so that argument resolvers
 *       ({@code @BotUserArgumentResolver}, {@code @BotChatArgumentResolver}, etc.) work.</li>
 *   <li>{@code BotApiMethodsSenderFilter} — flushes the {@link BotResponse} to
 *       Telegram after the handler returns, so bot replies are actually delivered.</li>
 * </ul>
 *
 * <p>This mirrors the topology of a real distributed consumer: it runs in a separate
 * process that naturally has no publishing filter.</p>
 *
 * <h2>Trace propagation</h2>
 * <p>Each {@link LinkedListQueueEntry} carries a {@code traceContext} map with the W3C
 * {@code traceparent} header captured by the publisher.  Before dispatching, this consumer
 * extracts the parent span via
 * {@link Propagator#extract(Object, Propagator.Getter)}
 * and opens a {@link Tracer.SpanInScope} so that
 * {@code BotObservabilityFilter} automatically creates the {@code easygram.update} span as
 * a child of the restored parent.  The resulting trace tree visible in Grafana Tempo is:
 * <pre>
 *   long-polling → easygram.update → linkedlist.consumer → easygram.update
 *   (transport)    (producer side)    (this span)           (consumer side)
 * </pre>
 * When tracing is not on the classpath dispatch proceeds without span restoration.</p>
 *
 * @since 0.0.6
 */
@Component
public class LinkedListBotConsumer implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LinkedListBotConsumer.class);

    private final LinkedBlockingDeque<LinkedListQueueEntry> botUpdateQueue;
    private final BotDispatcher botDispatcher;
    private final BotExceptionHandlerRegistry botExceptionHandlerRegistry;
    private final EasygramTelegramClientProvider telegramClientProvider;
    private final EasygramProperties botProperties;
    private final List<BotFilter> allFilters;

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private Propagator propagator;

    private TelegramClient telegramClient;
    private volatile boolean running = true;
    private Thread consumerThread;

    public LinkedListBotConsumer(
            LinkedBlockingDeque<LinkedListQueueEntry> botUpdateQueue,
            BotDispatcher botDispatcher,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            EasygramTelegramClientProvider telegramClientProvider,
            EasygramProperties botProperties,
            List<BotFilter> allFilters) {
        this.botUpdateQueue = botUpdateQueue;
        this.botDispatcher = botDispatcher;
        this.botExceptionHandlerRegistry = botExceptionHandlerRegistry;
        this.telegramClientProvider = telegramClientProvider;
        this.botProperties = botProperties;
        this.allFilters = allFilters;
    }

    /**
     * Initialises the Telegram client and starts the consumer thread.
     */
    @Override
    public void afterPropertiesSet() {
        telegramClient = telegramClientProvider.provide(botProperties.token());
        consumerThread = new Thread(this::processLoop, "linkedlist-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("LinkedList consumer started — polling in-memory queue");
    }

    /**
     * Signals the consumer thread to stop and waits briefly for it to finish.
     */
    @Override
    public void destroy() throws InterruptedException {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
            consumerThread.join(5_000);
        }
        log.info("LinkedList consumer stopped");
    }

    private void processLoop() {
        while (running) {
            try {
                LinkedListQueueEntry entry = botUpdateQueue.poll(1, TimeUnit.SECONDS);
                if (entry != null) {
                    dispatchWithTrace(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Restores the W3C trace context from the queue entry (if tracing is active),
     * then dispatches the update.  The {@code linkedlist.consumer} span becomes the
     * parent of the {@code easygram.update} span created inside {@code dispatch()}.
     */
    private void dispatchWithTrace(LinkedListQueueEntry entry) {
        if (tracer != null && propagator != null && !entry.traceContext().isEmpty()) {
            Span span = propagator.extract(entry.traceContext(), (map, key) -> map.get(key))
                    .name("linkedlist.consumer")
                    .start();
            log.debug("Restored trace context for update id={}", entry.update().getUpdateId());
            try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
                dispatch(entry.update());
            } finally {
                span.end();
            }
        } else {
            dispatch(entry.update());
        }
    }

    private void dispatch(Update update) {
        log.debug("Dispatching update id={} from queue", update.getUpdateId());

        BotRequest request = new BotRequest();
        request.setUpdate(update);
        request.setTelegramClient(telegramClient);

        // Run through all framework filters EXCEPT BotUpdatePublishingFilter.
        // BotContextSetterFilter populates request.user/chat so argument resolvers work.
        // BotApiMethodsSenderFilter flushes the BotResponse so replies reach Telegram.
        // Excluding BotUpdatePublishingFilter prevents re-enqueueing the same update.
        List<BotFilter> consumerFilters = allFilters.stream()
                .filter(f -> !(f instanceof BotUpdatePublishingFilter))
                .toList();

        new DefaultBotFilterChain(consumerFilters, botDispatcher, botExceptionHandlerRegistry)
                .doFilter(request, new BotResponse());
    }
}
