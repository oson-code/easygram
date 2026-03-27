package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to deleted business message events.
 *
 * <p>Annotate a method with {@code @BotDeletedBusinessMessages} to handle any update whose
 * {@code deleted_business_messages} field is populated (i.e., when messages sent by the
 * bot on behalf of a business account are deleted).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotDeletedBusinessMessages {
}
