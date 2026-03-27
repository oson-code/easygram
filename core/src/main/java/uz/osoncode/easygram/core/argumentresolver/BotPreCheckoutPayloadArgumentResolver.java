package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import uz.osoncode.easygram.core.bind.annotation.BotPreCheckoutPayload;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the pre-checkout query invoice payload into a
 * {@link BotPreCheckoutPayload}-annotated handler method parameter.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPreCheckoutPayloadArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(BotPreCheckoutPayload.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getPreCheckoutQuery)
                .map(PreCheckoutQuery::getInvoicePayload)
                .orElse(null);
    }
}
