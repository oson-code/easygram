package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to business connection events.
 *
 * <p>Annotate a method with {@code @BotBusinessConnection} to handle any update whose
 * {@code business_connection} field is populated (i.e., when a business account is
 * connected to or disconnected from the bot).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotBusinessConnection {
}
