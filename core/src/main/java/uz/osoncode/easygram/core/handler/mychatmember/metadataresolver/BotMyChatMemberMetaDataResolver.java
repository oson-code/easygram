package uz.osoncode.easygram.core.handler.mychatmember.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotMyChatMember;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotMyChatMember} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains an update about the
 * bot's own chat member status change. Handler methods annotated with
 * {@link BotMyChatMember} are invoked when the bot is added to, removed from, or has
 * its permissions changed in a chat.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotMyChatMemberMetaDataResolver implements BotMetaDataSpecResolver<BotMyChatMember> {

    @Override
    public Class<BotMyChatMember> getAnnotationType() {
        return BotMyChatMember.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotMyChatMember annotation) {
        return botRequest.getUpdate().hasMyChatMember();
    }
}
