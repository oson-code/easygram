/**
 * Immutable value-object return types for bot handler methods that represent plain-text replies.
 *
 * <p>These types implement {@link uz.osoncode.easygram.core.markup.MarkupAware} and can carry an
 * optional markup ID or a remove-markup instruction, which the framework resolves against
 * {@link uz.osoncode.easygram.core.markup.BotMarkupRegistry} before sending the message.</p>
 *
 * <h2>Key types</h2>
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.reply.PlainReply} — sends the text as-is, with optional keyboard attachment</li>
 *   <li>{@link uz.osoncode.easygram.core.reply.PlainTextTemplate} — sends formatted plain text using inline
 *       {@code #{index}} positional placeholder syntax (non-i18n)</li>
 * </ul>
 *
 * <p>For locale-aware replies, use {@code LocalizedReply} and {@code LocalizedTemplate} from the
 * {@code core-i18n} module instead.</p>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // Simple plain text
 * return PlainReply.of("Hello!");
 *
 * // With a pre-registered keyboard
 * return PlainReply.of("Choose an option:").withMarkup("main_menu");
 *
 * // Remove keyboard after handler returns
 * return PlainReply.of("Cancelled.").removeMarkup();
 *
 * // Formatted template: #{0} replaced with first arg
 * return PlainTextTemplate.of("Welcome, #{0}!", user.getFirstName());
 * }</pre>
 */
package uz.osoncode.easygram.core.reply;
