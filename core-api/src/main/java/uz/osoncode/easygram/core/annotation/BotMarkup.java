package uz.osoncode.easygram.core.annotation;
import uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup;

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
 * {@link uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory} used
 * for handler methods, so all built-in injectable types are available:</p>
 * <ul>
 *   <li><strong>No parameters</strong> — static/locale-independent markup</li>
 *   <li>{@link uz.osoncode.easygram.core.model.BotRequest} — full request context</li>
 *   <li>{@link org.telegram.telegrambots.meta.api.objects.User} — the message sender</li>
 *   <li>{@link org.telegram.telegrambots.meta.api.objects.Chat} — the originating chat</li>
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
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
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
