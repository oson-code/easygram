package uz.osoncode.easygram.core.argumentresolver;

import uz.osoncode.easygram.core.markup.BotMarkupContext;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * Argument resolver that injects the current {@link BotMarkupContext} into a
 * {@code @BotMarkup} factory method (or any handler method that declares a
 * {@link BotMarkupContext} parameter).
 *
 * <p>The context is read from {@link BotRequest#getAttribute(String)} under
 * {@link BotMarkupContext#REQUEST_ATTRIBUTE_KEY}. It is populated by the framework
 * immediately before {@link uz.osoncode.easygram.core.markup.BotMarkupRegistry#resolve}
 * is called, when the return value carries markup parameters via
 * {@link uz.osoncode.easygram.core.markup.MarkupAware#withMarkup(String, java.util.Map)}.</p>
 *
 * <p>If no context was set on the request (e.g. the factory is invoked without params),
 * {@link BotMarkupContext#empty()} is returned so factory methods always receive a
 * non-null value.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotMarkupContext
 */
public class BotMarkupContextArgumentResolver implements BotArgumentResolver {

    /**
     * Returns {@code true} when the parameter type is {@link BotMarkupContext}.
     *
     * @param parameter the method parameter to evaluate
     * @return {@code true} if the type is {@link BotMarkupContext}
     */
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return BotMarkupContext.class.isAssignableFrom(ParameterUtils.effectiveType(parameter));
    }

    /**
     * Returns the {@link BotMarkupContext} stored on the request, or
     * {@link BotMarkupContext#empty()} if none was set.
     *
     * @param parameter   the method parameter being resolved
     * @param botRequest  the current request context; may be {@code null}
     * @param botResponse unused
     * @return the current {@link BotMarkupContext}; never {@code null}
     */
    @Override
    public Object resolveArgument(Parameter parameter, BotRequest botRequest, BotResponse botResponse) {
        if (Objects.isNull(botRequest)) {
            return BotMarkupContext.empty();
        }
        BotMarkupContext ctx = botRequest.getAttribute(BotMarkupContext.REQUEST_ATTRIBUTE_KEY);
        return Objects.nonNull(ctx) ? ctx : BotMarkupContext.empty();
    }
}
