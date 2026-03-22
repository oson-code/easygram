package uz.osoncode.easygram.core.bind.annotation;


import java.lang.annotation.*;

/**
 * Maps a handler method to Telegram messages that contain a shared location.
 * The annotated method is invoked when an incoming message includes a location object
 * (e.g., when a user shares their geographic coordinates via Telegram).
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotLocation {
}
