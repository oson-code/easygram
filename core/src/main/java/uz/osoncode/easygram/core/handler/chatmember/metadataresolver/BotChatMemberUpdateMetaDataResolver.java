package uz.osoncode.easygram.core.handler.chatmember.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotChatMemberUpdate;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotChatMemberUpdate} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a chat member status
 * change for another user. Handler methods annotated with {@link BotChatMemberUpdate}
 * are invoked when any user's membership status in a chat is updated.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotChatMemberUpdateMetaDataResolver implements BotMetaDataSpecResolver<BotChatMemberUpdate> {

    @Override
    public Class<BotChatMemberUpdate> getAnnotationType() {
        return BotChatMemberUpdate.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotChatMemberUpdate annotation) {
        return botRequest.getUpdate().hasChatMember();
    }
}
