package uz.osoncode.easygram.core.annotation;

import java.lang.annotation.*;

/**
 * Defines the execution order of a bot handler method.
 * Lower values have higher priority. Defaults to {@link Integer#MAX_VALUE}.
 * Use this annotation alongside handler annotations like {@code @BotCommand}, {@code @BotText}, etc.
 * <pre>
 * {@code
 * @BotCommand("/start")
 * @BotOrder(1)
 * public void handleStart() { ... }
 * }
 * </pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotOrder {

    /**
     * The order value; lower values indicate higher priority.
     *
     * @return the order value, defaults to {@link Integer#MAX_VALUE}
     */
    int value() default Integer.MAX_VALUE;
}
