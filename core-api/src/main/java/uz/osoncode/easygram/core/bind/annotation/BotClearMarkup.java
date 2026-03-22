package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Decorates a handler method to automatically remove the current reply markup (keyboard)
 * from the user's client after the response is sent.
 *
 * <p>This is equivalent to sending a {@link org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove}.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @BotCommand("/cancel")
 * @BotClearMarkup
 * public String onCancel() {
 *     return "Operation cancelled.";
 * }
 * }</pre>
 *
 * <p>If this annotation is present, it takes precedence over {@link BotReplyMarkup}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotClearMarkup {
}
