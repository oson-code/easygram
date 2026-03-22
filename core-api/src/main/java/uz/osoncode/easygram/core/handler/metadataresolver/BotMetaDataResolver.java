package uz.osoncode.easygram.core.handler.metadataresolver;

import uz.osoncode.easygram.core.model.BotRequest;

import java.lang.annotation.Annotation;

/**
 * Core generic interface for matching a specific handler annotation against an incoming {@link BotRequest}.
 * Each implementation is bound to a concrete annotation type {@code T} and encodes the logic
 * for deciding whether the annotated handler method should be invoked for the current request.
 *
 * @param <T> the annotation type this resolver handles; must extend {@link Annotation}
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotMetaDataResolver<T extends Annotation> {

    /**
     * Returns the annotation type that this resolver is responsible for evaluating.
     *
     * @return the {@link Class} object representing the annotation type {@code T}
     */
    Class<T> getAnnotationType();

    /**
     * Evaluates whether the given annotation instance indicates that the annotated handler
     * method should handle the provided request.
     *
     * @param botRequest the current request context containing the incoming update; must not be {@code null}
     * @param annotation the annotation instance found on the handler method; must not be {@code null}
     * @return {@code true} if the handler method should be invoked for this request, {@code false} otherwise
     */
    boolean support(BotRequest botRequest, T annotation);
}
