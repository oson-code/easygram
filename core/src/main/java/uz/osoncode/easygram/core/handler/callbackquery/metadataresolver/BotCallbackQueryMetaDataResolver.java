package uz.osoncode.easygram.core.handler.callbackquery.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotCallbackQuery;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotCallbackQuery} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a callback query whose
 * data exactly matches one of the values declared in the {@link BotCallbackQuery} annotation.
 * This resolver is used to route specific callback query payloads to the correct handler method.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotCallbackQueryMetaDataResolver implements BotMetaDataSpecResolver<BotCallbackQuery> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotCallbackQuery} annotation class.
     */
    @Override
    public Class<BotCallbackQuery> getAnnotationType() {
        return BotCallbackQuery.class;
    }

    /**
     * Returns {@code true} if the update contains a callback query whose data equals
     * one of the values specified in the {@link BotCallbackQuery} annotation.
     *
     * <p>If the update does not contain a callback query, this method returns {@code false}
     * immediately without inspecting the annotation values.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotCallbackQuery} annotation declared on the handler method.
     * @return {@code true} if the callback query data matches any of the annotation's values;
     *         {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotCallbackQuery annotation) {
        if (!botRequest.getUpdate().hasCallbackQuery()) return false;
        String messageData = botRequest.getUpdate().getCallbackQuery().getData();
        if (messageData == null) return false;
        for (String text : annotation.value()) {
            if (messageData.equals(text)) return true;
        }
        return false;
    }
}
