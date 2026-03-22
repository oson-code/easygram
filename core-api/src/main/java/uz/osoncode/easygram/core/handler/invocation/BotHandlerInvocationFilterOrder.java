package uz.osoncode.easygram.core.handler.invocation;

/**
 * Order constants for the built-in {@link BotHandlerInvocationFilter} implementations.
 *
 * <p>These values define the execution sequence of the framework's built-in invocation
 * filters. Custom filters should use values outside this range or between these values
 * to insert themselves at the appropriate point in the pipeline.</p>
 *
 * <table>
 *   <caption>Built-in filter execution order</caption>
 *   <tr><th>Constant</th><th>Value</th><th>Filter</th><th>Purpose</th></tr>
 *   <tr><td>{@link #METHOD_INVOCATION}</td><td>{@code MIN_VALUE}</td>
 *       <td>{@code MethodInvocationFilter}</td>
 *       <td>Resolves method arguments and invokes the controller method reflectively;
 *           sets {@code ctx.returnValue}</td></tr>
 *   <tr><td>{@link #MARKUP_APPLICATION}</td><td>{@code MIN_VALUE + 1}</td>
 *       <td>{@code MarkupApplicationFilter}</td>
 *       <td>Reads {@code @BotReplyMarkup} / {@code @BotClearMarkup} and transforms the
 *           return value (MarkupAware types) or wraps String returns in a markup-aware reply</td></tr>
 *   <tr><td>{@link #RETURN_TYPE_DISPATCH}</td><td>{@code MIN_VALUE + 2}</td>
 *       <td>{@code ReturnTypeDispatchFilter}</td>
 *       <td>Selects the appropriate {@link uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler}
 *           and dispatches the (possibly transformed) return value to the response</td></tr>
 *   <tr><td>{@link #CHAT_STATE_UPDATE}</td><td>{@code MIN_VALUE + 3}</td>
 *       <td>{@code ChatStateUpdateFilter}</td>
 *       <td>Applies {@code @BotForwardChatState} / {@code @BotClearChatState} <em>after</em>
 *           the return value has been dispatched (state transitions only on success)</td></tr>
 * </table>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerInvocationFilter
 */
public final class BotHandlerInvocationFilterOrder {

    private BotHandlerInvocationFilterOrder() {
    }

    /**
     * Order for the {@code MethodInvocationFilter} — runs first; resolves arguments and
     * invokes the handler method reflectively, then sets {@code ctx.returnValue}.
     */
    public static final int METHOD_INVOCATION = Integer.MIN_VALUE;

    /**
     * Order for the {@code MarkupApplicationFilter} — runs at {@code MIN_VALUE + 1}, just
     * after the method has been invoked and {@code ctx.returnValue} has been set.
     * Applies {@code @BotReplyMarkup} / {@code @BotClearMarkup} to the return value,
     * transforming {@link uz.osoncode.easygram.core.markup.MarkupAware} types in-place.
     */
    public static final int MARKUP_APPLICATION = Integer.MIN_VALUE + 1;

    /**
     * Order for the {@code ReturnTypeDispatchFilter} — runs at {@code MIN_VALUE + 2}, after
     * markup has been applied. Selects and delegates to the correct
     * {@link uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler}.
     */
    public static final int RETURN_TYPE_DISPATCH = Integer.MIN_VALUE + 2;

    /**
     * Order for the {@code ChatStateUpdateFilter} — runs at {@code MIN_VALUE + 3}, last among
     * built-ins. Applies {@code @BotForwardChatState} and {@code @BotClearChatState} only after
     * the return value has been successfully dispatched.
     */
    public static final int CHAT_STATE_UPDATE = Integer.MIN_VALUE + 3;
}
