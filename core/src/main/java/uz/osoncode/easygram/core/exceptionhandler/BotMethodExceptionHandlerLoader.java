package uz.osoncode.easygram.core.exceptionhandler;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.stereotype.BotControllerAdvice;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory;
import uz.osoncode.easygram.core.bind.annotation.BotExceptionHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory;

import java.util.Arrays;

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
     * Scans {@link BotController} beans and registers their {@link BotExceptionHandler}
     * methods as exception handlers.
     *
     * <p>Iterates over every bean annotated with {@link BotController}, resolves the
     * underlying target class (bypassing AOP proxies), and then registers a
     * {@link BotExceptionMethodHandler} for each exception type listed in each
     * {@link BotExceptionHandler}-annotated method.
     *
     * @param args application arguments (not used).
     */
    @Override
    public void run(ApplicationArguments args) {
        applicationContext.getBeansWithAnnotation(BotController.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            Arrays.stream(targetClass.getMethods())
                    .filter(method -> method.isAnnotationPresent(BotExceptionHandler.class))
                    .forEach(method -> {
                        BotExceptionHandler botExceptionHandler = method.getAnnotation(BotExceptionHandler.class);
                        for (Class<? extends Throwable> aClass : botExceptionHandler.value()) {
                            botExceptionHandlerRegistry.register(new BotExceptionMethodHandler<>(
                                    aClass, 0, method, bean, botArgumentResolverFactory, botReturnTypeHandlerFactory));
                        }
                    });
        });

        applicationContext.getBeansWithAnnotation(BotControllerAdvice.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            Arrays.stream(targetClass.getMethods())
                    .filter(method -> method.isAnnotationPresent(BotExceptionHandler.class))
                    .forEach(method -> {
                        BotExceptionHandler botExceptionHandler = method.getAnnotation(BotExceptionHandler.class);
                        for (Class<? extends Throwable> aClass : botExceptionHandler.value()) {
                            botExceptionHandlerRegistry.register(new BotExceptionMethodHandler<>(
                                    aClass, 1, method, bean, botArgumentResolverFactory, botReturnTypeHandlerFactory));
                        }
                    });
        });
    }
}
