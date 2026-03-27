package uz.osoncode.easygram.core.handler.shippingquery.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotShippingQuery;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotShippingQuery} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a shipping query.
 * Handler methods annotated with {@link BotShippingQuery} are invoked when a user
 * confirms their delivery address for a payment that requires shipping.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotShippingQueryMetaDataResolver implements BotMetaDataSpecResolver<BotShippingQuery> {

    @Override
    public Class<BotShippingQuery> getAnnotationType() {
        return BotShippingQuery.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotShippingQuery annotation) {
        return botRequest.getUpdate().hasShippingQuery();
    }
}
