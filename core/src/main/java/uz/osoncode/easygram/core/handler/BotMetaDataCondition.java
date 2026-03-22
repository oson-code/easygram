package uz.osoncode.easygram.core.handler;

import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolver;
import uz.osoncode.easygram.core.model.BotRequest;

import java.lang.annotation.Annotation;

/**
 * {@link BotHandlerCondition} that delegates to a {@link BotMetaDataResolver} to decide
 * whether a handler method should handle the current request.
 *
 * <p>This condition wraps the per-annotation matching logic implemented by each
 * {@code BotMetaDataResolver} (e.g. checking that the update is a specific command,
 * a text message, a callback query, etc.) into the composable condition abstraction.</p>
 *
 * <p>An instance is created for each discovered handler method during the startup scan
 * in {@code BotHandlerLoader}, bound to the annotation instance found on that method.</p>
 *
 * @param <T> the annotation type handled by the resolver
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerCondition
 * @see BotMetaDataResolver
 */
public class BotMetaDataCondition<T extends Annotation> implements BotHandlerCondition {

    private final BotMetaDataResolver<T> resolver;
    private final T annotation;

    /**
     * Creates a new condition bound to the given resolver and annotation instance.
     *
     * @param resolver   the metadata resolver to delegate to; must not be {@code null}
     * @param annotation the annotation instance found on the handler method; must not be {@code null}
     */
    public BotMetaDataCondition(BotMetaDataResolver<T> resolver, T annotation) {
        this.resolver = resolver;
        this.annotation = annotation;
    }

    /**
     * Returns {@code true} if the resolver accepts the request for the bound annotation.
     *
     * @param botRequest the current bot request; must not be {@code null}
     * @return {@code true} if the handler should be considered for this update
     */
    @Override
    public boolean matches(BotRequest botRequest) {
        return resolver.support(botRequest, annotation);
    }
}
