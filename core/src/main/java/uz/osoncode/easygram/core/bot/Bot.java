package uz.osoncode.easygram.core.bot;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.BotFilterChain;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.model.BotMetadata;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Abstract base class for all Telegram bot implementations in this framework.
 * Manages the ordered {@link BotFilter} chain, the {@link BotDispatcher}, a thread-pool
 * {@link ExecutorService}, and the list of {@link BotStartTrigger} callbacks.
 * Subclasses integrate with a concrete Telegram client library (e.g., long-polling or
 * webhook) and forward incoming updates to {@link #handleUpdate(Update)}.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public abstract class Bot {

    /** The bot authentication token issued by BotFather. */
    protected final String token;

    /** The dispatcher that routes each request to the appropriate {@link uz.osoncode.easygram.core.handler.BotHandler}. */
    protected final BotDispatcher botDispatcher;

    /** The list of startup callbacks executed once after the bot authenticates. */
    protected final List<BotStartTrigger> triggers;

    /** The ordered list of filters applied to every incoming update. */
    protected final List<BotFilter> filters;

    /** The executor service used to process batches of updates concurrently. */
    protected final ExecutorService executorService;

    /** The Telegram API client used to send requests to the Bot API. */
    protected final TelegramClient telegramClient;

    /** The registry of exception handler methods defined in {@link uz.osoncode.easygram.core.stereotype.BotController} classes. */
    private final BotExceptionHandlerRegistry botExceptionHandlerRegistry;

    /** Metadata about the authenticated bot, populated during {@link #afterPropertiesSet()}. */
    protected User botMetaData;

    /**
     * Constructs a new {@code Bot} with the given infrastructure dependencies.
     * Filters are sorted in ascending order by {@link BotFilter#compareTo} before being stored.
     *
     * @param token                       the bot authentication token; must not be {@code null}
     * @param triggers                    the list of startup callbacks to execute after authentication; may be {@code null}
     * @param botDispatcher               the dispatcher that routes requests to handlers; must not be {@code null}
     * @param filters                     the list of filters to apply to each update; must not be {@code null}
     * @param executorService             the executor service for concurrent update processing; must not be {@code null}
     * @param telegramClient              the Telegram API client; must not be {@code null}
     * @param botExceptionHandlerRegistry the registry of exception handler methods; must not be {@code null}
     */
    public Bot(String token,
               List<BotStartTrigger> triggers,
               BotDispatcher botDispatcher,
               List<BotFilter> filters,
               ExecutorService executorService,
               TelegramClient telegramClient, BotExceptionHandlerRegistry botExceptionHandlerRegistry
    ) {
        this.token = token;
        this.triggers = triggers;
        this.botDispatcher = botDispatcher;
        this.filters = filters
                .stream()
                .sorted(BotFilter::compareTo)
                .toList();

        this.executorService = executorService;
        this.telegramClient = telegramClient;
        this.botExceptionHandlerRegistry = botExceptionHandlerRegistry;
    }

    /**
     * Initializes the bot after all properties have been set.
     * Executes a {@code getMe} request to populate {@link #botMetaData}, logs the result,
     * and then runs all registered {@link BotStartTrigger} callbacks.
     */
    @SneakyThrows
    public void afterPropertiesSet() {


        botMetaData = telegramClient.execute(GetMe.builder().build());
        log.info("Bot started. Bot : {}", botMetaData);

        if (Objects.nonNull(triggers)) {
            triggers.forEach(trigger -> trigger.execute(botMetaData, telegramClient));
            log.info("Bot triggers initialized");
        }
    }

    /**
     * Returns the metadata of the authenticated bot, populated after {@link #afterPropertiesSet()}.
     *
     * @return the bot's Telegram {@link User} object, or {@code null} if not yet initialized
     */
    public User getBotMetaData() {
        return botMetaData;
    }

    /**
     * Submits a batch of updates for asynchronous processing via the {@link #executorService}.
     * Each update in the list is processed independently by {@link #handleUpdate(Update)}.
     *
     * @param updates the list of incoming Telegram updates to process; must not be {@code null}
     */
    public final void handleUpdates(List<Update> updates) {
        for (Update update : updates) {
            executorService.submit(() -> this.handleUpdate(update));
        }
    }

    /**
     * Processes a single incoming Telegram update by constructing a fresh {@link BotFilterChain}
     * and passing a populated {@link BotRequest} through it.
     *
     * @param update the incoming Telegram update to process; must not be {@code null}
     */
    public final void handleUpdate(Update update) {
        BotFilterChain botFilterChain = new DefaultBotFilterChain(
                filters,
                botDispatcher,
                botExceptionHandlerRegistry
        );
        BotRequest botRequest = new BotRequest();
        botRequest.setUpdate(update);
        botRequest.setTelegramClient(telegramClient);

        if (botMetaData != null) {
            botRequest.setBotMetadata(BotMetadata.builder()
                    .token(token)
                    .id(botMetaData.getId())
                    .username(botMetaData.getUserName())
                    .build());
        }

        botFilterChain.doFilter(
                botRequest,
                new BotResponse()
        );
    }
}
