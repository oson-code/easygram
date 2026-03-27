package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to poll state update events.
 *
 * <p>Annotate a method with {@code @BotPoll} to handle any update whose
 * {@code poll} field is populated (i.e., when a poll is stopped or its state changes).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotPoll {
}
