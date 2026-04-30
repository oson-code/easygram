package uz.osoncode.easygram.core.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;
import uz.osoncode.easygram.core.annotation.BotOrder;
import uz.osoncode.easygram.core.bind.annotation.BotForwardChatState;
import uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataDefaultResolver;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolver;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolverFactory;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * {@link ApplicationRunner} that scans all {@link BotController}-annotated beans at
 * application startup and registers their handler methods in the {@link BotHandlerRegistry}.
 *
 * <p>For each bean annotated with {@link BotController}, this loader inspects every public
 * method. Methods carrying a handler annotation recognised by a
 * {@link uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver}
 * are registered as specific handlers; those recognised by a
 * {@link BotMetaDataDefaultResolver} are registered as default (fallback) handlers,
 * <em>unless</em> they carry a non-empty {@link BotChatState} — in that case they are
 * promoted to specific handlers so they are evaluated before any state-agnostic fallback.
 *
 * <p>AOP proxies are handled transparently via {@link AopUtils#getTargetClass} so that
 * the real class's annotations are always visible.
 *
 * <h2>Class-level {@code @BotChatState} inheritance</h2>
 * <p>Analogous to Spring MVC's {@code @RequestMapping}, placing {@link BotChatState} on the
 * controller <em>class</em> defines a default state requirement that is inherited by every
 * handler method in that class. A method-level {@link BotChatState} annotation overrides the
 * class-level default for that specific method only.</p>
 *
 * <h2>Extending handler matching</h2>
 * <p>Register a {@link BotHandlerConditionContributor} bean to add custom conditions to
 * all handler methods without modifying this class. The contributor is called once per
 * method during startup and may return zero or more additional
 * {@link BotHandlerCondition} instances.
 * Handler construction itself is delegated to {@link BotMethodHandlerFactory} — override
 * that bean to customise how {@link BotMethodHandler} instances are created.</p>
 *
 * <pre>{@code
 * // All handlers in this class require "REGISTRATION" state unless overridden per method.
 * @BotController
 * @BotChatState("REGISTRATION")
 * public class RegistrationFlow {
 *
 *     @BotText("name")
 *     public String handleName(@BotTextValue String name) { ... }  // requires REGISTRATION
 *
 *     @BotCommand("/cancel")
 *     @BotChatState   // empty value overrides class-level -> matches ANY state
 *     public String cancel() { ... }
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotMethodHandlerFactory
 * @see DefaultBotMethodHandlerFactory
 */
@Slf4j
@RequiredArgsConstructor
public class BotHandlerLoader implements ApplicationRunner {

    /** Spring application context used to discover {@link BotController} beans. */
    private final ApplicationContext applicationContext;

    /** Registry where discovered handlers are stored. */
    private final BotHandlerRegistry botHandlerRegistry;

    /** Factory providing all registered metadata resolvers. */
    private final BotMetaDataResolverFactory botMetaDataResolverFactory;

    /** Factory that creates {@link BotMethodHandler} instances for each discovered method. */
    private final BotMethodHandlerFactory botMethodHandlerFactory;

    /**
     * Scans {@link BotController} beans and registers their annotated methods as handlers.
     *
     * <p>For each controller bean, all spec resolvers and default resolvers are processed
     * in a single unified pass. The appropriate registry partition is selected based on:</p>
     * <ul>
     *   <li>Whether the resolver is a {@link BotMetaDataSpecResolver} (specific) or
     *       {@link BotMetaDataDefaultResolver} (default/fallback).</li>
     *   <li>Whether the effective {@link BotChatState} restricts to specific states —
     *       such methods are promoted to the spec partition regardless of resolver type.</li>
     * </ul>
     *
     * <p>A shared {@code seenKeys} map is built across all controllers during this scan.
     * If two handler methods share the same routing condition (annotation type + value +
     * effective chat state + tier), startup fails with a {@link BeanCreationException}
     * — analogous to Spring MVC rejecting duplicate {@code @RequestMapping} paths.</p>
     *
     * @param args application arguments (not used)
     */
    @Override
    public void run(ApplicationArguments args) {
        var controllers = applicationContext.getBeansWithAnnotation(BotController.class);
        log.info("Scanning {} @BotController bean(s) for handler methods", controllers.size());

        Map<String, String> seenKeys = new HashMap<>();
        controllers.forEach((beanName, bean) -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            BotChatState classChatState = AnnotationUtils.findAnnotation(targetClass, BotChatState.class);
            log.debug("Processing controller: {} (class={})", beanName, targetClass.getSimpleName());

            processResolvers(botMetaDataResolverFactory.getSpecResolvers(), targetClass, bean, classChatState,
                    false, seenKeys);
            processResolvers(botMetaDataResolverFactory.getDefaultResolvers(), targetClass, bean, classChatState,
                    true, seenKeys);
        });

        log.info("Handler registration complete: stateHandlers={}, specificHandlers={}, defaultHandlers={}",
                botHandlerRegistry.getStateHandlers().size(),
                botHandlerRegistry.getBotHandlers().size(),
                botHandlerRegistry.getDefaultHandlers().size());
    }

    /**
     * Processes a list of metadata resolvers for a single controller bean, scanning its
     * methods and registering matching ones as handlers.
     *
     * <p>Before registering each handler, a conflict key is computed from the handler's
     * tier, annotation type, annotation value(s), and effective chat state. If another
     * handler with the same key was already registered in any controller, startup fails
     * with a {@link BeanCreationException}.</p>
     *
     * @param resolvers        the resolvers to process
     * @param targetClass      the resolved (non-proxy) controller class
     * @param bean             the controller bean instance
     * @param classChatState   the class-level {@link BotChatState}, or {@code null}
     * @param useDefaultSlot   {@code true} for {@link BotMetaDataDefaultResolver}s —
     *                         non-state-restricted methods are registered via
     *                         {@link BotHandlerRegistry#registerDefault};
     *                         {@code false} for spec resolvers, which use
     *                         {@link BotHandlerRegistry#register}
     * @param seenKeys         shared map of already-registered condition keys to handler
     *                         descriptions, used to detect duplicates across all controllers
     */
    private void processResolvers(
            Iterable<? extends BotMetaDataResolver<?>> resolvers,
            Class<?> targetClass,
            Object bean,
            BotChatState classChatState,
            boolean useDefaultSlot,
            Map<String, String> seenKeys) {

        Method[] methods = targetClass.getMethods();
        for (BotMetaDataResolver<?> resolver : resolvers) {
            Arrays.stream(methods)
                    .filter(m -> m.isAnnotationPresent(resolver.getAnnotationType()))
                    .forEach(m -> {
                        BotChatState methodChatState = AnnotationUtils.findAnnotation(m, BotChatState.class);
                        BotChatState effectiveChatState = Objects.nonNull(methodChatState) ? methodChatState : classChatState;
                        boolean hasSpecificState = Objects.nonNull(effectiveChatState) && effectiveChatState.value().length > 0;

                        BotOrder botOrderAnnotation = AnnotationUtils.findAnnotation(m, BotOrder.class);
                        int order = Objects.nonNull(botOrderAnnotation) ? botOrderAnnotation.value() : Integer.MAX_VALUE;

                        BotReplyMarkup replyMarkup = AnnotationUtils.findAnnotation(m, BotReplyMarkup.class);
                        if (Objects.nonNull(replyMarkup) && !StringUtils.hasText(replyMarkup.value())) {
                            throw new BeanCreationException(
                                    "BotHandlerLoader",
                                    "@BotReplyMarkup on method '" + targetClass.getName() + "#" + m.getName()
                                    + "' has a blank value. Provide the name of a registered @BotMarkup.");
                        }

                        BotForwardChatState forwardChatState = AnnotationUtils.findAnnotation(m, BotForwardChatState.class);
                        if (Objects.nonNull(forwardChatState) && !StringUtils.hasText(forwardChatState.value())) {
                            throw new BeanCreationException(
                                    "BotHandlerLoader",
                                    "@BotForwardChatState on method '" + targetClass.getName() + "#" + m.getName()
                                    + "' has a blank value. Provide a non-blank state name.");
                        }

                        // Duplicate condition detection — fail fast, like Spring MVC on duplicate @RequestMapping
                        String tier = hasSpecificState ? "state" : useDefaultSlot ? "default" : "specific";
                        String stateKey = hasSpecificState
                                ? Arrays.stream(effectiveChatState.value()).sorted().collect(Collectors.joining(","))
                                : "";
                        String handlerDesc = targetClass.getName() + "#" + m.getName();
                        Annotation annotation = AnnotationUtils.findAnnotation(m, resolver.getAnnotationType());
                        List<String> values = extractAnnotationValues(annotation);

                        if (values.isEmpty()) {
                            String key = tier + ":" + resolver.getAnnotationType().getSimpleName() + ":*:state:" + stateKey;
                            checkForDuplicate(seenKeys, key, handlerDesc);
                            seenKeys.put(key, handlerDesc);
                        } else {
                            for (String value : values) {
                                String key = tier + ":" + resolver.getAnnotationType().getSimpleName() + ":" + value + ":state:" + stateKey;
                                checkForDuplicate(seenKeys, key, handlerDesc);
                                seenKeys.put(key, handlerDesc);
                            }
                        }

                        Consumer<BotHandler> registrar = hasSpecificState
                                ? botHandlerRegistry::registerState
                                : useDefaultSlot ? botHandlerRegistry::registerDefault : botHandlerRegistry::register;

                        registrar.accept(botMethodHandlerFactory.create(
                                m, bean, resolver, effectiveChatState, order, hasSpecificState));
                    });
        }
    }

    /**
     * Extracts the {@code String[] value()} from a routing annotation via reflection.
     *
     * <p>Most routing annotations ({@code @BotCommand}, {@code @BotText},
     * {@code @BotCallbackQuery}, etc.) declare a {@code String[] value()} element.
     * Annotations without this element (e.g. catch-all types like {@code @BotContact})
     * return an empty list, which causes the caller to treat the handler as a wildcard.</p>
     *
     * @param annotation the annotation instance to inspect; may be {@code null}
     * @return the annotation values, or an empty list if none are available
     */
    private List<String> extractAnnotationValues(Annotation annotation) {
        if (annotation == null) {
            return List.of();
        }
        try {
            Method valueMethod = annotation.annotationType().getMethod("value");
            Object result = valueMethod.invoke(annotation);
            if (result instanceof String[] arr) {
                return Arrays.asList(arr);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            // annotation has no String[] value() — treat as wildcard
        }
        return List.of();
    }

    /**
     * Checks whether a handler condition key has already been registered and throws if so.
     *
     * @param seenKeys   map of previously seen condition keys to their handler descriptions
     * @param key        the condition key for the handler being registered
     * @param newHandler human-readable description of the handler being registered
     * @throws BeanCreationException if {@code key} already maps to a different handler
     */
    private void checkForDuplicate(Map<String, String> seenKeys, String key, String newHandler) {
        String existing = seenKeys.get(key);
        if (existing != null) {
            throw new BeanCreationException(
                    "BotHandlerLoader",
                    "Duplicate handler mapping detected for condition [" + key + "]:\n" +
                    "  First  : " + existing + "\n" +
                    "  Second : " + newHandler + "\n" +
                    "Remove or rename one of the conflicting handler methods.");
        }
    }
}

