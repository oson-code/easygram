package uz.osoncode.easygram.core.handler.invocation;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Mutable context object passed through the {@link BotHandlerInvocationFilter} chain
 * during the execution of a single bot handler method.
 *
 * <p>The context carries all state needed to process one invocation:</p>
 * <ul>
 *   <li>{@link #getRequest()} and {@link #getResponse()} — the incoming Telegram update and the
 *       mutable response being built</li>
 *   <li>{@link #getMethod()} and {@link #getBean()} — the reflective method and its owning
 *       controller instance</li>
 *   <li>{@link #getReturnValue()} / {@link #setReturnValue(Object)} — the value produced by the
 *       handler method; set by {@code MethodInvocationFilter} and read/transformed by subsequent
 *       filters (e.g. markup application, return-type dispatch)</li>
 * </ul>
 *
 * <p>{@code returnValue} is the only mutable field. All other fields are set at construction
 * time and remain constant for the lifetime of one invocation chain execution.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerInvocationFilter
 * @see BotHandlerInvocationChain
 */
public class BotHandlerInvocationContext {

    /** The current Telegram update wrapped in framework context. */
    private final BotRequest request;

    /** The mutable response object being populated during this invocation. */
    private final BotResponse response;

    /** The controller method being invoked. */
    private final Method method;

    /** The controller bean instance that owns the method. */
    private final Object bean;

    /**
     * The value returned by the handler method after reflective invocation.
     * Initially {@code null}; set by {@code MethodInvocationFilter}.
     */
    private Object returnValue;

    /**
     * Creates a new invocation context.
     *
     * @param request  the current bot request; must not be {@code null}
     * @param response the mutable bot response; must not be {@code null}
     * @param method   the handler method to be invoked; must not be {@code null}
     * @param bean     the controller bean that owns the method; must not be {@code null}
     */
    public BotHandlerInvocationContext(BotRequest request, BotResponse response,
                                       Method method, Object bean) {
        this.request = Objects.requireNonNull(request, "request must not be null");
        this.response = Objects.requireNonNull(response, "response must not be null");
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.bean = Objects.requireNonNull(bean, "bean must not be null");
    }

    /**
     * Returns the current bot request.
     *
     * @return the bot request; never {@code null}
     */
    public BotRequest getRequest() {
        return request;
    }

    /**
     * Returns the mutable bot response being populated during this invocation.
     *
     * @return the bot response; never {@code null}
     */
    public BotResponse getResponse() {
        return response;
    }

    /**
     * Returns the controller method being invoked.
     *
     * @return the handler method; never {@code null}
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns the controller bean that owns the handler method.
     *
     * @return the controller bean; never {@code null}
     */
    public Object getBean() {
        return bean;
    }

    /**
     * Returns the value produced by the handler method after invocation.
     *
     * <p>This is {@code null} before {@code MethodInvocationFilter} sets it, and may
     * remain {@code null} for {@code void}-returning methods.</p>
     *
     * @return the return value, or {@code null} if not yet set or if the method returns void
     */
    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * Sets the return value, allowing subsequent filters to transform it.
     *
     * <p>Typically called by {@code MethodInvocationFilter} after reflective invocation,
     * and may be updated by {@code MarkupApplicationFilter} to transform markup-aware types.</p>
     *
     * @param returnValue the new return value; may be {@code null}
     */
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }
}
