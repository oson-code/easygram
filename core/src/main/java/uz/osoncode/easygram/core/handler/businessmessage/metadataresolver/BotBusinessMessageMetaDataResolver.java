package uz.osoncode.easygram.core.handler.businessmessage.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotBusinessMessage;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotBusinessMessage} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a message sent on behalf
 * of a business account. Handler methods annotated with {@link BotBusinessMessage} are
 * invoked when a connected business account sends a message through the bot.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotBusinessMessageMetaDataResolver implements BotMetaDataSpecResolver<BotBusinessMessage> {

    @Override
    public Class<BotBusinessMessage> getAnnotationType() {
        return BotBusinessMessage.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotBusinessMessage annotation) {
        return botRequest.getUpdate().hasBusinessMessage();
    }
}
