package uz.osoncode.easygram.core.handler.choseninlineresult.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotChosenInlineResult;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotChosenInlineResult} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a chosen inline result.
 * Handler methods annotated with {@link BotChosenInlineResult} are invoked when a user
 * selects a result from an inline query answer.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotChosenInlineResultMetaDataResolver implements BotMetaDataSpecResolver<BotChosenInlineResult> {

    @Override
    public Class<BotChosenInlineResult> getAnnotationType() {
        return BotChosenInlineResult.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotChosenInlineResult annotation) {
        return botRequest.getUpdate().hasChosenInlineQuery();
    }
}
