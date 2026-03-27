package uz.osoncode.easygram.core.handler.poll.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotPoll;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotPoll} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a poll state update.
 * Handler methods annotated with {@link BotPoll} are invoked when a poll is stopped
 * or its state otherwise changes.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPollMetaDataResolver implements BotMetaDataSpecResolver<BotPoll> {

    @Override
    public Class<BotPoll> getAnnotationType() {
        return BotPoll.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotPoll annotation) {
        return botRequest.getUpdate().hasPoll();
    }
}
