package uz.osoncode.easygram.core.filter;

import lombok.extern.slf4j.Slf4j;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Immutable filter chain that sequentially invokes {@link BotFilter} instances before
 * delegating to the {@link BotDispatcher}.
 *
 * <p>Each call to {@link #doFilter} advances the chain by creating a new
 * {@code BotFilterChain} instance with an incremented {@code startIndex}, so the chain
 * itself is effectively immutable and thread-safe per request. When all filters have been
 * traversed, the request is handed off to {@link BotDispatcher#dispatch}.
 *
 * <p>Any {@link Exception} thrown during filter or dispatch processing is caught here.
 * If the exception originates from a reflective invocation, the underlying cause is
 * unwrapped from {@link InvocationTargetException} before being matched against
 * registered exception handlers in {@link BotExceptionHandlerRegistry}. If no handler is
 * found, the error is logged.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class DefaultBotFilterChain implements BotFilterChain {

    /** Ordered list of filters to be applied to each request. */
    private final List<BotFilter> filters;

    /** Index of the next filter to execute in this chain step. */
    private final int startIndex;

    /** Dispatcher invoked after all filters have been executed. */
    private final BotDispatcher botDispatcher;

    /** Registry of exception handlers consulted when processing throws an exception. */
    private final BotExceptionHandlerRegistry botExceptionHandlerRegistry;

    /**
     * Creates the root filter chain starting at index {@code 0}.
     *
     * @param filters                     ordered list of filters to apply.
     * @param botDispatcher               dispatcher to invoke after all filters pass.
     * @param botExceptionHandlerRegistry registry used to route exceptions to handlers.
     */
    public DefaultBotFilterChain(List<BotFilter> filters, BotDispatcher botDispatcher,
                          BotExceptionHandlerRegistry botExceptionHandlerRegistry) {
        this(filters, botDispatcher, botExceptionHandlerRegistry, 0);
    }

    /**
     * Creates a filter chain step starting at the given index.
     *
     * @param filters                     ordered list of filters to apply.
     * @param botDispatcher               dispatcher to invoke after all filters pass.
     * @param botExceptionHandlerRegistry registry used to route exceptions to handlers.
     * @param startIndex                  index of the filter to execute in this step.
     */
    private DefaultBotFilterChain(List<BotFilter> filters, BotDispatcher botDispatcher,
                            BotExceptionHandlerRegistry botExceptionHandlerRegistry, int startIndex) {
        this.filters = filters;
        this.botDispatcher = botDispatcher;
        this.botExceptionHandlerRegistry = botExceptionHandlerRegistry;
        this.startIndex = startIndex;
    }

    /**
     * Advances the filter chain by executing the next filter, or delegates to the dispatcher
     * when all filters have been processed.
     *
     * <p>If the current filter's {@link BotFilter#shouldFilter} returns {@code false},
     * the filter is skipped and the chain proceeds to the next step. Any exception thrown
     * during processing is caught, unwrapped if necessary, and routed to the appropriate
     * exception handler registered in {@link BotExceptionHandlerRegistry}.
     *
     * @param botRequest  the incoming bot request containing the Telegram {@code Update}.
     * @param botResponse the response object that filters and handlers may populate.
     */
    @Override
    public void doFilter(BotRequest botRequest, BotResponse botResponse) {
        try {
            if (startIndex < filters.size()) {
                BotFilter currentFilter = filters.get(startIndex);
                BotFilterChain next = new DefaultBotFilterChain(filters, botDispatcher, botExceptionHandlerRegistry, startIndex + 1);
                if (currentFilter.shouldFilter(botRequest, botResponse))
                    currentFilter.doFilter(botRequest, botResponse, next);
                else
                    next.doFilter(botRequest, botResponse);
            } else {
                botDispatcher.dispatch(botRequest, botResponse);
            }
        } catch (Exception e) {
            Throwable targetException;
            if (e instanceof InvocationTargetException a) {
                targetException = a.getTargetException();
            } else {
                targetException = e;
            }
            boolean handled = botExceptionHandlerRegistry.getBotHandlers()
                    .stream()
                    .filter(h -> h.supports(targetException, botRequest))
                    .findFirst()
                    .map(h -> { h.handle(botRequest, botResponse, targetException); return true; })
                    .orElse(false);

            botRequest.setThrowable(targetException);
            if (!handled) {
                log.error("No exception handler found for exception: ", targetException);
                if (targetException instanceof RuntimeException re) throw re;
                if (targetException instanceof Error er) throw er;
                throw new RuntimeException("Unhandled exception during bot dispatch", targetException);
            }
        }
    }
}
