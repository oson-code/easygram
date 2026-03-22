package uz.osoncode.easygram.core.handler.invocation;

import java.util.List;

/**
 * Immutable invocation chain that sequentially executes {@link BotHandlerInvocationFilter}
 * instances for one handler method call.
 *
 * <p>Each call to {@link #proceed} advances the chain by creating a new instance with an
 * incremented {@code startIndex}, so the chain is thread-safe and stateless per step.
 * When all filters have been traversed the chain terminates naturally — the final filter
 * (typically {@code ChatStateUpdateFilter}) is responsible for completing the invocation.</p>
 *
 * <p>This class mirrors {@link uz.osoncode.easygram.core.filter.DefaultBotFilterChain}
 * at the handler-invocation level. The pattern is the same: an ordered list of filters,
 * indexed traversal, immutable per step.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerInvocationFilter
 * @see BotHandlerInvocationContext
 */
public class DefaultBotHandlerInvocationChain implements BotHandlerInvocationChain {

    /** The sorted list of filters to execute. */
    private final List<BotHandlerInvocationFilter> filters;

    /** Index of the next filter to execute. */
    private final int startIndex;

    /**
     * Creates the root invocation chain starting at index {@code 0}.
     *
     * @param filters the ordered list of filters; must be non-null and already sorted by order
     */
    public DefaultBotHandlerInvocationChain(List<BotHandlerInvocationFilter> filters) {
        this(filters, 0);
    }

    /**
     * Creates a chain step starting at the given index.
     *
     * @param filters    the ordered list of filters
     * @param startIndex the index of the filter to execute in this step
     */
    private DefaultBotHandlerInvocationChain(List<BotHandlerInvocationFilter> filters, int startIndex) {
        this.filters = filters;
        this.startIndex = startIndex;
    }

    /**
     * Advances the invocation pipeline by executing the next filter, or terminates if all
     * filters have been processed.
     *
     * @param context the mutable invocation context; must not be {@code null}
     */
    @Override
    public void proceed(BotHandlerInvocationContext context) {
        if (startIndex < filters.size()) {
            BotHandlerInvocationFilter current = filters.get(startIndex);
            BotHandlerInvocationChain next = new DefaultBotHandlerInvocationChain(filters, startIndex + 1);
            current.invoke(context, next);
        }
        // else: chain exhausted — nothing more to do
    }
}
