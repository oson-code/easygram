package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to paid media purchase events.
 *
 * <p>Annotate a method with {@code @BotPaidMediaPurchased} to handle any update whose
 * {@code paid_media_purchased} field is populated (i.e., when a user purchases paid
 * media sent by the bot).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotPaidMediaPurchased {
}
