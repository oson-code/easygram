package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Injects the full text of the current Telegram message into the annotated method parameter.
 * Apply this annotation to a {@link String} parameter in a handler method to receive
 * the plain text content of the incoming message.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotTextValue {
}
