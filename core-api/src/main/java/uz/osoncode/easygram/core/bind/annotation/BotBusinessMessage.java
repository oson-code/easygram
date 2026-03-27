package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to business account messages.
 *
 * <p>Annotate a method with {@code @BotBusinessMessage} to handle any update whose
 * {@code business_message} field is populated (i.e., messages sent on behalf of a
 * connected business account).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotBusinessMessage {
}
