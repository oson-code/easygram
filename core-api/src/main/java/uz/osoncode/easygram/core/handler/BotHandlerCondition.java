package uz.osoncode.easygram.core.handler;

import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Functional condition that decides whether a bot handler should be considered for a
 * given {@link BotRequest}.
 *
 * <p>Handler conditions compose the supports-predicate of a
 * {@link uz.osoncode.easygram.core.handler.BotHandler}: each registered condition must
 * return {@code true} for the handler to be eligible to process an update. If any condition
 * returns {@code false}, the handler is skipped.</p>
 *
 * <p>The framework provides two built-in conditions:</p>
 * <ul>
 *   <li>{@code BotMetaDataCondition} — delegates to the annotation-specific metadata resolver
 *       (e.g. checks that the update is a {@code /start} command)</li>
 *   <li>{@code BotChatStateCondition} — guards the handler behind a required chat state
 *       (sourced from {@code @BotChatState})</li>
 * </ul>
 *
 * <p>Custom conditions can be contributed globally for all handlers via
 * {@link BotHandlerConditionContributor}, or built ad-hoc and passed directly to a
 * {@code BotMethodHandler}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerConditionContributor
 */
@FunctionalInterface
public interface BotHandlerCondition {

    /**
     * Returns {@code true} if this condition is satisfied for the given request.
     *
     * @param botRequest the current bot request; must not be {@code null}
     * @return {@code true} if the handler should be considered, {@code false} to skip it
     */
    boolean matches(BotRequest botRequest);
}
