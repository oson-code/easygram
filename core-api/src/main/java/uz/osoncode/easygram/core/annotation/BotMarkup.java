package uz.osoncode.easygram.core.annotation;

import uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup;
import uz.osoncode.easygram.core.chatstate.BotChatState;

import java.lang.annotation.*;

/**
 * Marks a method in a {@link BotConfiguration} class as a **markup factory**.
 *
 * <p>When the application starts, the framework scans all {@link BotConfiguration} beans,
 * finds methods annotated with {@code @BotMarkup}, and registers them in the
 * {@link uz.osoncode.easygram.core.markup.BotMarkupRegistry} under the given ID.
 * The registered factory is later resolved at request time when a handler specifies
 * {@link BotReplyMarkup} or uses {@code .withMarkup("id")} on a return value.</p>
 *
 * <h2>Supported method signatures</h2>
 * <p>A {@code @BotMarkup} method may declare any number of parameters as long as each
 * parameter type is resolvable by a registered
 * {@link uz.osoncode.easygram.core.argumentresolver.BotArgumentResolver}.
 * The framework resolves parameters using the same
 * {@code BotArgumentResolverFactory} used
 * for handler methods, so all built-in injectable types are available:</p>
 * <ul>
 *   <li><strong>No parameters</strong> — static/locale-independent markup</li>
 *   <li>{@link uz.osoncode.easygram.core.model.BotRequest} — full request context</li>
 *   <li>{@link org.telegram.telegrambots.meta.api.objects.User} — the message sender</li>
 *   <li>{@link org.telegram.telegrambots.meta.api.objects.chat.Chat} — the originating chat</li>
 *   <li>{@link org.telegram.telegrambots.meta.generics.TelegramClient} — the Telegram client</li>
 *   <li>{@code Locale} — user locale (when {@code core-i18n} is on the classpath)</li>
 *   <li>Any custom type provided by a user-defined {@code BotArgumentResolver} bean</li>
 * </ul>
 *
 * <h2>Supported return types</h2>
 * <p>Any type that is assignable to
 * {@link org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard}
 * (e.g. {@code InlineKeyboardMarkup}, {@code ReplyKeyboardMarkup}).</p>
 *
 * <h2>State-bound keyboards (combined with {@link uz.osoncode.easygram.core.chatstate.BotChatState})</h2>
 * <p>Adding {@link uz.osoncode.easygram.core.chatstate.BotChatState @BotChatState("STATE")} to a
 * {@code @BotMarkup} method registers the same factory as the <em>default keyboard</em> for that
 * chat state. When a handler completes without an explicit keyboard or markup ID set, the
 * framework automatically attaches the state-bound keyboard — either for the current state or
 * for the state declared on {@code @BotForwardChatState}. The keyboard is still reachable by
 * its named ID ({@link #value()}) regardless of the state binding.</p>
 *
 * <p><b>Constraint:</b> {@code @BotChatState} used here must declare at least one state name;
 * an empty value array is silently ignored (no state binding is registered).</p>
 *
 * <p><b>Edit-mode constraint:</b> only {@code InlineKeyboardMarkup} is accepted by Telegram's
 * edit-message API. When a handler returns with {@code editMessage = true}, a state-bound
 * {@code ReplyKeyboardMarkup} registered here is silently skipped.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @BotConfiguration
 * public class MyMarkups {
 *
 *     // Static markup — no parameters
 *     @BotMarkup("cancel_kb")
 *     public ReplyKeyboardMarkup cancelKb() {
 *         return ReplyKeyboardMarkup.builder()...build();
 *     }
 *
 *     // Request-aware markup — receives the current BotRequest
 *     @BotMarkup("main_menu")
 *     public InlineKeyboardMarkup mainMenu(BotRequest request) {
 *         return InlineKeyboardMarkup.builder()...build();
 *     }
 *
 *     // Multi-parameter markup — user and chat resolved automatically
 *     @BotMarkup("profile_kb")
 *     public InlineKeyboardMarkup profileKb(User user, Chat chat) {
 *         return InlineKeyboardMarkup.builder()...build();
 *     }
 *
 *     // State-bound keyboard — auto-applied when entering "REGISTRATION"
 *     @BotMarkup("registration_kb")
 *     @BotChatState("REGISTRATION")
 *     public ReplyKeyboardMarkup registrationKb() {
 *         return ReplyKeyboardMarkup.builder()...build();
 *     }
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @see uz.osoncode.easygram.core.chatstate.BotChatState
 * @see uz.osoncode.easygram.core.markup.BotMarkupRegistry
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotMarkup {

    /**
     * The unique registration ID for this markup factory.
     * Must be non-empty. Used as the key in {@link uz.osoncode.easygram.core.markup.BotMarkupRegistry}.
     *
     * @return the markup ID; must not be empty
     */
    String value();
}
