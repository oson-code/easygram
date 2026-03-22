package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Injects the callback query data string from the current Telegram update
 * into the annotated method parameter.
 * Apply this annotation to a {@link String} parameter in a handler method to receive
 * the data payload associated with an inline keyboard callback query.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotCallbackQueryData {
}
