package uz.osoncode.easygram.core.bind.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the fallback handler for plain-text Telegram messages that do not match
 * any more specific {@link BotText} mapping.
 * Only one default text handler should be defined per controller.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BotTextDefault {
}
