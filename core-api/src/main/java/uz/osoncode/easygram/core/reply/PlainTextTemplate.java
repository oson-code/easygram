package uz.osoncode.easygram.core.reply;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.markup.MarkupAware;

import java.util.Map;

/**
 * Immutable value object representing a plain text reply whose content is built by
 * substituting {@code #{index}} tokens with positional arguments at send time.
 *
 * <p>The content is NOT resolved against message bundles. Use
 * {@code LocalizedTemplate} (in {@code core-i18n}) when i18n is required.</p>
 *
 * <p>Token format:</p>
 * <ul>
 *   <li>{@code #{index}} — replaced with {@code args[index]} (0-based); if the index is
 *       out of bounds the token is left unchanged.</li>
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
 * // Static factory — no args
 * return PlainTextTemplate.of("Hello!");
 *
 * // Static factory — with positional args
 * return PlainTextTemplate.of("Hello #{0}! You are #{1} years old.", name, age);
 *
 * // Builder pattern
 * return PlainTextTemplate.builder()
 *         .template("Hello #{0}!")
 *         .args(user.getFirstName())
 *         .markupId("main_menu")
 *         .build();
 * }</pre>
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
    private final boolean editMessage;

    private PlainTextTemplate(String template, Object[] args, String markupId,
                               Map<String, Object> markupParams, ReplyKeyboard keyboard,
                               boolean removeMarkup, boolean editMessage) {
        this.template = template;
        this.args = args;
        this.markupId = markupId;
        this.markupParams = markupParams;
        this.keyboard = keyboard;
        this.removeMarkup = removeMarkup;
        this.editMessage = editMessage;
    }

    /**
     * Creates a new {@link Builder} for {@code PlainTextTemplate}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PlainTextTemplate}.
     *
     * <pre>{@code
     * PlainTextTemplate reply = PlainTextTemplate.builder()
     *         .template("Hello #{0}!")
     *         .args(user.getFirstName())
     *         .markupId("main_menu")
     *         .build();
     * }</pre>
     */
    public static final class Builder {

        private String template;
        private Object[] args;
        private String markupId;
        private Map<String, Object> markupParams;
        private ReplyKeyboard keyboard;
        private boolean removeMarkup;
        private boolean editMessage;

        private Builder() {}

        /**
         * Sets the template string (may contain {@code #{index}} tokens).
         *
         * @param template the template; must not be {@code null}
         * @return this builder
         */
        public Builder template(String template) {
            this.template = template;
            return this;
        }

        /**
         * Sets the positional arguments substituted for {@code #{index}} tokens.
         *
         * @param args the arguments
         * @return this builder
         */
        public Builder args(Object... args) {
            this.args = args;
            return this;
        }

        /**
         * Sets the pre-registered markup ID.
         *
         * @param markupId the ID of a registered markup
         * @return this builder
         */
        public Builder markupId(String markupId) {
            this.markupId = markupId;
            return this;
        }

        /**
         * Sets the markup factory parameters forwarded to the {@code @BotMarkup} factory.
         *
         * @param markupParams the parameters map
         * @return this builder
         */
        public Builder markupParams(Map<String, Object> markupParams) {
            this.markupParams = markupParams;
            return this;
        }

        /**
         * Sets a directly-built keyboard (takes precedence over {@link #markupId}).
         *
         * @param keyboard the keyboard to attach
         * @return this builder
         */
        public Builder keyboard(ReplyKeyboard keyboard) {
            this.keyboard = keyboard;
            return this;
        }

        /**
         * Instructs the framework to send a {@code ReplyKeyboardRemove}.
         *
         * @return this builder
         */
        public Builder removeMarkup() {
            this.removeMarkup = true;
            return this;
        }

        /**
         * Instructs the framework to edit the original message (via {@code EditMessageText})
         * when this reply is returned from a callback query handler.
         *
         * @param editMessage {@code true} to edit; {@code false} to send a new message (default)
         * @return this builder
         * @since 0.0.2
         */
        public Builder editMessage(boolean editMessage) {
            this.editMessage = editMessage;
            return this;
        }

        /**
         * Builds and returns the immutable {@link PlainTextTemplate}.
         *
         * @return a new {@code PlainTextTemplate} instance
         */
        public PlainTextTemplate build() {
            return new PlainTextTemplate(template, args, markupId, markupParams, keyboard, removeMarkup, editMessage);
        }
    }

    /**
     * Creates a {@code PlainTextTemplate} with the given template and optional positional arguments.
     *
     * <p>Use {@code #{index}} tokens in the template to reference arguments by 0-based index,
     * e.g. {@code "Hello #{0}!"} with {@code args = [name]}.</p>
     *
     * @param template the template string; must not be {@code null}
     * @param args     optional positional arguments referenced via {@code #{index}} tokens
     * @return a new {@code PlainTextTemplate} instance
     */
    public static PlainTextTemplate of(String template, Object... args) {
        java.util.Objects.requireNonNull(template, "template must not be null");
        return new PlainTextTemplate(template, args, null, null, null, false, false);
    }

    @Override
    public PlainTextTemplate withMarkup(String markupId) {
        return new PlainTextTemplate(this.template, this.args, markupId, null, null, false, this.editMessage);
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
        return new PlainTextTemplate(this.template, this.args, markupId, params, null, false, this.editMessage);
    }

    /**
     * Returns a new {@code PlainTextTemplate} with the given {@link ReplyKeyboard} attached directly.
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code PlainTextTemplate} with the keyboard set
     */
    @Override
    public PlainTextTemplate withKeyboard(ReplyKeyboard keyboard) {
        return new PlainTextTemplate(this.template, this.args, null, null, keyboard, false, this.editMessage);
    }

    @Override
    public PlainTextTemplate removeMarkup() {
        return new PlainTextTemplate(this.template, this.args, null, null, null, true, this.editMessage);
    }

    /**
     * Returns a new {@code PlainTextTemplate} with the {@code editMessage} flag set to {@code true}.
     *
     * <p>When this flag is set and the request originates from a callback query, the framework
     * will edit the originating message instead of sending a new one.</p>
     *
     * @return a new {@code PlainTextTemplate} with {@code editMessage = true}
     * @since 0.0.2
     */
    public PlainTextTemplate withEditMessage() {
        return new PlainTextTemplate(this.template, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup, true);
    }

    public String getTemplate() {
        return template;
    }

    public Object[] getArgs() {
        return args != null ? args.clone() : null;
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

    /**
     * Returns {@code true} if the framework should edit the original callback-query message
     * instead of sending a new message.
     *
     * @return {@code true} to edit the originating message
     * @since 0.0.2
     */
    @Override
    public boolean isEditMessage() {
        return editMessage;
    }
}

