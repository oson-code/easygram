package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Injects the chosen inline result ID string into a handler method parameter.
 *
 * <p>Apply this annotation to a {@link String} parameter to receive the result ID of the
 * chosen inline result directly, without needing to unwrap the full
 * {@code ChosenInlineQuery} object.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotChosenInlineResultId {
}
