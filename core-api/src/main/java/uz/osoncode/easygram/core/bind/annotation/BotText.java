package uz.osoncode.easygram.core.bind.annotation;


import java.lang.annotation.*;

/**
 * Maps a handler method to one or more plain-text Telegram messages.
 * The annotated method is invoked when an incoming message's text matches any of the specified strings.
 * An empty {@code value} array matches all text messages not handled by a more specific mapping.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotText {

    /**
     * The exact text strings that trigger this handler.
     *
     * @return an array of text values to match against incoming message text
     */
    String[] value() default {};
}
