package uz.osoncode.easygram.core.markup;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.model.BotRequest;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Factory SPI for creating markup factory functions from {@code @BotMarkup}-annotated methods.
 *
 * <p>This interface separates <em>markup factory creation</em> (deciding how to invoke the
 * method and resolve its parameters) from the scanning and registration orchestration performed
 * by {@link BotMarkupLoader}. The default implementation, {@link DefaultBotMarkupFactory},
 * uses the framework's {@link uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory}
 * to resolve method parameters, enabling arbitrary parameter types in {@code @BotMarkup} methods.</p>
 *
 * <h2>Extension example</h2>
 * <pre>{@code
 * @Bean
 * public BotMarkupFactory myMarkupFactory(BotArgumentResolverFactory resolverFactory) {
 *     return new MyCustomMarkupFactory(resolverFactory);
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see DefaultBotMarkupFactory
 * @see BotMarkupLoader
 */
public interface BotMarkupFactory {

    /**
     * Creates a markup factory function for the given method on the given bean.
     *
     * <p>The returned {@code Function} is stored in {@link BotMarkupRegistry} under the
     * markup ID and called at request time with the current {@link BotRequest}. Implementations
     * are responsible for resolving the method's parameters and invoking it reflectively.</p>
     *
     * @param bean   the {@code @BotConfiguration} bean instance that owns the method;
     *               must not be {@code null}
     * @param method the {@code @BotMarkup}-annotated method to wrap; must not be {@code null}
     * @return a {@code Function<BotRequest, ReplyKeyboard>} that invokes the method with resolved
     *         arguments each time a markup is requested; never {@code null}
     */
    Function<BotRequest, ReplyKeyboard> create(Object bean, Method method);
}
