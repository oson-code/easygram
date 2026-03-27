package uz.osoncode.easygram.core.handler.businessconnection.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotBusinessConnection;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotBusinessConnection} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a business connection
 * event. Handler methods annotated with {@link BotBusinessConnection} are invoked when
 * a business account is connected to or disconnected from the bot.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotBusinessConnectionMetaDataResolver implements BotMetaDataSpecResolver<BotBusinessConnection> {

    @Override
    public Class<BotBusinessConnection> getAnnotationType() {
        return BotBusinessConnection.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotBusinessConnection annotation) {
        return botRequest.getUpdate().hasBusinessConnection();
    }
}
