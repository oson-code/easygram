package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to poll answer events.
 *
 * <p>Annotate a method with {@code @BotPollAnswer} to handle any update whose
 * {@code poll_answer} field is populated (i.e., when a user changes their vote in a
 * non-anonymous poll).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotPollAnswer {
}
