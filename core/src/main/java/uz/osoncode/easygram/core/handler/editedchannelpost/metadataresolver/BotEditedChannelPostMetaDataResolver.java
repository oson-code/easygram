package uz.osoncode.easygram.core.handler.editedchannelpost.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotEditedChannelPost;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotEditedChannelPost} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains an edited channel post.
 * Handler methods annotated with {@link BotEditedChannelPost} are invoked when a
 * previously sent channel message is edited.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotEditedChannelPostMetaDataResolver implements BotMetaDataSpecResolver<BotEditedChannelPost> {

    @Override
    public Class<BotEditedChannelPost> getAnnotationType() {
        return BotEditedChannelPost.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotEditedChannelPost annotation) {
        return botRequest.getUpdate().hasEditedChannelPost();
    }
}
