package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to the bot's own chat member status changes.
 *
 * <p>Annotate a method with {@code @BotMyChatMember} to handle any update whose
 * {@code my_chat_member} field is populated (e.g., when the bot is added to or removed
 * from a chat, or when its permissions change).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotMyChatMember {
}
