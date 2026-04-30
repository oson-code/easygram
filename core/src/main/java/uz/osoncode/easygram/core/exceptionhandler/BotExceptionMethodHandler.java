package uz.osoncode.easygram.core.exceptionhandler;

import lombok.Getter;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.exception.BotHandlerException;
import uz.osoncode.easygram.core.handler.invocation.BotHandlerInvocationContext;
import uz.osoncode.easygram.core.markup.MarkupAware;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotRequestAttributes;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory;
import uz.osoncode.easygram.core.handler.invocation.MarkupApplicationFilter;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

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
 * <p>When the handler method (or its controller class) is annotated with
 * {@link BotChatState}, the handler is only selected if the current chat is in one of the
 * declared states — identical to how regular handler methods respect {@link BotChatState}.
 *
 * @param <T> the exception type handled by this instance.
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
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
     * Markup application filter used to apply {@code @BotReplyMarkup}, {@code @BotClearMarkup},
     * and state-bound keyboards to the exception handler's return value — identical to how
     * regular handler methods have markup applied via the invocation filter chain.
     */
    private final MarkupApplicationFilter markupApplicationFilter;

    /**
     * Optional chat-state service used to check the current state of the chat.
     * {@code null} when the chat-state module is not present.
     */
    private final BotChatStateService botChatStateService;

    /**
     * The effective {@link BotChatState} annotation (method-level wins over class-level).
     * {@code null} means no state restriction — the handler applies to any state.
     */
    private final BotChatState chatState;

    /**
     * Predicate that restricts this advice handler to specific controller classes.
     * Always {@code true} for controller-local handlers and globally-scoped advice.
     */
    private final Predicate<Class<?>> controllerScopePredicate;

    /**
     * Constructs a new exception method handler with no controller scope restriction (global).
     *
     * @param exceptionType            the exception type this handler is bound to
     * @param priority                 {@code 0} for local, {@code 1} for global advice
     * @param method                   the annotated controller method
     * @param bean                     the controller bean instance
     * @param botArgumentResolverFactory factory for resolving method parameters
     * @param botReturnTypeHandlerFactory factory for processing the return value
     * @param botChatStateService      optional chat-state service; {@code null} if unavailable
     * @param chatState                effective {@link BotChatState}, or {@code null} for no restriction
     * @param markupApplicationFilter  filter for applying markup annotations
     */
    public BotExceptionMethodHandler(Class<T> exceptionType,
                                     int priority,
                                     Method method,
                                     Object bean,
                                     BotArgumentResolverFactory botArgumentResolverFactory,
                                     BotReturnTypeHandlerFactory botReturnTypeHandlerFactory,
                                     BotChatStateService botChatStateService,
                                     BotChatState chatState,
                                     MarkupApplicationFilter markupApplicationFilter) {
        this(exceptionType, priority, method, bean, botArgumentResolverFactory,
             botReturnTypeHandlerFactory, botChatStateService, chatState,
             markupApplicationFilter, c -> true);
    }

    /**
     * Constructs a new exception method handler with a controller scope predicate.
     *
     * @param exceptionType              the exception type this handler is bound to
     * @param priority                   {@code 0} for local, {@code 1} for global advice
     * @param method                     the annotated controller method
     * @param bean                       the controller bean instance
     * @param botArgumentResolverFactory factory for resolving method parameters
     * @param botReturnTypeHandlerFactory factory for processing the return value
     * @param botChatStateService        optional chat-state service; {@code null} if unavailable
     * @param chatState                  effective {@link BotChatState}, or {@code null} for no restriction
     * @param markupApplicationFilter    filter for applying markup annotations
     * @param controllerScopePredicate   predicate that restricts this handler to matching controller classes;
     *                                   use {@code c -> true} for global advice
     */
    public BotExceptionMethodHandler(Class<T> exceptionType,
                                     int priority,
                                     Method method,
                                     Object bean,
                                     BotArgumentResolverFactory botArgumentResolverFactory,
                                     BotReturnTypeHandlerFactory botReturnTypeHandlerFactory,
                                     BotChatStateService botChatStateService,
                                     BotChatState chatState,
                                     MarkupApplicationFilter markupApplicationFilter,
                                     Predicate<Class<?>> controllerScopePredicate) {
        this.exceptionType = exceptionType;
        this.priority = priority;
        this.method = method;
        this.bean = bean;
        this.botArgumentResolverFactory = botArgumentResolverFactory;
        this.botReturnTypeHandlerFactory = botReturnTypeHandlerFactory;
        this.botChatStateService = botChatStateService;
        this.chatState = chatState;
        this.markupApplicationFilter = markupApplicationFilter;
        this.controllerScopePredicate = controllerScopePredicate;
    }

    /**
     * Returns {@code true} if this handler can handle the given exception for the given request.
     *
     * <p>Three conditions must all be satisfied:</p>
     * <ol>
     *   <li>The exception type matches — {@link Class#isAssignableFrom} so subclasses are accepted.</li>
     *   <li>The controller scope matches — if a scope predicate was set via
     *       {@link uz.osoncode.easygram.core.stereotype.BotControllerAdvice} attributes, the
     *       dispatching controller class (stored in the request attribute
     *       {@code "easygram.controllerClass"}) must satisfy the predicate.</li>
     *   <li>The chat-state requirement is satisfied — if a {@link BotChatState} with non-empty
     *       states is declared, the current chat must be in one of those states.</li>
     * </ol>
     *
     * @param exception  the exception to check
     * @param botRequest the current bot request, used to read the active chat state and controller class
     * @return {@code true} if all conditions match
     */
    public boolean supports(Throwable exception, BotRequest botRequest) {
        if (!exceptionType.isAssignableFrom(exception.getClass())) {
            return false;
        }
        if (!matchesControllerScope(botRequest)) {
            return false;
        }
        return matchesChatState(botRequest);
    }

    /**
     * Checks whether the dispatching controller class satisfies this handler's scope predicate.
     * Returns {@code true} when no predicate was set or the controller class is unknown.
     */
    private boolean matchesControllerScope(BotRequest botRequest) {
        Class<?> controllerClass = botRequest.getAttribute(BotRequestAttributes.CONTROLLER_CLASS, Class.class);
        if (controllerClass == null) {
            return true;
        }
        return controllerScopePredicate.test(controllerClass);
    }

    /**
     * Checks whether the current chat state satisfies this handler's {@link BotChatState} requirement.
     *
     * <p>Returns {@code true} (handler allowed) when any of the following hold:</p>
     * <ul>
     *   <li>No {@link BotChatState} annotation is present (no restriction).</li>
     *   <li>The {@link BotChatStateService} is unavailable (module not present).</li>
     *   <li>The declared state set is empty (matches any state).</li>
     * </ul>
     * Returns {@code false} when the chat is absent, the current state is {@code null},
     * or the current state is not in the declared set.
     */
    private boolean matchesChatState(BotRequest botRequest) {
        if (Objects.isNull(chatState) || Objects.isNull(botChatStateService)) {
            return true;
        }
        Set<String> requiredStates = Set.of(chatState.value());
        if (requiredStates.isEmpty()) {
            return true;
        }
        if (Objects.isNull(botRequest.getChat())) {
            return false;
        }
        String currentState = botChatStateService.getState(botRequest.getChat().getId());
        if (Objects.isNull(currentState)) {
            return false;
        }
        return requiredStates.contains(currentState);
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
            Object returnValue = method.invoke(bean, objects);

            // Apply markup annotations (@BotReplyMarkup, @BotClearMarkup, state-bound keyboards)
            // so that exception handler methods participate in the same markup pipeline as regular handlers.
            BotHandlerInvocationContext ctx = new BotHandlerInvocationContext(botRequest, botResponse, method, bean);
            ctx.setReturnValue(returnValue);
            markupApplicationFilter.invoke(ctx, noopCtx -> {});
            returnValue = ctx.getReturnValue();

            if (returnValue instanceof MarkupAware) {
                botReturnTypeHandlerFactory.getReturnTypeHandler(returnValue)
                        .handleReturnType(botRequest, botResponse, returnValue);
            } else {
                botReturnTypeHandlerFactory.getReturnTypeHandler(method)
                        .handleReturnType(botRequest, botResponse, returnValue);
            }
        } catch (Exception e) {
            throw new BotHandlerException("Failed to invoke exception handler method: " + method, e);
        }
    }
}
