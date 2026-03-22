package uz.osoncode.easygram.core.filter;

/**
 * Pre-defined ordering constants for built-in {@link BotFilter} implementations.
 * Lower values run earlier in the filter chain.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public final class BotFilterOrder {

    /** Order for the {@code BotContextSetterFilter} — runs first to resolve User/Chat. */
    public static final int CONTEXT_SETTER = Integer.MIN_VALUE;
    /**
     * Order for the {@code BotObservabilityFilter} — runs at {@code MIN_VALUE + 1}, just after
     * {@link #CONTEXT_SETTER} so that {@code User} and {@code Chat} are already resolved.
     * Wraps the entire remaining pipeline (handler dispatch + send) in a Micrometer
     * {@code Observation}, producing a timer metric and a distributed trace span per update.
     */
    public static final int OBSERVATION = Integer.MIN_VALUE + 1;
    /** Order for the {@code BotApiMethodsSenderFilter} — runs after context setters. */
    public static final int API_SENDER = Integer.MIN_VALUE + 2;
    /** Order for the {@code BotUpdatePublishingFilter} — forwards updates to the broker. */
    public static final int PUBLISHING = Integer.MIN_VALUE + 1000;

    private BotFilterOrder() {
    }
}
