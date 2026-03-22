package uz.osoncode.easygram.core.handler.metadataresolver;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Factory that separates bot metadata resolvers into two distinct categories:
 * specific resolvers and default (fallback) resolvers.
 *
 * <p>{@link BotMetaDataSpecResolver} instances handle updates that match a concrete,
 * annotation-driven condition (e.g. a particular command or callback pattern).
 * {@link BotMetaDataDefaultResolver} instances act as catch-all fallbacks when no
 * specific resolver claims the incoming update.</p>
 *
 * <p>Both lists are injected at construction time via the Lombok-generated
 * {@code @RequiredArgsConstructor} and exposed through Lombok-generated getters.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotMetaDataResolverFactory {

    /** Specific resolvers that match updates based on precise annotation-driven conditions. */
    @Getter
    private final List<BotMetaDataSpecResolver<? extends Annotation>> specResolvers;

    /** Default (fallback) resolvers that handle updates not claimed by any specific resolver. */
    @Getter
    private final List<BotMetaDataDefaultResolver<? extends Annotation>> defaultResolvers;

}
