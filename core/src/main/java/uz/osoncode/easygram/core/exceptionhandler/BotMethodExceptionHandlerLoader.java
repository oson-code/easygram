package uz.osoncode.easygram.core.exceptionhandler;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.stereotype.BotControllerAdvice;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory;
import uz.osoncode.easygram.core.bind.annotation.BotExceptionHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@link ApplicationRunner} that scans all {@link BotController}-annotated beans at
 * application startup and registers their {@link BotExceptionHandler}-annotated methods
 * in the {@link BotExceptionHandlerRegistry}.
 *
 * <p>For each {@link BotController} bean, this loader inspects every public method. Methods
 * annotated with {@link BotExceptionHandler} are wrapped in a
 * {@link BotExceptionMethodHandler} for each exception type declared in the annotation's
 * {@code value()} array, and the resulting handlers are registered in the
 * {@link BotExceptionHandlerRegistry}.
 *
 * <p>AOP proxies are handled transparently via {@link AopUtils#getTargetClass} so that
 * annotations on the real class are always visible.
 *
 * <h2>Chat-state scoping</h2>
 * <p>When a {@link BotExceptionHandler} method (or its enclosing {@link BotController}
 * class) is annotated with {@link BotChatState}, the handler is only selected if the
 * current chat is in one of the declared states at the time the exception occurs.
 * Method-level {@link BotChatState} overrides the class-level annotation, consistent with
 * the behavior of regular handler methods.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotMethodExceptionHandlerLoader implements ApplicationRunner {

    /** Spring application context used to discover {@link BotController} beans. */
    private final ApplicationContext applicationContext;

    /** Factory for resolving method arguments at exception-handler invocation time. */
    private final BotArgumentResolverFactory botArgumentResolverFactory;

    /** Registry where discovered exception handlers are stored. */
    private final BotExceptionHandlerRegistry botExceptionHandlerRegistry;

    /** Factory for processing exception-handler method return values. */
    private final BotReturnTypeHandlerFactory botReturnTypeHandlerFactory;

    /**
     * Optional chat-state service used to enforce {@link BotChatState} restrictions on
     * exception handlers. When absent (module not on classpath), state restrictions are ignored.
     */
    private final Optional<BotChatStateService> botChatStateService;

    /**
     * Scans {@link BotController} and {@link BotControllerAdvice} beans and registers
     * their {@link BotExceptionHandler} methods, respecting any {@link BotChatState} declared
     * at method or class level.
     *
     * @param args application arguments (not used).
     */
    @Override
    public void run(ApplicationArguments args) {
        BotChatStateService stateService = botChatStateService.orElse(null);

        applicationContext.getBeansWithAnnotation(BotController.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            BotChatState classChatState = AnnotationUtils.findAnnotation(targetClass, BotChatState.class);
            Arrays.stream(targetClass.getMethods())
                    .filter(method -> method.isAnnotationPresent(BotExceptionHandler.class))
                    .forEach(method -> {
                        BotChatState methodChatState = AnnotationUtils.findAnnotation(method, BotChatState.class);
                        BotChatState effectiveChatState = methodChatState != null ? methodChatState : classChatState;
                        BotExceptionHandler botExceptionHandler = method.getAnnotation(BotExceptionHandler.class);
                        for (Class<? extends Throwable> aClass : botExceptionHandler.value()) {
                            botExceptionHandlerRegistry.register(new BotExceptionMethodHandler<>(
                                    aClass, 0, method, bean,
                                    botArgumentResolverFactory, botReturnTypeHandlerFactory,
                                    stateService, effectiveChatState));
                        }
                    });
        });

        applicationContext.getBeansWithAnnotation(BotControllerAdvice.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            BotChatState classChatState = AnnotationUtils.findAnnotation(targetClass, BotChatState.class);
            Arrays.stream(targetClass.getMethods())
                    .filter(method -> method.isAnnotationPresent(BotExceptionHandler.class))
                    .forEach(method -> {
                        BotChatState methodChatState = AnnotationUtils.findAnnotation(method, BotChatState.class);
                        BotChatState effectiveChatState = methodChatState != null ? methodChatState : classChatState;
                        BotExceptionHandler botExceptionHandler = method.getAnnotation(BotExceptionHandler.class);
                        for (Class<? extends Throwable> aClass : botExceptionHandler.value()) {
                            botExceptionHandlerRegistry.register(new BotExceptionMethodHandler<>(
                                    aClass, 1, method, bean,
                                    botArgumentResolverFactory, botReturnTypeHandlerFactory,
                                    stateService, effectiveChatState));
                        }
                    });
        });
    }
}
