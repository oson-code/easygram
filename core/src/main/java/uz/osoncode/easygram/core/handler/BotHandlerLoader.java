package uz.osoncode.easygram.core.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import uz.osoncode.easygram.core.annotation.BotOrder;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataDefaultResolver;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolver;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolverFactory;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

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
     * @param args application arguments (not used)
     */
    @Override
    public void run(ApplicationArguments args) {
        var controllers = applicationContext.getBeansWithAnnotation(BotController.class);
        log.info("Scanning {} @BotController bean(s) for handler methods", controllers.size());

        controllers.forEach((beanName, bean) -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            BotChatState classChatState = AnnotationUtils.findAnnotation(targetClass, BotChatState.class);
            log.debug("Processing controller: {} (class={})", beanName, targetClass.getSimpleName());

            processResolvers(botMetaDataResolverFactory.getSpecResolvers(), targetClass, bean, classChatState,
                    false);
            processResolvers(botMetaDataResolverFactory.getDefaultResolvers(), targetClass, bean, classChatState,
                    true);
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
     * @param resolvers        the resolvers to process
     * @param targetClass      the resolved (non-proxy) controller class
     * @param bean             the controller bean instance
     * @param classChatState   the class-level {@link BotChatState}, or {@code null}
     * @param useDefaultSlot   {@code true} for {@link BotMetaDataDefaultResolver}s —
     *                         non-state-restricted methods are registered via
     *                         {@link BotHandlerRegistry#registerDefault};
     *                         {@code false} for spec resolvers, which use
     *                         {@link BotHandlerRegistry#register}
     */
    private void processResolvers(
            Iterable<? extends BotMetaDataResolver<?>> resolvers,
            Class<?> targetClass,
            Object bean,
            BotChatState classChatState,
            boolean useDefaultSlot) {

        for (BotMetaDataResolver<?> resolver : resolvers) {
            Arrays.stream(targetClass.getMethods())
                    .filter(m -> m.isAnnotationPresent(resolver.getAnnotationType()))
                    .forEach(m -> {
                        BotChatState methodChatState = AnnotationUtils.findAnnotation(m, BotChatState.class);
                        BotChatState effectiveChatState = Objects.nonNull(methodChatState) ? methodChatState : classChatState;
                        boolean hasSpecificState = Objects.nonNull(effectiveChatState) && effectiveChatState.value().length > 0;

                        BotOrder botOrderAnnotation = AnnotationUtils.findAnnotation(m, BotOrder.class);
                        int order = Objects.nonNull(botOrderAnnotation) ? botOrderAnnotation.value() : Integer.MAX_VALUE;

                        Consumer<BotHandler> registrar = hasSpecificState
                                ? botHandlerRegistry::registerState
                                : useDefaultSlot ? botHandlerRegistry::registerDefault : botHandlerRegistry::register;

                        registrar.accept(botMethodHandlerFactory.create(
                                m, bean, resolver, effectiveChatState, order, hasSpecificState));
                    });
        }
    }
}

