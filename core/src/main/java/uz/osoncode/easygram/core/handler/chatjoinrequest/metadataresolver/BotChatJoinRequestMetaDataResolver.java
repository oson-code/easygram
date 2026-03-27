package uz.osoncode.easygram.core.handler.chatjoinrequest.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotChatJoinRequest;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotChatJoinRequest} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a chat join request.
 * Handler methods annotated with {@link BotChatJoinRequest} are invoked when a user
 * requests to join a chat that requires admin approval.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotChatJoinRequestMetaDataResolver implements BotMetaDataSpecResolver<BotChatJoinRequest> {

    @Override
    public Class<BotChatJoinRequest> getAnnotationType() {
        return BotChatJoinRequest.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotChatJoinRequest annotation) {
        return botRequest.getUpdate().hasChatJoinRequest();
    }
}
