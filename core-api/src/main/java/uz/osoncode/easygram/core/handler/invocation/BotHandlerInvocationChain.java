package uz.osoncode.easygram.core.handler.invocation;

/**
 * Functional contract for advancing the bot handler invocation pipeline.
 *
 * <p>Each {@link BotHandlerInvocationFilter} receives an instance of this interface and is
 * responsible for calling {@link #proceed(BotHandlerInvocationContext)} to pass control to
 * the next filter in the chain. A filter that does <em>not</em> call {@code proceed}
 * short-circuits the pipeline — useful for early returns (e.g. permission denials).</p>
 *
 * <p>This interface mirrors {@link uz.osoncode.easygram.core.filter.BotFilterChain} at the
 * handler-invocation level. The outer {@code BotFilterChain} wraps the entire update
 * processing pipeline (all handlers), while {@code BotHandlerInvocationChain} wraps the
 * invocation of one specific matched handler method.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerInvocationFilter
 * @see BotHandlerInvocationContext
 */
@FunctionalInterface
public interface BotHandlerInvocationChain {

    /**
     * Passes control to the next filter in the invocation pipeline.
     *
     * @param context the mutable invocation context; must not be {@code null}
     */
    void proceed(BotHandlerInvocationContext context);
}
