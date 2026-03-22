package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Injects the command text (e.g., {@code /start}) from the current Telegram update
 * into the annotated method parameter.
 * Apply this annotation to a {@link String} parameter in a handler method to receive
 * the full command string sent by the user.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotCommandValue {
}
