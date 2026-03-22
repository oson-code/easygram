package uz.osoncode.easygram.core.reply;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.markup.MarkupAware;

import java.util.Map;

/**
 * Immutable value object representing a plain text reply whose content is built via
 * {@link String#format(String, Object...)} at send time.
 *
 * <p>The content is NOT resolved against message bundles. Use
 * {@code LocalizedTemplate} (in {@code core-i18n}) when i18n is required.</p>
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
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public final class PlainTextTemplate implements MarkupAware {

    private final String template;
    private final Object[] args;
    private final String markupId;
    private final Map<String, Object> markupParams;
    private final ReplyKeyboard keyboard;
    private final boolean removeMarkup;

    private PlainTextTemplate(String template, Object[] args, String markupId,
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
     * Creates a {@code PlainTextTemplate} with the given text and optional arguments.
     *
     * @param template the text template (e.g. "Hello %s"); must not be {@code null}
     * @param args     optional arguments for {@link String#format}
     * @return a new {@code PlainTextTemplate} instance
     */
    public static PlainTextTemplate of(String template, Object... args) {
        return new PlainTextTemplate(template, args, null, null, null, false);
    }

    @Override
    public PlainTextTemplate withMarkup(String markupId) {
        return new PlainTextTemplate(this.template, this.args, markupId, null, null, false);
    }

    /**
     * Returns a new {@code PlainTextTemplate} with the given markup ID and factory parameters.
     *
     * @param markupId the ID of a pre-registered markup; must not be {@code null}
     * @param params   the parameters to pass to the factory; must not be {@code null}
     * @return a new {@code PlainTextTemplate} with the markup ID and params set
     */
    @Override
    public PlainTextTemplate withMarkup(String markupId, Map<String, Object> params) {
        return new PlainTextTemplate(this.template, this.args, markupId, params, null, false);
    }

    /**
     * Returns a new {@code PlainTextTemplate} with the given {@link ReplyKeyboard} attached directly.
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code PlainTextTemplate} with the keyboard set
     */
    @Override
    public PlainTextTemplate withKeyboard(ReplyKeyboard keyboard) {
        return new PlainTextTemplate(this.template, this.args, null, null, keyboard, false);
    }

    @Override
    public PlainTextTemplate removeMarkup() {
        return new PlainTextTemplate(this.template, this.args, null, null, null, true);
    }

    public String getTemplate() {
        return template;
    }

    public Object[] getArgs() {
        return args;
    }

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

    @Override
    public boolean isRemoveMarkup() {
        return removeMarkup;
    }
}

