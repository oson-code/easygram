package uz.osoncode.easygram.core.handler.callbackquery.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotDefaultCallbackQuery;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataDefaultResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotDefaultCallbackQuery} annotation.
 *
 * <p>Acts as a fallback resolver that matches any incoming callback query update,
 * regardless of the callback data payload. Handler methods annotated with
 * {@link BotDefaultCallbackQuery} are invoked when no specific
 * {@link uz.osoncode.easygram.core.bind.annotation.BotCallbackQuery}
 * handler matched the current request.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotDefaultCallbackQueryMetaDataResolver implements BotMetaDataDefaultResolver<BotDefaultCallbackQuery> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotDefaultCallbackQuery} annotation class.
     */
    @Override
    public Class<BotDefaultCallbackQuery> getAnnotationType() {
        return BotDefaultCallbackQuery.class;
    }

    /**
     * Returns {@code true} if the update contains a callback query of any data value.
     *
     * <p>This resolver does not inspect the callback query data and therefore matches
     * every callback query update, making it the catch-all fallback for callback queries.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotDefaultCallbackQuery} annotation declared on the handler method.
     * @return {@code true} if the update contains a callback query; {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotDefaultCallbackQuery annotation) {
        return botRequest.getUpdate().hasCallbackQuery();
    }
}

