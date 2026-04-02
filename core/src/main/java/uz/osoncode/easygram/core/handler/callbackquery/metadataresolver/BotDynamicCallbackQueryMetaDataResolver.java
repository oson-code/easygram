package uz.osoncode.easygram.core.handler.callbackquery.metadataresolver;

import lombok.RequiredArgsConstructor;
import uz.osoncode.easygram.core.bind.annotation.BotDynamicCallbackQuery;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackQueryService;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotDynamicCallbackQuery} annotation.
 *
 * <p>When an incoming update contains a callback query, this resolver looks up the raw
 * callback data string via {@link BotDynamicCallbackQueryService#resolve} to retrieve
 * the associated {@link BotDynamicCallbackData} payload.  If the payload exists and its
 * {@link BotDynamicCallbackData#getType()} matches one of the values declared on the
 * annotation, the handler method is eligible for dispatch.</p>
 *
 * <p>On a positive match the resolved payload is stored in
 * {@link BotRequest#setAttribute} under {@link BotDynamicCallbackData#ATTRIBUTE_KEY} so
 * that {@link uz.osoncode.easygram.core.argumentresolver.BotDynamicCallbackDataArgumentResolver}
 * can inject it without a second service lookup.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.4
 * @see BotDynamicCallbackQuery
 * @see BotDynamicCallbackQueryService
 */
@RequiredArgsConstructor
public class BotDynamicCallbackQueryMetaDataResolver implements BotMetaDataSpecResolver<BotDynamicCallbackQuery> {

    private final BotDynamicCallbackQueryService dynamicCallbackQueryService;

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotDynamicCallbackQuery} annotation class
     */
    @Override
    public Class<BotDynamicCallbackQuery> getAnnotationType() {
        return BotDynamicCallbackQuery.class;
    }

    /**
     * Returns {@code true} when the update contains a callback query whose resolved payload
     * type matches one of the values in the annotation.
     *
     * <p>Resolution steps:</p>
     * <ol>
     *   <li>Reject immediately if the update does not contain a callback query.</li>
     *   <li>Call {@link BotDynamicCallbackQueryService#resolve} with the raw callback data.</li>
     *   <li>Reject if no payload is found (unknown key).</li>
     *   <li>Check whether the payload's type equals any value in {@code annotation.value()}.</li>
     *   <li>On match, cache the payload in {@link BotRequest#setAttribute} for downstream injection.</li>
     * </ol>
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}
     * @param annotation the {@link BotDynamicCallbackQuery} annotation on the handler method
     * @return {@code true} if the payload type matches; {@code false} otherwise
     */
    @Override
    public boolean support(BotRequest botRequest, BotDynamicCallbackQuery annotation) {
        if (!botRequest.getUpdate().hasCallbackQuery()) return false;

        String callbackData = botRequest.getUpdate().getCallbackQuery().getData();
        BotDynamicCallbackData payload = dynamicCallbackQueryService.resolve(callbackData);
        if (payload == null) return false;

        for (String type : annotation.value()) {
            if (payload.getType().equals(type)) {
                botRequest.setAttribute(BotDynamicCallbackData.ATTRIBUTE_KEY, payload);
                return true;
            }
        }
        return false;
    }
}
