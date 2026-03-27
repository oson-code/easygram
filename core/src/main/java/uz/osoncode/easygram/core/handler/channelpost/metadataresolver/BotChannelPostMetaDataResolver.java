package uz.osoncode.easygram.core.handler.channelpost.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotChannelPost;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotChannelPost} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a channel post.
 * Handler methods annotated with {@link BotChannelPost} are invoked when a new message
 * is sent in a channel where the bot is a member.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotChannelPostMetaDataResolver implements BotMetaDataSpecResolver<BotChannelPost> {

    @Override
    public Class<BotChannelPost> getAnnotationType() {
        return BotChannelPost.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotChannelPost annotation) {
        return botRequest.getUpdate().hasChannelPost();
    }
}
