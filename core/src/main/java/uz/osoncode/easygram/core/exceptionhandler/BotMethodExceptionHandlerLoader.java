package uz.osoncode.easygram.core.exceptionhandler;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
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
import uz.osoncode.easygram.core.handler.invocation.MarkupApplicationFilter;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
     * Markup application filter injected into every {@link BotExceptionMethodHandler} so that
     * {@code @BotReplyMarkup}, {@code @BotClearMarkup}, and state-bound keyboards are applied
     * to exception-handler return values exactly as they are for regular handler methods.
     */
    private final MarkupApplicationFilter markupApplicationFilter;

    /**
     * Scans {@link BotController} and {@link BotControllerAdvice} beans and registers
     * their {@link BotExceptionHandler} methods, respecting any {@link BotChatState} declared
     * at method or class level.
     *
     * <p>Duplicate detection is performed within each priority group: if two
     * {@link BotController} methods (or two {@link BotControllerAdvice} methods) declare
     * {@link BotExceptionHandler} for the same exception type and the same chat-state
     * condition, a {@link BeanCreationException} is thrown immediately to prevent ambiguous
     * runtime behaviour. A controller handler and an advice handler for the same exception
     * type are in separate priority groups and are therefore allowed to coexist.</p>
     *
     * @param args application arguments (not used).
     * @throws BeanCreationException if a duplicate exception-handler condition is detected.
     */
    @Override
    public void run(ApplicationArguments args) {
        BotChatStateService stateService = botChatStateService.orElse(null);

        Map<String, String> controllerSeenKeys = new HashMap<>();
        applicationContext.getBeansWithAnnotation(BotController.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            BotChatState classChatState = AnnotationUtils.findAnnotation(targetClass, BotChatState.class);
            Arrays.stream(targetClass.getMethods())
                    .filter(method -> method.isAnnotationPresent(BotExceptionHandler.class))
                    .forEach(method -> {
                        BotChatState methodChatState = AnnotationUtils.findAnnotation(method, BotChatState.class);
                        BotChatState effectiveChatState = Objects.nonNull(methodChatState) ? methodChatState : classChatState;
                        BotExceptionHandler botExceptionHandler = method.getAnnotation(BotExceptionHandler.class);
                        String handlerDesc = targetClass.getSimpleName() + "#" + method.getName();
                        for (Class<? extends Throwable> aClass : botExceptionHandler.value()) {
                            String key = buildExceptionHandlerKey(aClass, effectiveChatState);
                            checkForDuplicateExceptionHandler(controllerSeenKeys, key, handlerDesc);
                            controllerSeenKeys.put(key, handlerDesc);
                            botExceptionHandlerRegistry.register(new BotExceptionMethodHandler<>(
                                    aClass, 0, method, bean,
                                    botArgumentResolverFactory, botReturnTypeHandlerFactory,
                                    stateService, effectiveChatState, markupApplicationFilter));
                        }
                    });
        });

        Map<String, String> adviceSeenKeys = new HashMap<>();
        applicationContext.getBeansWithAnnotation(BotControllerAdvice.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            BotControllerAdvice advice = AnnotationUtils.findAnnotation(targetClass, BotControllerAdvice.class);
            Predicate<Class<?>> scopePredicate = buildScopePredicate(advice);
            BotChatState classChatState = AnnotationUtils.findAnnotation(targetClass, BotChatState.class);
            Arrays.stream(targetClass.getMethods())
                    .filter(method -> method.isAnnotationPresent(BotExceptionHandler.class))
                    .forEach(method -> {
                        BotChatState methodChatState = AnnotationUtils.findAnnotation(method, BotChatState.class);
                        BotChatState effectiveChatState = Objects.nonNull(methodChatState) ? methodChatState : classChatState;
                        BotExceptionHandler botExceptionHandler = method.getAnnotation(BotExceptionHandler.class);
                        String handlerDesc = targetClass.getSimpleName() + "#" + method.getName();
                        for (Class<? extends Throwable> aClass : botExceptionHandler.value()) {
                            String key = buildExceptionHandlerKey(aClass, effectiveChatState);
                            checkForDuplicateExceptionHandler(adviceSeenKeys, key, handlerDesc);
                            adviceSeenKeys.put(key, handlerDesc);
                            botExceptionHandlerRegistry.register(new BotExceptionMethodHandler<>(
                                    aClass, 1, method, bean,
                                    botArgumentResolverFactory, botReturnTypeHandlerFactory,
                                    stateService, effectiveChatState, markupApplicationFilter, scopePredicate));
                        }
                    });
        });
    }

    /**
     * Builds a deduplication key for an exception handler registration.
     *
     * <p>The key encodes the exception type and the effective chat-state constraint so that
     * two handlers for the same exception type but different chat states are treated as
     * distinct registrations and do not trigger a duplicate error.</p>
     *
     * @param exceptionType  the exception class handled by the method
     * @param chatState      the effective {@link BotChatState} annotation, or {@code null}
     * @return a string key unique to this exception-type + state combination
     */
    private static String buildExceptionHandlerKey(Class<? extends Throwable> exceptionType,
                                                   BotChatState chatState) {
        String stateKey = chatState != null && chatState.value().length > 0
                ? Arrays.stream(chatState.value()).sorted().collect(Collectors.joining(","))
                : "";
        return exceptionType.getName() + ":state:" + stateKey;
    }

    /**
     * Checks whether an exception handler condition key has already been registered and
     * throws a {@link BeanCreationException} if so.
     *
     * <p>Two maps are maintained — one per {@link BotController} group and one per
     * {@link BotControllerAdvice} group — so that a controller method intentionally
     * overriding an advice handler for the same exception type is not flagged as a
     * duplicate.</p>
     *
     * @param seenKeys   map of previously seen condition keys to their handler descriptions
     * @param key        the condition key for the handler being registered
     * @param newHandler human-readable description of the handler being registered
     *                   (e.g. {@code "MyController#onError"})
     * @throws BeanCreationException if {@code key} is already present in {@code seenKeys}
     */
    private static void checkForDuplicateExceptionHandler(Map<String, String> seenKeys,
                                                          String key, String newHandler) {
        String existing = seenKeys.get(key);
        if (existing != null) {
            throw new BeanCreationException(
                    "BotMethodExceptionHandlerLoader",
                    "Duplicate @BotExceptionHandler mapping detected for condition [" + key + "]:\n" +
                    "  First  : " + existing + "\n" +
                    "  Second : " + newHandler + "\n" +
                    "Remove or rename one of the conflicting exception handler methods.");
        }
    }

    /**
     *
     * <p>Returns an always-true predicate when all scoping arrays are empty (global advice).
     * When multiple scoping criteria are set, a controller class must satisfy at least one
     * ({@code OR} semantics, matching Spring MVC's {@code @ControllerAdvice} behaviour).</p>
     *
     * @param advice the annotation instance; may be {@code null} (returns always-true)
     * @return a predicate that returns {@code true} for matching controller classes
     */
    private Predicate<Class<?>> buildScopePredicate(BotControllerAdvice advice) {
        if (advice == null) {
            return c -> true;
        }
        String[] basePackages = advice.basePackages();
        Class<?>[] assignableTypes = advice.assignableTypes();
        Class<? extends Annotation>[] annotations = advice.annotations();

        boolean hasScope = basePackages.length > 0 || assignableTypes.length > 0 || annotations.length > 0;
        if (!hasScope) {
            return c -> true;
        }

        Predicate<Class<?>> predicate = c -> false;

        if (basePackages.length > 0) {
            Set<String> packages = Set.of(basePackages);
            Predicate<Class<?>> pkgPredicate = c -> {
                String pkg = c.getPackageName();
                return packages.stream().anyMatch(pkg::startsWith);
            };
            predicate = predicate.or(pkgPredicate);
        }

        if (assignableTypes.length > 0) {
            Predicate<Class<?>> typePredicate = c ->
                    Arrays.stream(assignableTypes).anyMatch(t -> t.isAssignableFrom(c));
            predicate = predicate.or(typePredicate);
        }

        if (annotations.length > 0) {
            Predicate<Class<?>> annPredicate = c ->
                    Arrays.stream(annotations).anyMatch(a -> AnnotationUtils.findAnnotation(c, a) != null);
            predicate = predicate.or(annPredicate);
        }

        return predicate;
    }
}
