package uz.osoncode.easygram.core.markup;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory;
import uz.osoncode.easygram.core.model.BotRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Function;

/**
 * Default {@link BotMarkupFactory} that resolves {@code @BotMarkup} method parameters
 * using the framework's {@link BotArgumentResolverFactory}.
 *
 * <p>At request time, for every parameter declared on the markup method this factory
 * asks the {@link BotArgumentResolverFactory} to resolve a value from the current
 * {@link BotRequest}. {@code BotResponse} is passed as {@code null} because markup
 * methods are solely responsible for producing a {@link ReplyKeyboard} — they do not
 * participate in message dispatch.</p>
 *
 * <p>This approach makes any parameter type resolvable by a registered
 * {@link uz.osoncode.easygram.core.argumentresolver.BotArgumentResolver} valid in a
 * {@code @BotMarkup} method signature, including {@code User}, {@code Chat},
 * {@code TelegramClient}, {@code Locale} (core-i18n), and custom resolver types.</p>
 *
 * <p>Override this bean by declaring a custom {@code BotMarkupFactory} bean annotated
 * with {@code @ConditionalOnMissingBean} to change how markup methods are invoked
 * without modifying any framework class.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotMarkupFactory
 * @see BotArgumentResolverFactory
 */
@RequiredArgsConstructor
public class DefaultBotMarkupFactory implements BotMarkupFactory {

    /** Factory used to resolve each parameter of a {@code @BotMarkup} method. */
    private final BotArgumentResolverFactory argumentResolverFactory;

    /**
     * Creates a markup factory function that resolves all method parameters via
     * {@link BotArgumentResolverFactory} and invokes the method reflectively.
     *
     * @param bean   the {@code @BotConfiguration} bean instance
     * @param method the {@code @BotMarkup}-annotated method
     * @return a {@code Function<BotRequest, ReplyKeyboard>} backed by the method
     */
    @Override
    public Function<BotRequest, ReplyKeyboard> create(Object bean, Method method) {
        method.setAccessible(true);
        Parameter[] parameters = method.getParameters();
        return request -> {
            Object[] args = argumentResolverFactory.resolveArguments(parameters, request, null);
            try {
                return (ReplyKeyboard) method.invoke(bean, args);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to invoke @BotMarkup method '" + method.getName() + "'", e);
            }
        };
    }
}
