package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Injects a query parameter value from the current bot command into the annotated method parameter.
 * Use this annotation to extract individual arguments passed alongside a bot command
 * (e.g., the {@code "42"} in {@code /start 42}).
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotCommandQueryParam {
}
