package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to shipping queries received by the bot.
 *
 * <p>Annotate a method with {@code @BotShippingQuery} to handle any update whose
 * {@code shipping_query} field is populated.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotShippingQuery {
}
