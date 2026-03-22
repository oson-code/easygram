package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Marks a method as the fallback handler for Telegram bot commands that do not match
 * any more specific {@link BotCommand} mapping.
 * Only one default command handler should be defined per controller.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotDefaultCommand {
}
