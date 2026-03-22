package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a {@code @BotController} method to one or more
 * {@link org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup} button
 * presses.
 *
 * <p>When a user presses a reply keyboard button the Telegram API delivers a plain text message
 * whose content equals the button label.  The framework matches that text against the declared
 * {@link #value()} strings using the registered {@link uz.osoncode.easygram.core.handler.message.replybutton.BotReplyButtonMatcher}
 * strategy.</p>
 *
 * <h2>Default behaviour (without {@code core-i18n})</h2>
 * <p>Values are compared directly against the incoming message text (exact match):</p>
 * <pre>{@code
 * @BotReplyButton({"Yes", "Да", "Ha"})
 * public String onYes(BotRequest request) {
 *     return "Confirmed!";
 * }
 * }</pre>
 *
 * <h2>With {@code core-i18n} on the classpath</h2>
 * <p>Values are treated as <em>message-bundle keys</em>. The framework resolves each key in
 * the user's locale and compares the result against the incoming text — so a single annotation
 * covers all supported languages automatically:</p>
 * <pre>{@code
 * @BotReplyButton("btn.yes")
 * public String onYes(BotRequest request) {
 *     return messages.getMessage("response.confirmed", request);
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see uz.osoncode.easygram.core.handler.message.replybutton.BotReplyButtonMatcher
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotReplyButton {

    /**
     * One or more values to match against the incoming text message.
     *
     * <p>Without {@code core-i18n}: treated as exact text strings.<br>
     * With {@code core-i18n}: treated as message-bundle keys resolved per user locale.</p>
     *
     * @return array of text values or message keys
     */
    String[] value();
}
