package uz.osoncode.easygram.core.handler.message.text.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataDefaultResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotTextDefault} annotation.
 *
 * <p>Acts as a fallback resolver that matches any update containing a plain-text message,
 * regardless of the message content. Handler methods annotated with {@link BotTextDefault}
 * are invoked when no specific {@link uz.osoncode.easygram.core.bind.annotation.BotText}
 * handler matched the incoming text message.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotTextDefaultMetaDataResolver implements BotMetaDataDefaultResolver<BotTextDefault> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotTextDefault} annotation class.
     */
    @Override
    public Class<BotTextDefault> getAnnotationType() {
        return BotTextDefault.class;
    }

    /**
     * Returns {@code true} if the update contains a message with text content of any value.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotTextDefault} annotation declared on the handler method.
     * @return {@code true} if the update contains a text message; {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotTextDefault annotation) {
        return botRequest.getUpdate().hasMessage() && botRequest.getUpdate().getMessage().hasText()
                && !botRequest.getUpdate().getMessage().getText().startsWith("/");
    }
}
