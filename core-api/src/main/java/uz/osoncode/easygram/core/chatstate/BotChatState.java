package uz.osoncode.easygram.core.chatstate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts a handler method or an entire {@code @BotController} class to specific chat states.
 *
 * <p>When applied to a <strong>method</strong>, only that handler is guarded by the declared
 * states. When applied to a <strong>class</strong>, the annotation acts as a class-wide default:
 * every handler method in that class inherits the state requirement, unless the method declares
 * its own {@code @BotChatState} which then overrides the class-level default — identical to the
 * way Spring MVC's {@code @RequestMapping} at class level provides a default path prefix.</p>
 *
 * <p>An empty {@code value} array (the default) means the element accepts <em>any</em> state,
 * making it a convenient method-level override to "opt out" of a class-level restriction.</p>
 *
 * <pre>{@code
 * // All handlers in this class require "REGISTRATION" state.
 * @BotController
 * @BotChatState("REGISTRATION")
 * public class RegistrationFlow {
 *
 *     @BotText("name")
 *     public String handleName(@BotTextValue String name, ...) { ... }  // requires REGISTRATION
 *
 *     @BotText("email")
 *     public String handleEmail(@BotTextValue String email, ...) { ... } // requires REGISTRATION
 *
 *     @BotCommand("/cancel")
 *     @BotChatState   // empty value overrides class-level -> matches ANY state
 *     public String cancel() { ... }
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BotChatState {

    /**
     * The chat state identifiers that activate this handler.
     *
     * @return an array of state name strings; empty array matches any state
     */
    String[] value() default {};
}
