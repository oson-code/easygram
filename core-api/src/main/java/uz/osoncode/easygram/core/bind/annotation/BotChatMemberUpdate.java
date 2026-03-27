package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to other users' chat member status changes.
 *
 * <p>Annotate a method with {@code @BotChatMemberUpdate} to handle any update whose
 * {@code chat_member} field is populated (i.e., when another user's membership status
 * in a chat changes). Named {@code BotChatMemberUpdate} to avoid a naming clash with
 * the Telegram API's {@code ChatMember} class.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotChatMemberUpdate {
}
