package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to one or more Telegram callback query data values.
 * The annotated method is invoked when an incoming callback query's data field
 * matches any of the specified strings.
 * An empty {@code value} array matches all callback queries not handled by a more specific mapping.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotCallbackQuery {

    /**
     * The callback query data strings that trigger this handler.
     *
     * @return an array of data strings to match against incoming callback queries
     */
    String[] value() default {};
}
