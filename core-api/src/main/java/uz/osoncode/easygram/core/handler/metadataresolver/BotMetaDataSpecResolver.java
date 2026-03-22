package uz.osoncode.easygram.core.handler.metadataresolver;

import java.lang.annotation.Annotation;

/**
 * Marker interface for specific (non-default) metadata resolvers.
 * Implementations of this interface are associated with handler methods that carry explicit
 * matching criteria (e.g., {@link uz.osoncode.easygram.core.bind.annotation.BotCommand}
 * with a specific command value).
 * Such methods are registered in the main handler registry and ordered according to
 * {@link uz.osoncode.easygram.core.annotation.BotOrder}.
 *
 * @param <T> the annotation type this resolver handles; must extend {@link Annotation}
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotMetaDataSpecResolver<T extends Annotation> extends BotMetaDataResolver<T> {
}
