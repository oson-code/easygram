package uz.example.linkedlist.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Consumer component that drains the shared in-memory queue and dispatches each
 * {@link Update} to the bot handler pipeline.
 *
 * <p>A single daemon thread is started in {@link #afterPropertiesSet()} and stopped
 * cleanly in {@link #destroy()}.  The consumer runs the update directly through a
 * {@link DefaultBotFilterChain} with <em>no filters</em> (to avoid re-triggering the
 * {@code BotUpdatePublishingFilter} that put the update into the queue in the first
 * place).</p>
 *
 * <p>This is intentional: in a real distributed setup the consumer runs in a <em>separate
 * process</em> that has no publishing filter at all.  The empty filter list here mirrors
 * that topology within a single JVM.</p>
 *
 * @since 0.0.6
 */
@Component
public class LinkedListBotConsumer implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(LinkedListBotConsumer.class);

    private final LinkedBlockingDeque<Update> botUpdateQueue;
    private final BotDispatcher botDispatcher;
    private final BotExceptionHandlerRegistry botExceptionHandlerRegistry;
    private final BotTelegramClientProvider telegramClientProvider;
    private final EasygramProperties botProperties;

    private TelegramClient telegramClient;
    private volatile boolean running = true;
    private Thread consumerThread;

    public LinkedListBotConsumer(
            LinkedBlockingDeque<Update> botUpdateQueue,
            BotDispatcher botDispatcher,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            BotTelegramClientProvider telegramClientProvider,
            EasygramProperties botProperties) {
        this.botUpdateQueue = botUpdateQueue;
        this.botDispatcher = botDispatcher;
        this.botExceptionHandlerRegistry = botExceptionHandlerRegistry;
        this.telegramClientProvider = telegramClientProvider;
        this.botProperties = botProperties;
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
                Update update = botUpdateQueue.poll(1, TimeUnit.SECONDS);
                if (update != null) {
                    dispatch(update);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void dispatch(Update update) {
        log.debug("Dispatching update id={} from queue", update.getUpdateId());

        BotRequest request = new BotRequest();
        request.setUpdate(update);
        request.setTelegramClient(telegramClient);

        // Run through the filter chain with no filters so updates are passed directly
        // to the dispatcher — no risk of re-triggering BotUpdatePublishingFilter.
        new DefaultBotFilterChain(Collections.emptyList(), botDispatcher, botExceptionHandlerRegistry)
                .doFilter(request, new BotResponse());
    }
}
