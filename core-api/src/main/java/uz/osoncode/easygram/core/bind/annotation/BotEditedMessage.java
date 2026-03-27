package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to edited messages received by the bot.
 *
 * <p>Annotate a method with {@code @BotEditedMessage} to handle any update whose
 * {@code edited_message} field is populated.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotEditedMessage {
}
