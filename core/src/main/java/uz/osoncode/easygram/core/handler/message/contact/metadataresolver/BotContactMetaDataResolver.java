package uz.osoncode.easygram.core.handler.message.contact.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotContact;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotContact} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a message with a shared
 * contact. Handler methods annotated with {@link BotContact} are invoked when a user
 * shares a phone contact with the bot.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotContactMetaDataResolver implements BotMetaDataSpecResolver<BotContact> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotContact} annotation class.
     */
    @Override
    public Class<BotContact> getAnnotationType() {
        return BotContact.class;
    }

    /**
     * Returns {@code true} if the update contains a message with a contact attached.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotContact} annotation declared on the handler method.
     * @return {@code true} if the update message contains a contact; {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotContact annotation) {
        return botRequest.getUpdate().hasMessage() && botRequest.getUpdate().getMessage().hasContact();
    }
}

