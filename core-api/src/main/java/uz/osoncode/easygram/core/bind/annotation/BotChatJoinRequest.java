package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to chat join requests.
 *
 * <p>Annotate a method with {@code @BotChatJoinRequest} to handle any update whose
 * {@code chat_join_request} field is populated (i.e., when a user requests to join a
 * chat that requires admin approval).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotChatJoinRequest {
}
