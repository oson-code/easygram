package uz.osoncode.easygram.core.handler.paidmediapurchased.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotPaidMediaPurchased;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotPaidMediaPurchased} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a paid media purchase
 * event. Handler methods annotated with {@link BotPaidMediaPurchased} are invoked when
 * a user purchases paid media sent by the bot.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPaidMediaPurchasedMetaDataResolver implements BotMetaDataSpecResolver<BotPaidMediaPurchased> {

    @Override
    public Class<BotPaidMediaPurchased> getAnnotationType() {
        return BotPaidMediaPurchased.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotPaidMediaPurchased annotation) {
        return botRequest.getUpdate().hasPaidMediaPurchased();
    }
}
