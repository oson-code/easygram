package uz.osoncode.easygram.core.handler.message.text.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotText} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a plain-text message
 * whose content exactly matches one of the values declared in the {@link BotText} annotation.
 * This resolver enables routing of specific text messages (such as menu button labels) to
 * dedicated handler methods.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotTextMetaDataResolver implements BotMetaDataSpecResolver<BotText> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotText} annotation class.
     */
    @Override
    public Class<BotText> getAnnotationType() {
        return BotText.class;
    }

    /**
     * Returns {@code true} if the update contains a text message whose content equals
     * one of the values specified in the {@link BotText} annotation.
     *
     * <p>The comparison is exact and case-sensitive. If the update does not contain a
     * text message, this method returns {@code false} immediately.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotText} annotation declared on the handler method.
     * @return {@code true} if the message text matches any of the annotation's values;
     *         {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotText annotation) {
        if (botRequest.getUpdate().hasMessage() && botRequest.getUpdate().getMessage().hasText()) {
            String messageText = botRequest.getUpdate().getMessage().getText();
            for (String text : annotation.value()) {
                if (messageText.equals(text)) return true;
            }
        }
        return false;
    }
}
