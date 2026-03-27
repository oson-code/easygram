package uz.osoncode.easygram.core.handler.pollanswer.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotPollAnswer;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotPollAnswer} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a poll answer event.
 * Handler methods annotated with {@link BotPollAnswer} are invoked when a user changes
 * their answer in a non-anonymous poll.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPollAnswerMetaDataResolver implements BotMetaDataSpecResolver<BotPollAnswer> {

    @Override
    public Class<BotPollAnswer> getAnnotationType() {
        return BotPollAnswer.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotPollAnswer annotation) {
        return botRequest.getUpdate().hasPollAnswer();
    }
}
