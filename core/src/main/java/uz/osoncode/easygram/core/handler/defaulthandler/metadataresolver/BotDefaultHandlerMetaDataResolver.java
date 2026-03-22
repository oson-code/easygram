package uz.osoncode.easygram.core.handler.defaulthandler.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataDefaultResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotDefaultHandler} annotation.
 *
 * <p>Serves as a global catch-all resolver that unconditionally matches any
 * {@link BotRequest} regardless of the update type. Handler methods annotated
 * with {@link BotDefaultHandler} are used as the last-resort fallback when no
 * other registered handler supports the incoming update.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotDefaultHandlerMetaDataResolver implements BotMetaDataDefaultResolver<BotDefaultHandler> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotDefaultHandler} annotation class.
     */
    @Override
    public Class<BotDefaultHandler> getAnnotationType() {
        return BotDefaultHandler.class;
    }

    /**
     * Always returns {@code true}, matching every update unconditionally.
     *
     * <p>Because this resolver is registered as a default resolver, it is only
     * consulted after all specific handlers have failed to match, making it the
     * universal fallback for any update type.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotDefaultHandler} annotation declared on the handler method.
     * @return {@code true} always.
     */
    @Override
    public boolean support(BotRequest botRequest, BotDefaultHandler annotation) {
        return true;
    }
}
