package uz.osoncode.easygram.core.exceptionhandler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory;
import uz.osoncode.easygram.core.exception.BotHandlerException;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory;

import java.lang.reflect.Method;

/**
 * Reflective invoker for a method annotated with
 * {@link uz.osoncode.easygram.core.bind.annotation.BotExceptionHandler}.
 *
 * <p>Each instance is bound to a specific exception type {@code T} and a single controller
 * method. When {@link #supports} returns {@code true} for a thrown exception, {@link #handle}
 * stores the throwable on the {@link BotRequest}, resolves the method's parameters via
 * {@link BotArgumentResolverFactory}, invokes the method reflectively, and passes the
 * return value to {@link BotReturnTypeHandlerFactory} for post-processing.
 *
 * <p>Parameter resolution supports injection of the Telegram {@code Update},
 * the raw {@link Throwable}, and the {@link BotRequest} itself, depending on what the
 * handler method declares.
 *
 * @param <T> the exception type handled by this instance.
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotExceptionMethodHandler<T extends Throwable> {

    /** The exact exception type this handler is bound to. */
    @Getter
    private final Class<T> exceptionType;

    /**
     * Handler priority: {@code 0} for controller-local handlers,
     * {@code 1} for global advice handlers. Lower value wins on tie.
     */
    @Getter
    private final int priority;

    /** The controller method to invoke when this handler is selected. */
    private final Method method;

    /** The controller bean instance that owns the method. */
    private final Object bean;

    /** Factory used to resolve the method's parameters from the request and response. */
    private final BotArgumentResolverFactory botArgumentResolverFactory;

    /** Factory used to process the method's return value after invocation. */
    private final BotReturnTypeHandlerFactory botReturnTypeHandlerFactory;

    /**
     * Returns {@code true} if this handler can handle the given exception.
     *
     * <p>Uses {@link Class#isAssignableFrom} so that subclasses of {@code exceptionType}
     * are also accepted.
     *
     * @param exception the exception to check.
     * @return {@code true} if the exception is an instance of {@link #exceptionType} or
     *         a subclass thereof; {@code false} otherwise.
     */
    public boolean supports(Throwable exception) {
        return exceptionType.isAssignableFrom(exception.getClass());
    }

    /**
     * Handles the given exception by invoking the bound controller method reflectively.
     *
     * <p>The throwable is stored on {@code botRequest} via {@link BotRequest#setThrowable}
     * so that argument resolvers can inject it into the method parameters. Method parameters
     * are then resolved via {@link BotArgumentResolverFactory#resolveArguments}, the method
     * is invoked, and the return value is processed by
     * {@link BotReturnTypeHandlerFactory#getReturnTypeHandler}.
     *
     * @param botRequest  the current bot request; the throwable is attached to this object
     *                    before argument resolution.
     * @param botResponse the response object that may be populated by the handler method.
     * @param throwable   the exception that triggered this handler.
     * @throws BotHandlerException if argument resolution, method invocation, or return-type
     *                             handling throws any exception.
     */
    public void handle(BotRequest botRequest, BotResponse botResponse, Throwable throwable) {
        try {
            botRequest.setThrowable(throwable);
            Object[] objects = botArgumentResolverFactory.resolveArguments(method.getParameters(), botRequest, botResponse);
            Object invoke = method.invoke(bean, objects);
            botReturnTypeHandlerFactory.getReturnTypeHandler(method)
                    .handleReturnType(botRequest, botResponse, invoke);
        } catch (Exception e) {
            throw new BotHandlerException("Failed to invoke exception handler method: " + method, e);
        }
    }
}
