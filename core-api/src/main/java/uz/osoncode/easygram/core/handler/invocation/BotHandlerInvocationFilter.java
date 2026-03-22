package uz.osoncode.easygram.core.handler.invocation;

/**
 * Interceptor interface for the bot handler method invocation pipeline.
 *
 * <p>Filters wrap the execution of a single matched handler method, enabling cross-cutting
 * concerns such as argument resolution, markup transformation, return-type dispatch,
 * state management, logging, and permission checks to be applied as independent, composable
 * steps.</p>
 *
 * <p>The framework provides four built-in filters executed in this order:</p>
 * <ol>
 *   <li>{@code MethodInvocationFilter} ({@link BotHandlerInvocationFilterOrder#METHOD_INVOCATION}) —
 *       resolves method arguments and invokes the controller method reflectively</li>
 *   <li>{@code MarkupApplicationFilter} ({@link BotHandlerInvocationFilterOrder#MARKUP_APPLICATION}) —
 *       applies {@code @BotReplyMarkup} / {@code @BotClearMarkup} annotations to the return value</li>
 *   <li>{@code ReturnTypeDispatchFilter} ({@link BotHandlerInvocationFilterOrder#RETURN_TYPE_DISPATCH}) —
 *       selects the appropriate {@link uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler}
 *       and dispatches the return value</li>
 *   <li>{@code ChatStateUpdateFilter} ({@link BotHandlerInvocationFilterOrder#CHAT_STATE_UPDATE}) —
 *       applies {@code @BotForwardChatState} / {@code @BotClearChatState} after dispatch</li>
 * </ol>
 *
 * <p>Register a custom filter as a Spring {@code @Bean} to extend the pipeline without
 * modifying any framework class:</p>
 *
 * <pre>{@code
 * @Bean
 * public BotHandlerInvocationFilter auditFilter() {
 *     return new BotHandlerInvocationFilter() {
 *         public int getOrder() { return 0; }  // runs before built-ins
 *
 *         public void invoke(BotHandlerInvocationContext ctx, BotHandlerInvocationChain chain) {
 *             log.info("Invoking {}", ctx.getMethod().getName());
 *             chain.proceed(ctx);
 *             log.info("Completed {}, returnValue={}", ctx.getMethod().getName(), ctx.getReturnValue());
 *         }
 *     };
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerInvocationChain
 * @see BotHandlerInvocationContext
 * @see BotHandlerInvocationFilterOrder
 */
public interface BotHandlerInvocationFilter extends Comparable<BotHandlerInvocationFilter> {

    /**
     * Returns the priority order of this filter.
     * Lower values run earlier in the chain. Defaults to {@link Integer#MAX_VALUE}.
     *
     * @return the order value
     */
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * Executes this filter's logic and optionally delegates to the rest of the chain.
     *
     * <p>Implementations must call {@code chain.proceed(context)} to continue execution.
     * Omitting this call short-circuits the pipeline — useful for permission guards or
     * other early-return scenarios.</p>
     *
     * @param context the mutable invocation context; must not be {@code null}
     * @param chain   the remaining chain to delegate to; must not be {@code null}
     */
    void invoke(BotHandlerInvocationContext context, BotHandlerInvocationChain chain);

    /**
     * Compares this filter to another for ordering.
     *
     * @param o the other filter; must not be {@code null}
     * @return negative, zero, or positive as this filter's order is lower, equal, or higher
     */
    @Override
    default int compareTo(BotHandlerInvocationFilter o) {
        return Integer.compare(getOrder(), o.getOrder());
    }
}
