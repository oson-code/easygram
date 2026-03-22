package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to one or more Telegram bot commands.
 * The annotated method is invoked when an incoming message contains a command
 * (e.g., {@code /start}) that matches any of the specified values.
 * An empty {@code value} array matches all commands not handled by a more specific mapping.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotCommand {

    /**
     * The bot command strings (e.g., {@code "/start"}, {@code "/help"}) that trigger this handler.
     *
     * @return an array of command strings to match against incoming messages
     */
    String[] value() default {};
}
