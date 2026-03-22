package uz.osoncode.easygram.core.handler.message.location.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotLocation;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotLocation} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a message with a shared
 * geographic location. Handler methods annotated with {@link BotLocation} are invoked
 * when a user sends a location to the bot.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotLocationMetaDataResolver implements BotMetaDataSpecResolver<BotLocation> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotLocation} annotation class.
     */
    @Override
    public Class<BotLocation> getAnnotationType() {
        return BotLocation.class;
    }

    /**
     * Returns {@code true} if the update contains a message with a location attached.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotLocation} annotation declared on the handler method.
     * @return {@code true} if the update message contains a location; {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotLocation annotation) {
        return botRequest.getUpdate().hasMessage() && botRequest.getUpdate().getMessage().hasLocation();
    }
}

