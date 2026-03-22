package uz.osoncode.easygram.core.handler;

import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Factory SPI for creating {@link BotMethodHandler} instances during handler registration.
 *
 * <p>This interface separates <em>handler construction</em> from <em>scanning and registration
 * orchestration</em> performed by {@link BotHandlerLoader}. The default implementation,
 * {@link DefaultBotMethodHandlerFactory}, builds the composable
 * {@link BotHandlerCondition} list and wires the invocation filter chain. Library consumers
 * can override handler creation by declaring a custom {@code BotMethodHandlerFactory} bean.</p>
 *
 * <h2>Extension example</h2>
 * <pre>{@code
 * @Bean
 * public BotMethodHandlerFactory myHandlerFactory(
 *         List<BotHandlerInvocationFilter> filters,
 *         List<BotHandlerConditionContributor> contributors,
 *         Optional<BotChatStateService> stateService) {
 *     return new MyCustomHandlerFactory(filters, contributors, stateService);
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see DefaultBotMethodHandlerFactory
 * @see BotHandlerLoader
 */
public interface BotMethodHandlerFactory {

    /**
     * Creates a {@link BotMethodHandler} for the given controller method.
     *
     * <p>Implementations are responsible for building the supports predicate (typically
     * from {@link BotHandlerCondition} objects), wiring the invocation filter chain, and
     * constructing the {@link BotMethodHandler}. The returned handler is ready to be
     * registered directly in {@link BotHandlerRegistry}.</p>
     *
     * @param <T>                     the annotation type recognised by the resolver
     * @param method                  the controller method to wrap
     * @param bean                    the controller bean instance that owns the method
     * @param resolver                the metadata resolver for this annotation type
     * @param effectiveChatState      the resolved {@link BotChatState} (method-level if present,
     *                                otherwise class-level, or {@code null} if absent)
     * @param order                   the handler priority — lower values are matched first;
     *                                typically from {@link uz.osoncode.easygram.core.annotation.BotOrder}
     *                                or {@link Integer#MAX_VALUE} as the default
     * @param hasChatStateRestriction {@code true} if {@code effectiveChatState} defines a
     *                                non-empty set of required states; used as a tie-breaker
     *                                in handler ordering
     * @return a fully constructed, ready-to-register {@link BotMethodHandler}
     */
    <T extends Annotation> BotMethodHandler create(
            Method method,
            Object bean,
            BotMetaDataResolver<T> resolver,
            BotChatState effectiveChatState,
            int order,
            boolean hasChatStateRestriction);
}
