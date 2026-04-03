package uz.osoncode.easygram.core.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import uz.osoncode.easygram.core.annotation.BotOrder;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.handler.invocation.BotHandlerInvocationFilter;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolver;
import uz.osoncode.easygram.core.model.BotRequest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Default {@link BotMethodHandlerFactory} implementation that builds
 * {@link BotMethodHandler} instances using the composable
 * {@link BotHandlerCondition} pattern.
 *
 * <p>For each handler method this factory:</p>
 * <ol>
 *   <li>Creates a {@link BotMetaDataCondition} that delegates routing to the
 *       method's metadata resolver.</li>
 *   <li>Creates a {@link BotChatStateCondition} that enforces the effective
 *       {@link BotChatState} (method-level overrides class-level).</li>
 *   <li>Collects any additional {@link BotHandlerCondition}s contributed by
 *       registered {@link BotHandlerConditionContributor} beans.</li>
 *   <li>Composes all conditions into a single {@link Predicate} and wraps them
 *       in a new {@link BotMethodHandler} together with the ordered invocation
 *       filter chain.</li>
 * </ol>
 *
 * <p>Override this bean by declaring a custom {@code BotMethodHandlerFactory} bean
 * annotated with {@code @ConditionalOnMissingBean} in your application context to
 * change how handlers are constructed without modifying this class.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotMethodHandlerFactory
 * @see BotHandlerCondition
 * @see BotHandlerConditionContributor
 * @see BotHandlerInvocationFilter
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultBotMethodHandlerFactory implements BotMethodHandlerFactory {

    /** Optional chat-state service used by {@link BotChatStateCondition}. */
    private final Optional<BotChatStateService> chatStateService;

    /** Ordered invocation filters passed to every {@link BotMethodHandler}. */
    private final List<BotHandlerInvocationFilter> invocationFilters;

    /**
     * Contributors that may add extra {@link BotHandlerCondition}s per method.
     * Consulted once per method at startup; not invoked at request time.
     */
    private final List<BotHandlerConditionContributor> conditionContributors;

    /**
     * Creates a {@link BotMethodHandler} for the given controller method.
     *
     * <p>Extracts the annotation instance from the method using the resolver's
     * annotation type, resolves the handler order from {@link BotOrder}, then builds
     * and composes all conditions into the handler's supports predicate.</p>
     *
     * @param <T>                     the annotation type recognised by the resolver
     * @param method                  the controller method to wrap
     * @param bean                    the controller bean instance
     * @param resolver                the metadata resolver for this annotation type
     * @param effectiveChatState      the effective {@link BotChatState} for this method
     * @param order                   the handler priority
     * @param hasChatStateRestriction {@code true} if the effective state has required values
     * @return a fully constructed {@link BotMethodHandler}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> BotMethodHandler create(
            Method method,
            Object bean,
            BotMetaDataResolver<T> resolver,
            BotChatState effectiveChatState,
            int order,
            boolean hasChatStateRestriction) {

        T annotation = method.getAnnotation(resolver.getAnnotationType());

        List<BotHandlerCondition> conditions = new ArrayList<>();
        conditions.add(new BotMetaDataCondition<>(resolver, annotation));
        conditions.add(new BotChatStateCondition(chatStateService.orElse(null), effectiveChatState));
        for (BotHandlerConditionContributor contributor : conditionContributors) {
            conditions.addAll(contributor.contribute(method, bean));
        }

        Predicate<BotRequest> supportsPredicate = req -> conditions.stream().allMatch(c -> c.matches(req));

        log.debug("Built handler: {}.{}() conditions={} order={} stateRestricted={}",
                bean.getClass().getSimpleName(), method.getName(),
                conditions.size(), order, hasChatStateRestriction);

        return new BotMethodHandler(method, bean, supportsPredicate, invocationFilters, order, hasChatStateRestriction);
    }
}
