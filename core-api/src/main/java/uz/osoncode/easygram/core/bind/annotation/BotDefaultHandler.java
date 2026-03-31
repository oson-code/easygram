package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or type as the global fallback handler for any Telegram update that does not match
 * any other more specific handler mapping in the bot controller hierarchy.
 * Use this annotation to define catch-all behaviour for unrecognised updates.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BotDefaultHandler {
}
