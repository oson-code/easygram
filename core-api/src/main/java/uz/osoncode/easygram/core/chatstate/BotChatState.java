package uz.osoncode.easygram.core.chatstate;

import java.lang.annotation.Documented;
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
 * <h2>State-bound keyboards ({@link uz.osoncode.easygram.core.annotation.BotMarkup} combined usage)</h2>
 * <p>When {@code @BotChatState} is placed on a
 * {@link uz.osoncode.easygram.core.annotation.BotMarkup @BotMarkup} method inside a
 * {@link uz.osoncode.easygram.core.annotation.BotConfiguration @BotConfiguration} class,
 * the keyboard factory is additionally registered as the <em>default keyboard</em> for each
 * declared state. Handlers that enter that state — either because they are already in it or
 * because they carry {@code @BotForwardChatState("STATE")} — will receive the keyboard
 * automatically, without an explicit
 * {@link uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup @BotReplyMarkup}.</p>
 *
 * <p><b>Constraint:</b> {@code value} must not be empty when used on a {@code @BotMarkup}
 * method; an empty array has no states to bind to and is silently ignored by the loader.</p>
 *
 * <p><b>Edit-mode constraint:</b> in edit-message context only {@code InlineKeyboardMarkup}
 * is supported by Telegram. A state-bound {@code ReplyKeyboardMarkup} is silently skipped
 * when the handler returns with {@code editMessage = true}.</p>
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
 *
 * // State-bound keyboard — automatically shown when entering "REGISTRATION"
 * @BotConfiguration
 * public class MyMarkups {
 *
 *     @BotMarkup("registration_kb")
 *     @BotChatState("REGISTRATION")
 *     public ReplyKeyboardMarkup registrationKeyboard() {
 *         return ReplyKeyboardMarkup.builder()...build();
 *     }
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @see uz.osoncode.easygram.core.annotation.BotMarkup
 * @see uz.osoncode.easygram.core.markup.BotMarkupRegistry
 * @since 0.0.1
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotChatState {

    /**
     * The chat state identifiers that activate this handler.
     *
     * @return an array of state name strings; empty array matches any state
     */
    String[] value() default {};
}
