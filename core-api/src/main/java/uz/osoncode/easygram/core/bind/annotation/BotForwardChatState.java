package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the chat state to the given value after the annotated handler method returns successfully.
 *
 * <p>Place this annotation on a handler method inside a {@code @BotController} to automatically
 * advance the conversation to a new state without injecting {@link uz.osoncode.easygram.core.chatstate.BotChatStateService}
 * manually:</p>
 *
 * <pre>{@code
 * @BotCommand("/start")
 * @BotForwardChatState("WAITING_NAME")
 * public String onStart() {
 *     return "Please enter your name:";
 * }
 * }</pre>
 *
 * <p>The state change is applied <strong>after</strong> the handler returns without throwing an
 * exception. If the current update has no associated {@code Chat} (e.g. inline-query updates),
 * or if no {@link uz.osoncode.easygram.core.chatstate.BotChatStateService} bean is present in
 * the application context, the annotation is silently ignored.</p>
 *
 * <p>If both {@code @BotForwardChatState} and {@link BotClearChatState} are present on the
 * same method, {@link BotClearChatState} takes precedence.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotClearChatState
 * @see uz.osoncode.easygram.core.chatstate.BotChatState
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotForwardChatState {

    /**
     * The state value to set after the handler returns.
     *
     * @return the new chat state string; must not be empty
     */
    String value();
}
