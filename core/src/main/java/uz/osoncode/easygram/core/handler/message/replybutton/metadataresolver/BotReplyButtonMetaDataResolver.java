package uz.osoncode.easygram.core.handler.message.replybutton.metadataresolver;

import lombok.RequiredArgsConstructor;
import uz.osoncode.easygram.core.handler.message.replybutton.BotReplyButtonMatcher;
import uz.osoncode.easygram.core.bind.annotation.BotReplyButton;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotReplyButton} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} is a text message that should be handled
 * by a {@code @BotReplyButton}-annotated method.  The actual matching logic is delegated to the
 * configured {@link BotReplyButtonMatcher} strategy, making the resolver independent of whether
 * the annotation values are exact strings or message-bundle keys.</p>
 *
 * <p>The default matcher (registered by {@code CoreAutoConfiguration} when no other
 * {@link BotReplyButtonMatcher} bean is present) performs an exact case-sensitive text comparison.
 * When {@code core-i18n} is on the classpath it automatically replaces the matcher with a
 * locale-aware implementation.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotReplyButtonMetaDataResolver implements BotMetaDataSpecResolver<BotReplyButton> {

    private final BotReplyButtonMatcher matcher;

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return {@link BotReplyButton}.class
     */
    @Override
    public Class<BotReplyButton> getAnnotationType() {
        return BotReplyButton.class;
    }

    /**
     * Returns {@code true} when the update is a text message and the configured
     * {@link BotReplyButtonMatcher} considers it a match for the annotation values.
     *
     * @param botRequest the current bot request
     * @param annotation the {@link BotReplyButton} annotation on the candidate handler method
     * @return {@code true} if this handler should process the request
     */
    @Override
    public boolean support(BotRequest botRequest, BotReplyButton annotation) {
        if (!botRequest.getUpdate().hasMessage()
                || !botRequest.getUpdate().getMessage().hasText()) {
            return false;
        }
        return matcher.matches(annotation.value(), botRequest);
    }
}
