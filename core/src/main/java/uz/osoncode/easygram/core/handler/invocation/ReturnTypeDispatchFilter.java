package uz.osoncode.easygram.core.handler.invocation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uz.osoncode.easygram.core.markup.MarkupAware;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * {@link BotHandlerInvocationFilter} that selects the appropriate
 * {@link BotReturnTypeHandler} and dispatches the handler method's return value.
 *
 * <p>Runs at order {@link BotHandlerInvocationFilterOrder#RETURN_TYPE_DISPATCH}, after
 * {@code MarkupApplicationFilter} has normalised the return value. Selection strategy:</p>
 * <ul>
 *   <li>If {@code ctx.returnValue} is a {@link MarkupAware} instance, the handler is
 *       selected by the runtime value type ({@link BotReturnTypeHandlerFactory#getReturnTypeHandler(Object)}).</li>
 *   <li>Otherwise the handler is selected by the method's declared return type
 *       ({@link BotReturnTypeHandlerFactory#getReturnTypeHandler(java.lang.reflect.Method)}).</li>
 * </ul>
 *
 * <p>The 4-arg {@code handleReturnType} overload is always called so handlers that
 * previously relied on method-level annotation inspection (e.g. the old
 * {@code BotStringReturnHandler} method override) retain access to the originating method.
 * After dispatching, this filter calls {@code chain.proceed(context)} to allow subsequent
 * filters (e.g. {@code ChatStateUpdateFilter}) to run.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class ReturnTypeDispatchFilter implements BotHandlerInvocationFilter {

    private final BotReturnTypeHandlerFactory botReturnTypeHandlerFactory;

    /**
     * {@inheritDoc}
     *
     * @return {@link BotHandlerInvocationFilterOrder#RETURN_TYPE_DISPATCH}
     */
    @Override
    public int getOrder() {
        return BotHandlerInvocationFilterOrder.RETURN_TYPE_DISPATCH;
    }

    /**
     * Selects a return-type handler and dispatches the return value.
     *
     * @param context the mutable invocation context
     * @param chain   the remaining filter chain
     */
    @Override
    public void invoke(BotHandlerInvocationContext context, BotHandlerInvocationChain chain) throws InvocationTargetException, IllegalAccessException {
        Object returnValue = context.getReturnValue();
        BotReturnTypeHandler handler;

        if (returnValue instanceof MarkupAware) {
            handler = botReturnTypeHandlerFactory.getReturnTypeHandler(returnValue);
        } else {
            handler = botReturnTypeHandlerFactory.getReturnTypeHandler(context.getMethod());
        }

        log.trace("Dispatching return value type='{}' to handler '{}'",
                returnValue == null ? "null" : returnValue.getClass().getSimpleName(),
                handler.getClass().getSimpleName());

        handler.handleReturnType(context.getRequest(), context.getResponse(), returnValue, context.getMethod());
        chain.proceed(context);
    }
}
