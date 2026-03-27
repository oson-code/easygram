package uz.osoncode.easygram.core.handler.precheckoutquery.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotPreCheckoutQuery;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotPreCheckoutQuery} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a pre-checkout query.
 * Handler methods annotated with {@link BotPreCheckoutQuery} are invoked when a user
 * confirms checkout details before a payment is finalised.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPreCheckoutQueryMetaDataResolver implements BotMetaDataSpecResolver<BotPreCheckoutQuery> {

    @Override
    public Class<BotPreCheckoutQuery> getAnnotationType() {
        return BotPreCheckoutQuery.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotPreCheckoutQuery annotation) {
        return botRequest.getUpdate().hasPreCheckoutQuery();
    }
}
