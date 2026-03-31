package uz.osoncode.easygram.core.argumentresolver;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Utility methods for inspecting {@link Parameter} types with transparent
 * {@link Optional} unwrapping.
 *
 * <p>When a handler method declares a parameter as {@code Optional<T>}, the framework
 * resolves the inner type {@code T} and wraps the result in {@code Optional.ofNullable()}.
 * This class provides the helpers used by {@link BotArgumentResolver} implementations and
 * {@code BotArgumentResolverFactory} to support that contract.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.3
 */
public final class ParameterUtils {

    private ParameterUtils() {}

    /**
     * Returns the <em>effective</em> type of {@code parameter}.
     *
     * <p>If the parameter is declared as {@code Optional<T>} this method returns {@code T}.
     * For any other type (including a raw {@code Optional} with no type argument) it returns
     * {@code parameter.getType()} unchanged.</p>
     *
     * @param parameter the method parameter to inspect; must not be {@code null}
     * @return the unwrapped inner type when the parameter is {@code Optional<T>},
     *         otherwise the declared parameter type
     */
    public static Class<?> effectiveType(Parameter parameter) {
        if (!Optional.class.equals(parameter.getType())) {
            return parameter.getType();
        }
        Type generic = parameter.getParameterizedType();
        if (generic instanceof ParameterizedType pt) {
            Type inner = pt.getActualTypeArguments()[0];
            if (inner instanceof Class<?> cls) {
                return cls;
            }
        }
        // Raw Optional or wildcard — fall back to Optional.class itself
        return Optional.class;
    }

    /**
     * Returns {@code true} if {@code parameter} is declared as {@code Optional<T>}
     * (including raw {@code Optional} without a type argument).
     *
     * @param parameter the method parameter to inspect; must not be {@code null}
     * @return {@code true} if the parameter type is {@link Optional}
     */
    public static boolean isOptional(Parameter parameter) {
        return Optional.class.equals(parameter.getType());
    }
}
