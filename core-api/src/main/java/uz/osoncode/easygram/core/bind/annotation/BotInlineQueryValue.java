package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Injects the inline query text string into a handler method parameter.
 *
 * <p>Apply this annotation to a {@link String} parameter to receive the text of the
 * current inline query directly, without needing to unwrap the full
 * {@code InlineQuery} object.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotInlineQueryValue {
}
