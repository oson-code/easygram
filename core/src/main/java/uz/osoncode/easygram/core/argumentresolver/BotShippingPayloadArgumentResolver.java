package uz.osoncode.easygram.core.argumentresolver;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.ShippingQuery;
import uz.osoncode.easygram.core.bind.annotation.BotShippingPayload;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Argument resolver that injects the shipping query invoice payload into a
 * {@link BotShippingPayload}-annotated handler method parameter.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotShippingPayloadArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return parameter.isAnnotationPresent(BotShippingPayload.class);
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return Optional.ofNullable(botRequest.getUpdate())
                .map(Update::getShippingQuery)
                .map(ShippingQuery::getInvoicePayload)
                .orElse(null);
    }
}
