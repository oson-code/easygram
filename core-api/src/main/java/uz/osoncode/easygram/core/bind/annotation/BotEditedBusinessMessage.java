package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to edited business account messages.
 *
 * <p>Annotate a method with {@code @BotEditedBusinessMessage} to handle any update whose
 * {@code edited_business_message} field is populated.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotEditedBusinessMessage {
}
