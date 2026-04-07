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
    private final boolean answerCallbackQuery;
    private final boolean callbackAlert;
    private final String callbackUrl;
    private final Integer callbackCacheTime;

    private PlainTextTemplate(String template, Object[] args, String markupId,
                               Map<String, Object> markupParams, ReplyKeyboard keyboard,
                               boolean removeMarkup, boolean editMessage,
                               boolean answerCallbackQuery, boolean callbackAlert,
                               String callbackUrl, Integer callbackCacheTime) {
        this.template = template;
        this.args = args;
        this.markupId = markupId;
        this.markupParams = markupParams;
        this.keyboard = keyboard;
        this.removeMarkup = removeMarkup;
        this.editMessage = editMessage;
        this.answerCallbackQuery = answerCallbackQuery;
        this.callbackAlert = callbackAlert;
        this.callbackUrl = callbackUrl;
        this.callbackCacheTime = callbackCacheTime;
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
        private boolean answerCallbackQuery;
        private boolean callbackAlert;
        private String callbackUrl;
        private Integer callbackCacheTime;

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
         * Sets whether to answer the callback query using this template's resolved text as
         * the popup notification. Setting this to {@code true} is equivalent to calling
         * {@link PlainTextTemplate#asAnswerCallbackQuery()} on the built instance.
         *
         * @param answerCallbackQuery {@code true} to answer the callback query
         * @return this builder
         * @since 0.0.5
         */
        public Builder answerCallbackQuery(boolean answerCallbackQuery) {
            this.answerCallbackQuery = answerCallbackQuery;
            return this;
        }

        /**
         * Instructs the framework to show the callback answer as an alert dialog instead of a
         * toast. Implicitly enables {@code answerCallbackQuery}.
         *
         * @param callbackAlert {@code true} for an alert dialog
         * @return this builder
         * @since 0.0.5
         */
        public Builder callbackAlert(boolean callbackAlert) {
            if (callbackAlert) this.answerCallbackQuery = true;
            this.callbackAlert = callbackAlert;
            return this;
        }

        /**
         * Sets an optional URL to open when the callback answer notification is tapped.
         * Implicitly enables {@code answerCallbackQuery}.
         *
         * @param callbackUrl the URL; may be {@code null}
         * @return this builder
         * @since 0.0.5
         */
        public Builder callbackUrl(String callbackUrl) {
            if (callbackUrl != null) this.answerCallbackQuery = true;
            this.callbackUrl = callbackUrl;
            return this;
        }

        /**
         * Sets the client-side cache duration in seconds for the callback answer.
         * Implicitly enables {@code answerCallbackQuery}.
         *
         * @param callbackCacheTime cache duration in seconds; may be {@code null}
         * @return this builder
         * @since 0.0.5
         */
        public Builder callbackCacheTime(Integer callbackCacheTime) {
            if (callbackCacheTime != null) this.answerCallbackQuery = true;
            this.callbackCacheTime = callbackCacheTime;
            return this;
        }

        /**
         * Builds and returns the immutable {@link PlainTextTemplate}.
         *
         * @return a new {@code PlainTextTemplate} instance
         */
        public PlainTextTemplate build() {
            return new PlainTextTemplate(template, args, markupId, markupParams, keyboard, removeMarkup, editMessage,
                    answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime);
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
        return new PlainTextTemplate(template, args, null, null, null, false, false, false, false, null, null);
    }

    @Override
    public PlainTextTemplate withMarkup(String markupId) {
        return new PlainTextTemplate(this.template, this.args, markupId, null, null, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime);
    }

    @Override
    public PlainTextTemplate withMarkup(String markupId, Map<String, Object> params) {
        return new PlainTextTemplate(this.template, this.args, markupId, params, null, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime);
    }

    @Override
    public PlainTextTemplate withKeyboard(ReplyKeyboard keyboard) {
        return new PlainTextTemplate(this.template, this.args, null, null, keyboard, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime);
    }

    @Override
    public PlainTextTemplate removeMarkup() {
        return new PlainTextTemplate(this.template, this.args, null, null, null, true, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime);
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
        return new PlainTextTemplate(this.template, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, true, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime);
    }

    /**
     * Returns a new {@code PlainTextTemplate} that will also send an {@link org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery}
     * using the resolved template text as the popup notification text.
     *
     * <p>This method is a no-op when the originating update is not a callback query.</p>
     *
     * @return a new {@code PlainTextTemplate} with {@code answerCallbackQuery = true}
     * @since 0.0.5
     */
    public PlainTextTemplate asAnswerCallbackQuery() {
        return new PlainTextTemplate(this.template, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, true, this.callbackAlert, this.callbackUrl, this.callbackCacheTime);
    }

    /**
     * Returns a new {@code PlainTextTemplate} that shows the callback answer as an alert
     * dialog instead of a toast. Implicitly enables {@code answerCallbackQuery}.
     *
     * @return a new {@code PlainTextTemplate} with {@code callbackAlert = true}
     * @since 0.0.5
     */
    public PlainTextTemplate withCallbackAlert() {
        return new PlainTextTemplate(this.template, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, true, true, this.callbackUrl, this.callbackCacheTime);
    }

    /**
     * Returns a new {@code PlainTextTemplate} with the given URL to open when the
     * callback answer notification is tapped. Implicitly enables {@code answerCallbackQuery}.
     *
     * @param url the URL; must not be {@code null}
     * @return a new {@code PlainTextTemplate} with the callback URL set
     * @since 0.0.5
     */
    public PlainTextTemplate withCallbackUrl(String url) {
        return new PlainTextTemplate(this.template, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, true, this.callbackAlert, url, this.callbackCacheTime);
    }

    /**
     * Returns a new {@code PlainTextTemplate} with the given client-side cache duration
     * for the callback answer. Implicitly enables {@code answerCallbackQuery}.
     *
     * @param cacheTime cache duration in seconds
     * @return a new {@code PlainTextTemplate} with the callback cache time set
     * @since 0.0.5
     */
    public PlainTextTemplate withCallbackCacheTime(int cacheTime) {
        return new PlainTextTemplate(this.template, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, true, this.callbackAlert, this.callbackUrl, cacheTime);
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

    /**
     * Returns {@code true} if an {@link org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery}
     * should be sent alongside this reply.
     *
     * @return {@code true} to answer the callback query
     * @since 0.0.5
     */
    public boolean isAnswerCallbackQuery() {
        return answerCallbackQuery;
    }

    /**
     * Returns {@code true} if the callback answer should be shown as an alert dialog.
     *
     * @return {@code true} for alert dialog; {@code false} for toast notification
     * @since 0.0.5
     */
    public boolean isCallbackAlert() {
        return callbackAlert;
    }

    /**
     * Returns the optional URL to open when the callback answer notification is tapped.
     *
     * @return the callback URL, or {@code null} if not set
     * @since 0.0.5
     */
    public String getCallbackUrl() {
        return callbackUrl;
    }

    /**
     * Returns the client-side cache duration in seconds for the callback answer.
     *
     * @return the cache time in seconds, or {@code null} to use the Telegram default
     * @since 0.0.5
     */
    public Integer getCallbackCacheTime() {
        return callbackCacheTime;
    }
}

