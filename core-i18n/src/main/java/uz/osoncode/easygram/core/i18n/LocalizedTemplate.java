package uz.osoncode.easygram.core.i18n;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.markup.MarkupAware;

import java.util.Map;

/**
 * Immutable value object representing a localised template to be resolved by the framework
 * before sending to Telegram.
 *
 * <p>The {@code template} string supports two token types:</p>
 * <ul>
 *   <li>{@code ${key}} — resolved from the message bundle via
 *       {@link BotMessageSource#getMessage(String, uz.osoncode.easygram.core.model.BotRequest, Object...)}
 *       using the user's locale</li>
 *   <li>{@code #{index}} — replaced with {@code args[index]} (0-based positional argument)</li>
 * </ul>
 *
 * <p>Keyboard markup can be attached in three ways, in order of precedence:</p>
 * <ol>
 *   <li>{@link #withKeyboard(ReplyKeyboard)} — attach a directly-built keyboard inline.</li>
 *   <li>{@link #withMarkup(String, Map)} — resolve a registered keyboard by ID and pass
 *       runtime parameters to the {@code @BotMarkup} factory method.</li>
 *   <li>{@link #withMarkup(String)} — resolve a registered keyboard by ID (no params).</li>
 *   <li>{@link #removeMarkup()} — send {@code ReplyKeyboardRemove} to clear the keyboard.</li>
 * </ol>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // Single message-bundle key
 * return LocalizedTemplate.of("${welcome}");
 *
 * // Key with a positional argument
 * return LocalizedTemplate.of("${greeting} #{0}!", user.getFirstName());
 *
 * // Literal text mixed with key and argument
 * return LocalizedTemplate.of("${hello} #{0}, ${how.are.you}?", user.getFirstName());
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public final class LocalizedTemplate implements MarkupAware {

    private final String template;
    private final Object[] args;
    private final String markupId;
    private final Map<String, Object> markupParams;
    private final ReplyKeyboard keyboard;
    private final boolean removeMarkup;

    private LocalizedTemplate(String template, Object[] args, String markupId,
                               Map<String, Object> markupParams, ReplyKeyboard keyboard,
                               boolean removeMarkup) {
        this.template = template;
        this.args = args;
        this.markupId = markupId;
        this.markupParams = markupParams;
        this.keyboard = keyboard;
        this.removeMarkup = removeMarkup;
    }

    /**
     * Creates a {@code LocalizedTemplate} with the given template and optional positional arguments.
     *
     * @param template the template string; must not be {@code null}
     * @param args     optional positional arguments referenced via {@code #{index}} tokens
     * @return a new {@code LocalizedTemplate} instance
     */
    public static LocalizedTemplate of(String template, Object... args) {
        return new LocalizedTemplate(template, args, null, null, null, false);
    }

    /**
     * Returns a copy of this reply with the specified markup ID attached.
     *
     * @param markupId the ID of the markup to resolve and attach; may be {@code null}
     * @return a new {@code LocalizedTemplate} instance
     */
    @Override
    public LocalizedTemplate withMarkup(String markupId) {
        return new LocalizedTemplate(this.template, this.args, markupId, null, null, false);
    }

    /**
     * Returns a new {@code LocalizedTemplate} with the given markup ID and factory parameters.
     *
     * @param markupId the ID of a pre-registered markup; must not be {@code null}
     * @param params   the parameters to pass to the factory; must not be {@code null}
     * @return a new {@code LocalizedTemplate} with the markup ID and params set
     */
    @Override
    public LocalizedTemplate withMarkup(String markupId, Map<String, Object> params) {
        return new LocalizedTemplate(this.template, this.args, markupId, params, null, false);
    }

    /**
     * Returns a new {@code LocalizedTemplate} with the given {@link ReplyKeyboard} attached directly.
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code LocalizedTemplate} with the keyboard set
     */
    @Override
    public LocalizedTemplate withKeyboard(ReplyKeyboard keyboard) {
        return new LocalizedTemplate(this.template, this.args, null, null, keyboard, false);
    }

    /**
     * Returns a copy of this reply with the instruction to remove the current markup.
     *
     * @return a new {@code LocalizedTemplate} with the remove-markup flag set
     */
    @Override
    public LocalizedTemplate removeMarkup() {
        return new LocalizedTemplate(this.template, this.args, null, null, null, true);
    }

    /**
     * Returns the raw template string (not yet resolved).
     *
     * @return the template string; never {@code null}
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Returns the positional arguments to be substituted for {@code #{index}} tokens.
     *
     * @return the argument array; may be empty, never {@code null}
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * Returns the ID of the markup to attach, or {@code null} if none.
     *
     * @return the markup ID
     */
    @Override
    public String getMarkupId() {
        return markupId;
    }

    @Override
    public Map<String, Object> getMarkupParams() {
        return markupParams;
    }

    @Override
    public ReplyKeyboard getKeyboard() {
        return keyboard;
    }

    /**
     * Returns {@code true} if this reply instructs to remove the current markup.
     *
     * @return {@code true} if markup should be removed
     */
    @Override
    public boolean isRemoveMarkup() {
        return removeMarkup;
    }
}

