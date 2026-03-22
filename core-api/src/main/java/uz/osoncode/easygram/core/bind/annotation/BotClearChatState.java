package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Clears the chat state (sets it to {@code null}) after the annotated handler method returns
 * successfully.
 *
 * <p>Place this annotation on a handler method inside a {@code @BotController} to automatically
 * reset the conversation state without injecting {@link uz.osoncode.easygram.core.chatstate.BotChatStateService}
 * manually:</p>
 *
 * <pre>{@code
 * @BotCommand("/cancel")
 * @BotClearChatState
 * public String onCancel() {
 *     return "Operation cancelled.";
 * }
 * }</pre>
 *
 * <p>The state is cleared <strong>after</strong> the handler returns without throwing an
 * exception. If the current update has no associated {@code Chat} (e.g. inline-query updates),
 * or if no {@link uz.osoncode.easygram.core.chatstate.BotChatStateService} bean is present in
 * the application context, the annotation is silently ignored.</p>
 *
 * <p>If both {@link BotForwardChatState} and {@code @BotClearChatState} are present on the
 * same method, {@code @BotClearChatState} takes precedence.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotForwardChatState
 * @see uz.osoncode.easygram.core.chatstate.BotChatState
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotClearChatState {
}
