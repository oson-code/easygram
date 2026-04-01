package uz.osoncode.easygram.core.argumentresolver;

import uz.osoncode.easygram.core.model.BotMetadata;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;

/**
 * Resolver for {@link BotMetadata} arguments.
 * Injects the metadata (token, username, ID) of the bot handling the current request.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotMetadataArgumentResolver implements BotArgumentResolver {

    @Override
    public boolean supportsParameter(Parameter parameter) {
        return BotMetadata.class.isAssignableFrom(ParameterUtils.effectiveType(parameter));
    }

    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        return botRequest.getBotMetadata();
    }
}
