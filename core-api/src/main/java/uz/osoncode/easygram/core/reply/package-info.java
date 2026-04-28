/**
 * Immutable value-object return types for bot handler methods that represent plain-text replies.
 *
 * <p>These types implement {@link uz.osoncode.easygram.core.markup.MarkupAware} and can carry an
 * optional markup ID or a remove-markup instruction, which the framework resolves against
 * {@link uz.osoncode.easygram.core.markup.BotMarkupRegistry} before sending the message.</p>
 *
 * <h2>Key types</h2>
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.reply.PlainReply} — sends the text as-is, with optional
 *       keyboard attachment and optional {@link java.text.MessageFormat} positional arguments
 *       ({@code {0}}, {@code {1}}, …)</li>
 * </ul>
 *
 * <p>For locale-aware replies, use {@code LocalizedReply} from the {@code core-i18n} module instead.</p>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // Simple plain text
 * return PlainReply.of("Hello!");
 *
 * // With positional args (Java MessageFormat)
 * return PlainReply.of("Hello, {0}!", user.getFirstName());
 *
 * // With a pre-registered keyboard
 * return PlainReply.of("Choose an option:").withMarkup("main_menu");
 *
 * // Remove keyboard after handler returns
 * return PlainReply.of("Cancelled.").removeMarkup();
 * }</pre>
 */
package uz.osoncode.easygram.core.reply;
