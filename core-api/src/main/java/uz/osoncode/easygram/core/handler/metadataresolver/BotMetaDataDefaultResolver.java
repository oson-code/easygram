package uz.osoncode.easygram.core.handler.metadataresolver;

import java.lang.annotation.Annotation;

/**
 * Marker interface for default/fallback metadata resolvers.
 * Implementations are associated with handler methods that act as catch-all handlers
 * (e.g., {@link uz.osoncode.easygram.core.bind.annotation.BotDefaultCommand})
 * and are consulted only when no specific resolver declared via
 * {@link BotMetaDataSpecResolver} matches the current request.
 *
 * @param <T> the annotation type this resolver handles; must extend {@link Annotation}
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotMetaDataDefaultResolver<T extends Annotation> extends BotMetaDataResolver<T> {
}
