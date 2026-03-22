package uz.osoncode.easygram.core.i18n;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.markup.MarkupAware;

import java.util.Map;

/**
 * Immutable value object representing a localised reply where the content is a simple
 * message bundle key (without {@code ${}} syntax) and arguments.
 *
 * <p>Unlike {@link LocalizedTemplate}, this class does not support mixed content or
 * template tokens. The {@code key} is resolved directly against the message bundle,
 * and {@code args} are used for formatting that specific message.</p>
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
public final class LocalizedReply implements MarkupAware {

    private final String key;
    private final Object[] args;
    private final String markupId;
    private final Map<String, Object> markupParams;
    private final ReplyKeyboard keyboard;
    private final boolean removeMarkup;

    private LocalizedReply(String key, Object[] args, String markupId,
                            Map<String, Object> markupParams, ReplyKeyboard keyboard,
                            boolean removeMarkup) {
        this.key = key;
        this.args = args;
        this.markupId = markupId;
        this.markupParams = markupParams;
        this.keyboard = keyboard;
        this.removeMarkup = removeMarkup;
    }

    /**
     * Creates a {@code LocalizedReply} with the given message key and arguments.
     *
     * @param key  the message bundle key (e.g. "welcome.message"); must not be {@code null}
     * @param args optional arguments for the message format
     * @return a new {@code LocalizedReply} instance
     */
    public static LocalizedReply of(String key, Object... args) {
        return new LocalizedReply(key, args, null, null, null, false);
    }

    @Override
    public LocalizedReply withMarkup(String markupId) {
        return new LocalizedReply(this.key, this.args, markupId, null, null, false);
    }

    /**
     * Returns a new {@code LocalizedReply} with the given markup ID and factory parameters.
     *
     * @param markupId the ID of a pre-registered markup; must not be {@code null}
     * @param params   the parameters to pass to the factory; must not be {@code null}
     * @return a new {@code LocalizedReply} with the markup ID and params set
     */
    @Override
    public LocalizedReply withMarkup(String markupId, Map<String, Object> params) {
        return new LocalizedReply(this.key, this.args, markupId, params, null, false);
    }

    /**
     * Returns a new {@code LocalizedReply} with the given {@link ReplyKeyboard} attached directly.
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code LocalizedReply} with the keyboard set
     */
    @Override
    public LocalizedReply withKeyboard(ReplyKeyboard keyboard) {
        return new LocalizedReply(this.key, this.args, null, null, keyboard, false);
    }

    @Override
    public LocalizedReply removeMarkup() {
        return new LocalizedReply(this.key, this.args, null, null, null, true);
    }

    public String getKey() {
        return key;
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

