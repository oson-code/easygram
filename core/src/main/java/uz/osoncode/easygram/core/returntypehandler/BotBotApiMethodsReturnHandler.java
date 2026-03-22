package uz.osoncode.easygram.core.returntypehandler;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * {@link BotReturnTypeHandler} implementation that handles handler methods returning
 * a {@link Collection} of {@link BotApiMethod} instances.
 *
 * <p>Supports any parameterized {@link Collection} whose element type is {@link BotApiMethod}
 * or a subclass, including wildcard forms such as {@code Collection<BotApiMethod<?>>}.
 * When the return value is non-null, all elements in the collection are forwarded to
 * {@link BotResponse} for dispatch to the Telegram API.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotBotApiMethodsReturnHandler implements BotReturnTypeHandler {

    /**
     * Determines whether this handler supports the return type of the given method.
     *
     * <p>The check proceeds through the following steps:
     * <ol>
     *   <li>The return type must be assignable to {@link Collection}.</li>
     *   <li>The return type must carry generic type information (i.e. be a {@link ParameterizedType}).</li>
     *   <li>The element type of the collection must be {@link BotApiMethod} or a subclass.
     *       Wildcard parameterizations such as {@code BotApiMethod<?>} are unwrapped to their
     *       raw type before the assignability check.</li>
     * </ol>
     *
     * @param method the handler method whose return type is checked; must not be {@code null}
     * @return {@code true} if the return type is a parameterized {@link Collection} of
     *         {@link BotApiMethod} elements
     */
    @Override
    public boolean supportsReturnType(Method method) {
        // 1. Must be a Collection
        if (!Collection.class.isAssignableFrom(method.getReturnType())) {
            return false;
        }

        // 2. Must have generic information
        Type genericReturnType = method.getGenericReturnType();

        if (!(genericReturnType instanceof ParameterizedType parameterizedType)) {
            return false;
        }

        // 3. Extract generic argument: Collection<T>
        Type elementType = parameterizedType.getActualTypeArguments()[0];

        // 4. Handle cases like BotApiMethod<?>
        if (elementType instanceof ParameterizedType pt) {
            elementType = pt.getRawType();
        }

        // 5. Ensure it is BotApiMethod or subclass
        if (elementType instanceof Class<?> clazz) {
            return BotApiMethod.class.isAssignableFrom(clazz);
        }

        return false;
    }

    /**
     * Processes the return value by adding all API methods in the collection to {@link BotResponse}.
     *
     * <p>If {@code returnValue} is non-null, it is cast to {@code Collection<BotApiMethod<?>>}
     * and all its entries are registered in {@code botResponse} so the framework can send them
     * to Telegram. A {@code null} return value is silently ignored.</p>
     *
     * @param botRequest  the current bot request providing context for the update; must not be {@code null}
     * @param botResponse the mutable response object to which the API methods are added; must not be {@code null}
     * @param returnValue the value returned by the handler method; may be {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {
        if (returnValue != null) {
            botResponse.addBotApiMethods((Collection<BotApiMethod<?>>) returnValue);
        }
    }
}
