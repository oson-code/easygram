package uz.osoncode.easygram.core.filter;

/**
 * Pre-defined ordering constants for built-in {@link BotFilter} implementations.
 * Lower values run earlier in the filter chain (consistent with Spring's {@code Ordered}).
 *
 * <p>Default execution order:</p>
 * <ol>
 *   <li>{@link #MDC_CONTEXT} — initialises MDC correlation keys for every log statement
 *       in the downstream pipeline.</li>
 *   <li>{@link #CONTEXT_SETTER} — resolves {@code Chat} and {@code User} from the update,
 *       then enriches the MDC with {@code bot.chat.id} / {@code bot.user.id}.</li>
 *   <li>{@link #OBSERVATION} — wraps the remaining pipeline in a Micrometer
 *       {@code Observation} (timer + distributed trace span).</li>
 *   <li>{@link #API_SENDER} — calls the rest of the chain, then sends all accumulated
 *       {@code BotApiMethod} responses.</li>
 *   <li>{@link #PUBLISHING} — publishes the raw update to an external message broker
 *       (Kafka / RabbitMQ).</li>
 * </ol>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public final class BotFilterOrder {

    /**
     * Order for the {@code BotMdcFilter} — the very first filter to run.
     * Sets MDC keys {@code bot.update.id} and {@code bot.transport} so that every
     * subsequent log statement — including those emitted by the context-setter,
     * dispatcher, handlers, and broker filters — automatically carries the update
     * correlation context. Cleared in a {@code finally} block after the chain returns.
     *
     * @since 0.0.4
     */
    public static final int MDC_CONTEXT = Integer.MIN_VALUE;

    /**
     * Order for the {@code BotContextSetterFilter} — runs just after {@link #MDC_CONTEXT}.
     * Extracts {@code Chat} and {@code User} from the incoming Telegram {@code Update} and
     * stores them on {@link uz.osoncode.easygram.core.model.BotRequest}. After resolution,
     * enriches the MDC with {@code bot.chat.id} and {@code bot.user.id}.
     */
    public static final int CONTEXT_SETTER = Integer.MIN_VALUE + 1;

    /**
     * Order for the {@code BotObservabilityFilter} — runs just after {@link #CONTEXT_SETTER}
     * so that {@code User} and {@code Chat} are already resolved when the observation opens.
     * Wraps the entire remaining pipeline (handler dispatch + send) in a Micrometer
     * {@code Observation}, producing a timer metric and a distributed trace span per update.
     */
    public static final int OBSERVATION = Integer.MIN_VALUE + 2;

    /**
     * Order for the {@code BotApiMethodsSenderFilter}.
     * Calls the downstream chain first, then iterates over all {@code BotApiMethod} entries
     * collected in {@link uz.osoncode.easygram.core.model.BotResponse} and dispatches each
     * one via {@link org.telegram.telegrambots.meta.generics.TelegramClient#executeAsync}.
     */
    public static final int API_SENDER = Integer.MIN_VALUE + 3;

    /**
     * Order for the {@code BotUpdatePublishingFilter} — forwards the raw Telegram update to
     * an external message broker (Kafka, RabbitMQ, etc.) via {@code BotUpdatePublisher}.
     * Runs well after the context filters to ensure full request context is available.
     */
    public static final int PUBLISHING = Integer.MIN_VALUE + 1000;

    private BotFilterOrder() {
    }
}
