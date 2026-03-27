package uz.osoncode.easygram.core.handler.deletedbusinessmessages.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotDeletedBusinessMessages;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotDeletedBusinessMessages} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a deleted business
 * messages event. Handler methods annotated with {@link BotDeletedBusinessMessages} are
 * invoked when messages sent by the bot on behalf of a business account are deleted.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotDeletedBusinessMessagesMetaDataResolver implements BotMetaDataSpecResolver<BotDeletedBusinessMessages> {

    @Override
    public Class<BotDeletedBusinessMessages> getAnnotationType() {
        return BotDeletedBusinessMessages.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotDeletedBusinessMessages annotation) {
        return botRequest.getUpdate().hasDeletedBusinessMessage();
    }
}
