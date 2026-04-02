package uz.osoncode.easygram.core.argumentresolver;

import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that injects the resolved {@link BotDynamicCallbackData} payload
 * into handler method parameters.
 *
 * <p>The payload is read from {@link BotRequest#getAttribute} using
 * {@link BotDynamicCallbackData#ATTRIBUTE_KEY}, which is set by
 * {@link uz.osoncode.easygram.core.handler.callbackquery.metadataresolver.BotDynamicCallbackQueryMetaDataResolver}
 * during handler matching. No second service lookup is performed.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @BotDynamicCallbackQuery("product_buy")
 * public String onBuy(BotDynamicCallbackData data) {
 *     Long id = (Long) data.getData().get("id");
 *     return "Bought product #" + id;
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.4
 * @see BotDynamicCallbackData
 */
public class BotDynamicCallbackDataArgumentResolver implements BotArgumentResolver {

    /**
     * Returns {@code true} when the parameter type is {@link BotDynamicCallbackData}.
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if this resolver supports the parameter
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ParameterUtils.effectiveType(parameter).equals(BotDynamicCallbackData.class);
    }

    /**
     * Returns the {@link BotDynamicCallbackData} previously cached in the request attributes
     * by the metadata resolver, or {@code null} if not present.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current bot request
     * @param botResponse the mutable response accumulator (unused here)
     * @return the resolved {@link BotDynamicCallbackData}, or {@code null}
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botRequest.getAttribute(BotDynamicCallbackData.ATTRIBUTE_KEY);
    }
}
