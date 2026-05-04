package uz.osoncode.easygram.core.chatstate;

/**
 * Internal metrics hook for {@link InMemoryBotChatStateService}.
 *
 * <p>Implementations are notified on every state read, write, and clear, allowing
 * observability back-ends (e.g. Micrometer) to record counters without coupling the
 * core service class to any specific metrics library.</p>
 *
 * <p>When no metrics library is on the classpath, the service is created with a
 * no-op instance returned by {@link #noOp()}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.7
 */
public interface BotChatStateMetrics {

    /**
     * Called after a state look-up.
     *
     * @param hit {@code true} if the chat ID had a stored state; {@code false} if absent
     */
    void recordGet(boolean hit);

    /** Called after a state value has been stored. */
    void recordSet();

    /** Called after a state entry has been removed. */
    void recordClear();

    /**
     * Returns a no-op instance that discards all metrics.
     *
     * @return a stateless, side-effect-free {@link BotChatStateMetrics}
     */
    static BotChatStateMetrics noOp() {
        return new BotChatStateMetrics() {
            @Override public void recordGet(boolean hit) {}
            @Override public void recordSet() {}
            @Override public void recordClear() {}
        };
    }
}
