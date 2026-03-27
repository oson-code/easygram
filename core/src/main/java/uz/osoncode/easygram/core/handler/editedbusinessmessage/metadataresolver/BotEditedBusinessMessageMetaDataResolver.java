package uz.osoncode.easygram.core.handler.editedbusinessmessage.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotEditedBusinessMessage;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotEditedBusinessMessage} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains an edited message sent
 * on behalf of a business account. Handler methods annotated with
 * {@link BotEditedBusinessMessage} are invoked when a previously sent business message
 * is edited.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotEditedBusinessMessageMetaDataResolver implements BotMetaDataSpecResolver<BotEditedBusinessMessage> {

    @Override
    public Class<BotEditedBusinessMessage> getAnnotationType() {
        return BotEditedBusinessMessage.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotEditedBusinessMessage annotation) {
        return botRequest.getUpdate().hasEditedBusinessMessage();
    }
}
