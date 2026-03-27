package uz.osoncode.easygram.core.handler.editedmessage.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotEditedMessage;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotEditedMessage} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains an edited message.
 * Handler methods annotated with {@link BotEditedMessage} are invoked when a previously
 * sent message is edited by the user.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotEditedMessageMetaDataResolver implements BotMetaDataSpecResolver<BotEditedMessage> {

    @Override
    public Class<BotEditedMessage> getAnnotationType() {
        return BotEditedMessage.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotEditedMessage annotation) {
        return botRequest.getUpdate().hasEditedMessage();
    }
}
