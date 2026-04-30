package uz.osoncode.easygram.core.handler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uz.osoncode.easygram.core.handler.invocation.BotHandlerInvocationContext;
import uz.osoncode.easygram.core.handler.invocation.BotHandlerInvocationFilter;
import uz.osoncode.easygram.core.handler.invocation.DefaultBotHandlerInvocationChain;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

/**
 * {@link BotHandler} implementation that delegates to a controller method via reflection.
 *
 * <p>At dispatch time, {@link #supports} evaluates a {@link Predicate} supplied during
 * construction (composed from {@link BotHandlerCondition} objects, typically a
 * {@link BotMetaDataCondition} and an optional {@link BotChatStateCondition}).
 * When the handler is selected, {@link #handle} creates a
 * {@link BotHandlerInvocationContext} and runs it through the ordered
 * {@link BotHandlerInvocationFilter} chain.</p>
 *
 * <p>The built-in filter chain processes each invocation in four steps:</p>
 * <ol>
 *   <li>{@code MethodInvocationFilter} — resolves method parameters and invokes the method</li>
 *   <li>{@code MarkupApplicationFilter} — applies {@code @BotReplyMarkup} / {@code @BotClearMarkup}</li>
 *   <li>{@code ReturnTypeDispatchFilter} — dispatches the return value via
 *       {@link uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory}</li>
 *   <li>{@code ChatStateUpdateFilter} — applies {@code @BotForwardChatState} /
 *       {@code @BotClearChatState}</li>
 * </ol>
 *
 * <p>Custom cross-cutting behavior can be injected by adding a
 * {@link BotHandlerInvocationFilter} bean to the Spring application context — no changes
 * to this class are required.</p>
 *
 * <p>Instances are created by {@link BotHandlerLoader} and stored in
 * {@link BotHandlerRegistry}. The {@link #getOrder()} value controls the priority of this
 * handler relative to others supporting the same request.</p>
 *
 * @author Islom Mirsaburov
 * @see BotHandlerInvocationFilter
 * @see BotHandlerCondition
 * @see BotHandlerLoader
 * @since 0.0.1
 */
@Slf4j
public class BotMethodHandler implements BotHandler {

    /**
     * The controller method to be invoked when this handler is selected.
     */
    private final Method method;

    /**
     * The controller bean instance that owns the method.
     */
    private final Object bean;

    /**
     * Composed predicate built from {@link BotHandlerCondition} objects at registration time.
     */
    private final Predicate<BotRequest> supportsPredicate;

    /**
     * Ordered list of invocation filters that process each handler call.
     * Sorted by {@link BotHandlerInvocationFilter#getOrder()} ascending.
     */
    private final List<BotHandlerInvocationFilter> invocationFilters;

    /**
     * Priority order; lower values are checked first.
     */
    @Getter
    private final int order;

    /**
     * {@code true} when this handler requires a specific chat state (non-empty
     * {@code @BotChatState}). Used as a tie-breaker in {@link #compareTo}: when two
     * handlers share the same {@link #order}, the state-specific one sorts first so that
     * it is evaluated before a state-agnostic handler with the same command/text pattern.
     */
    private final boolean hasChatStateRestriction;

    /**
     * Creates a new {@code BotMethodHandler}.
     *
     * @param method                  the controller method to invoke
     * @param bean                    the controller bean that owns the method
     * @param supportsPredicate       predicate composed from {@link BotHandlerCondition}s
     *                                that decides whether this handler matches a request
     * @param invocationFilters       ordered list of filters that process each invocation
     * @param order                   sort priority; lower values are checked first
     * @param hasChatStateRestriction {@code true} if this handler requires a specific chat state
     */
    public BotMethodHandler(Method method,
                            Object bean,
                            Predicate<BotRequest> supportsPredicate,
                            List<BotHandlerInvocationFilter> invocationFilters,
                            int order,
                            boolean hasChatStateRestriction) {
        this.method = method;
        this.bean = bean;
        this.supportsPredicate = supportsPredicate;
        this.invocationFilters = invocationFilters;
        this.order = order;
        this.hasChatStateRestriction = hasChatStateRestriction;
    }

    /**
     * Returns {@code true} if this handler supports the given {@link BotRequest}.
     *
     * <p>Delegates to the composed {@code supportsPredicate} built from
     * {@link BotHandlerCondition} instances at construction time.</p>
     *
     * @param botRequest the current bot request to evaluate
     * @return {@code true} if this handler can process the request; {@code false} otherwise
     */
    @Override
    public boolean supports(BotRequest botRequest) {
        return supportsPredicate.test(botRequest);
    }

    /**
     * Compares this handler to another by order, then by state-restriction as a tie-breaker.
     *
     * <p>When two handlers share the same {@link #order} value, a state-specific handler
     * (one with {@code hasChatStateRestriction = true}) sorts before a state-agnostic one,
     * ensuring the more-specific handler is evaluated first.</p>
     *
     * @param o the other handler to compare against
     * @return negative if this handler has higher priority, positive if lower, 0 if equal
     */
    @Override
    public int compareTo(BotHandler o) {
        int orderCmp = Integer.compare(this.order, o.getOrder());
        if (orderCmp != 0) return orderCmp;

        // Tie-break: state-specific handlers sort before state-agnostic ones.
        if (o instanceof BotMethodHandler other) {
            if (this.hasChatStateRestriction && !other.hasChatStateRestriction) return -1;
            if (!this.hasChatStateRestriction && other.hasChatStateRestriction) return 1;
        }
        return 0;
    }

    /**
     * Handles the given request by running the invocation filter chain.
     *
     * <p>Creates a {@link BotHandlerInvocationContext} for this request and passes it
     * through the {@link DefaultBotHandlerInvocationChain}. The chain's built-in filters
     * handle argument resolution, method invocation, markup application, return-type
     * dispatch, and chat-state transitions in sequence.</p>
     *
     * @param botRequest  the current bot request providing context for argument resolution
     * @param botResponse the response object populated by the handler or return-type handler
     */
    @Override
    public void handle(BotRequest botRequest, BotResponse botResponse) throws InvocationTargetException, IllegalAccessException {
        log.trace("Dispatching to handler: {}.{}()", bean.getClass().getSimpleName(), method.getName());
        botRequest.setAttribute("easygram.controllerClass", bean.getClass());
        BotHandlerInvocationContext context = new BotHandlerInvocationContext(botRequest, botResponse, method, bean);
        new DefaultBotHandlerInvocationChain(invocationFilters).proceed(context);
    }

    @Override
    public String info() {
        return method.toString();
    }
}
