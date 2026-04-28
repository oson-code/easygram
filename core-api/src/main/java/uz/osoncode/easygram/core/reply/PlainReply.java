package uz.osoncode.easygram.core.reply;

import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.ReplyParameters;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.markup.MarkupAware;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object representing a plain-text reply to be sent by the framework.
 *
 * <p>Unlike {@code LocalizedReply} (in {@code core-i18n}), {@code PlainReply} sends the
 * text as-is — no i18n lookup is performed. Use it when the message text is already
 * fully composed at the call site.</p>
 *
 * <p>Optional positional arguments may be provided via {@link #of(String, Object...)} or
 * {@link #withArgs(Object...)}. When present, the text is processed with
 * {@link java.text.MessageFormat#format(String, Object[])} before it is sent, so standard
 * {@code {0}}, {@code {1}}, … placeholders are substituted:</p>
 * <pre>{@code
 * return PlainReply.of("Hello, {0}! You have {1} messages.", user.getFirstName(), count);
 * }</pre>
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
 * // Plain text, no markup
 * return PlainReply.of("Hello!");
 *
 * // With positional args (Java MessageFormat)
 * return PlainReply.of("Hello, {0}!", user.getFirstName());
 *
 * // Pre-registered markup by ID
 * return PlainReply.of("Choose an option:").withMarkup("main_menu");
 *
 * // Pre-registered markup with runtime params
 * return PlainReply.of("Page 2:").withMarkup("item_list", Map.of("page", 2));
 *
 * // Directly built keyboard
 * return PlainReply.of("Dynamic:").withKeyboard(buildKeyboard(items));
 *
 * // Builder pattern
 * return PlainReply.builder()
 *         .text("Hello, {0}!")
 *         .args(user.getFirstName())
 *         .markupId("main_menu")
 *         .build();
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public final class PlainReply implements MarkupAware {

    private final String text;
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
    private final String parseMode;
    private final Boolean disableNotification;
    private final Boolean protectContent;
    private final Integer messageThreadId;
    private final ReplyParameters replyParameters;
    private final LinkPreviewOptions linkPreviewOptions;

    private PlainReply(String text, Object[] args, String markupId, Map<String, Object> markupParams,
                       ReplyKeyboard keyboard, boolean removeMarkup, boolean editMessage,
                       boolean answerCallbackQuery, boolean callbackAlert,
                       String callbackUrl, Integer callbackCacheTime, String parseMode,
                       Boolean disableNotification, Boolean protectContent, Integer messageThreadId,
                       ReplyParameters replyParameters, LinkPreviewOptions linkPreviewOptions) {
        this.text = text;
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
        this.parseMode = parseMode;
        this.disableNotification = disableNotification;
        this.protectContent = protectContent;
        this.messageThreadId = messageThreadId;
        this.replyParameters = replyParameters;
        this.linkPreviewOptions = linkPreviewOptions;
    }

    /**
     * Creates a new {@link Builder} for {@code PlainReply}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PlainReply}.
     *
     * <pre>{@code
     * PlainReply reply = PlainReply.builder()
     *         .text("Choose an option:")
     *         .markupId("main_menu")
     *         .build();
     * }</pre>
     */
    public static final class Builder {

        private String text;
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
        private String parseMode;
        private Boolean disableNotification;
        private Boolean protectContent;
        private Integer messageThreadId;
        private ReplyParameters replyParameters;
        private LinkPreviewOptions linkPreviewOptions;

        private Builder() {}

        /**
         * Sets the reply text.
         *
         * @param text the reply text; must not be {@code null}
         * @return this builder
         */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Sets positional arguments for {@link java.text.MessageFormat} substitution.
         *
         * <p>When args are present the text is formatted with
         * {@code MessageFormat.format(text, args)} before sending, so {@code {0}}, {@code {1}}, …
         * placeholders in the text are replaced with the corresponding arguments.</p>
         *
         * @param args the positional arguments; may be empty
         * @return this builder
         * @since 0.0.7
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
         * Activates {@code AnswerCallbackQuery} emission. The reply text is used as the
         * popup text. Has no effect when the update is not a callback query.
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
         * Sets {@code showAlert = true} on the {@code AnswerCallbackQuery} call, displaying
         * an alert dialog instead of a toast notification. Implicitly sets
         * {@code answerCallbackQuery = true}.
         *
         * @return this builder
         * @since 0.0.5
         */
        public Builder callbackAlert(boolean callbackAlert) {
            if (callbackAlert) this.answerCallbackQuery = true;
            this.callbackAlert = callbackAlert;
            return this;
        }

        /**
         * Sets the {@code url} parameter on the {@code AnswerCallbackQuery} call. Used for
         * opening a URL or launching a game. Implicitly sets {@code answerCallbackQuery = true}.
         *
         * @param callbackUrl the URL to open; may be {@code null}
         * @return this builder
         * @since 0.0.5
         */
        public Builder callbackUrl(String callbackUrl) {
            if (callbackUrl != null) this.answerCallbackQuery = true;
            this.callbackUrl = callbackUrl;
            return this;
        }

        /**
         * Sets the {@code cache_time} parameter on the {@code AnswerCallbackQuery} call.
         * Implicitly sets {@code answerCallbackQuery = true}.
         *
         * @param callbackCacheTime cache duration in seconds; must be non-negative
         * @return this builder
         * @since 0.0.5
         */
        public Builder callbackCacheTime(Integer callbackCacheTime) {
            if (callbackCacheTime != null) this.answerCallbackQuery = true;
            this.callbackCacheTime = callbackCacheTime;
            return this;
        }

        /**
         * Sets the Telegram parse mode for the outgoing message.
         *
         * <p>Typical values: {@code "HTML"}, {@code "MarkdownV2"}, {@code "Markdown"}.</p>
         *
         * @param parseMode the Telegram parse mode string; may be {@code null} to leave unset
         * @return this builder
         * @since 0.0.6
         */
        public Builder parseMode(String parseMode) {
            this.parseMode = parseMode;
            return this;
        }

        /**
         * Sets whether to send the message silently (no sound or vibration on the receiver's device).
         *
         * @param disableNotification {@code true} to send silently; {@code null} for default
         * @return this builder
         * @since 0.0.7
         */
        public Builder disableNotification(Boolean disableNotification) {
            this.disableNotification = disableNotification;
            return this;
        }

        /**
         * Sets whether to protect the message content from forwarding and saving.
         *
         * @param protectContent {@code true} to protect; {@code null} for default
         * @return this builder
         * @since 0.0.7
         */
        public Builder protectContent(Boolean protectContent) {
            this.protectContent = protectContent;
            return this;
        }

        /**
         * Sets the forum topic thread ID. Only applicable in supergroups with forum topics enabled.
         *
         * @param messageThreadId the thread ID; {@code null} for regular (non-threaded) chats
         * @return this builder
         * @since 0.0.7
         */
        public Builder messageThreadId(Integer messageThreadId) {
            this.messageThreadId = messageThreadId;
            return this;
        }

        /**
         * Sets the reply-to parameters so the message appears as a reply to a specific message.
         *
         * @param replyParameters the reply parameters; {@code null} to send without replying
         * @return this builder
         * @since 0.0.7
         */
        public Builder replyParameters(ReplyParameters replyParameters) {
            this.replyParameters = replyParameters;
            return this;
        }

        /**
         * Sets the link preview options for the outgoing message.
         *
         * @param linkPreviewOptions the link preview options; {@code null} for default preview
         * @return this builder
         * @since 0.0.7
         */
        public Builder linkPreviewOptions(LinkPreviewOptions linkPreviewOptions) {
            this.linkPreviewOptions = linkPreviewOptions;
            return this;
        }

        /**
         * Builds and returns the immutable {@link PlainReply}.
         *
         * @return a new {@code PlainReply} instance
         */
        public PlainReply build() {
            return new PlainReply(text, args, markupId, markupParams, keyboard, removeMarkup, editMessage,
                    answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime, parseMode,
                    disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
        }
    }

    /**
     * Creates a {@code PlainReply} with the given text and no markup or args.
     *
     * @param text the reply text; must not be {@code null}
     * @return a new {@code PlainReply} instance
     */
    public static PlainReply of(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return new PlainReply(text, null, null, null, null, false, false, false, false, null, null, null, null, null, null, null, null);
    }

    /**
     * Creates a {@code PlainReply} with the given text and positional arguments.
     *
     * <p>The text is formatted with {@link java.text.MessageFormat#format(String, Object[])}
     * before being sent, replacing {@code {0}}, {@code {1}}, … placeholders with the
     * corresponding arguments.</p>
     *
     * @param text the reply text template; must not be {@code null}
     * @param args the positional arguments
     * @return a new {@code PlainReply} instance
     * @since 0.0.7
     */
    public static PlainReply of(String text, Object... args) {
        Objects.requireNonNull(text, "text must not be null");
        return new PlainReply(text, args, null, null, null, false, false, false, false, null, null, null, null, null, null, null, null);
    }

    /**
     * Returns a new {@code PlainReply} with the same text and the given markup ID.
     *
     * @param markupId the ID of a pre-registered markup; must not be {@code null}
     * @return a new {@code PlainReply} with the markup ID set
     */
    @Override
    public PlainReply withMarkup(String markupId) {
        return new PlainReply(this.text, this.args, markupId, null, null, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} with the given markup ID and factory parameters.
     *
     * <p>The {@code params} map is forwarded to the {@code @BotMarkup} factory method via
     * {@link uz.osoncode.easygram.core.markup.BotMarkupContext} so the factory can build
     * a dynamic keyboard at request time.</p>
     *
     * @param markupId the ID of a pre-registered markup; must not be {@code null}
     * @param params   the parameters to pass to the factory; must not be {@code null}
     * @return a new {@code PlainReply} with the markup ID and params set
     */
    @Override
    public PlainReply withMarkup(String markupId, Map<String, Object> params) {
        return new PlainReply(this.text, this.args, markupId, params, null, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} with the given {@link ReplyKeyboard} attached directly.
     *
     * <p>The keyboard is used as-is; no registry lookup is performed. This takes precedence
     * over any markup ID set via {@link #withMarkup(String)}.</p>
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code PlainReply} with the keyboard set
     */
    @Override
    public PlainReply withKeyboard(ReplyKeyboard keyboard) {
        return new PlainReply(this.text, this.args, null, null, keyboard, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} with the instruction to remove the markup.
     *
     * @return a new {@code PlainReply} with the remove-markup flag set
     */
    @Override
    public PlainReply removeMarkup() {
        return new PlainReply(this.text, this.args, null, null, null, true, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} with the {@code editMessage} flag set to {@code true}.
     *
     * <p>When this flag is set and the request originates from a callback query, the framework
     * will edit the originating message instead of sending a new one.</p>
     *
     * @return a new {@code PlainReply} with {@code editMessage = true}
     * @since 0.0.2
     */
    public PlainReply withEditMessage() {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup, true,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} that will answer the originating callback query using
     * this reply's text as the popup notification text. If the update is not a callback query
     * the instruction is silently ignored.
     *
     * <p>Chain {@link #withCallbackAlert()}, {@link #withCallbackUrl(String)}, or
     * {@link #withCallbackCacheTime(int)} to further configure the answer.</p>
     *
     * @return a new {@code PlainReply} with {@code answerCallbackQuery = true}
     * @since 0.0.5
     */
    public PlainReply asAnswerCallbackQuery() {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, true, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} with {@code showAlert = true} on the
     * {@code AnswerCallbackQuery} call, displaying an alert dialog instead of a toast.
     * Implicitly activates {@code answerCallbackQuery}.
     *
     * @return a new {@code PlainReply} with the alert flag set
     * @since 0.0.5
     */
    public PlainReply withCallbackAlert() {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, true, true, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} with the given URL set on the
     * {@code AnswerCallbackQuery} call. Implicitly activates {@code answerCallbackQuery}.
     *
     * @param url the URL to open; must not be {@code null}
     * @return a new {@code PlainReply} with the callback URL set
     * @since 0.0.5
     */
    public PlainReply withCallbackUrl(String url) {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, true, this.callbackAlert, url, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} with the given cache time set on the
     * {@code AnswerCallbackQuery} call. Implicitly activates {@code answerCallbackQuery}.
     *
     * @param cacheTime cache duration in seconds; must be non-negative
     * @return a new {@code PlainReply} with the cache time set
     * @since 0.0.5
     */
    public PlainReply withCallbackCacheTime(int cacheTime) {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, true, this.callbackAlert, this.callbackUrl, cacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code PlainReply} with the given Telegram parse mode set.
     *
     * <p>Typical values: {@code "HTML"}, {@code "MarkdownV2"}, {@code "Markdown"}.</p>
     *
     * @param parseMode the Telegram parse mode string; must not be {@code null}
     * @return a new {@code PlainReply} with the parse mode set
     * @since 0.0.6
     */
    @Override
    public PlainReply withParseMode(String parseMode) {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns the reply text.
     *
     * @return the text; never {@code null}
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the positional arguments for {@link java.text.MessageFormat} substitution,
     * or {@code null} if no args were provided.
     *
     * @return the args array; may be {@code null}
     * @since 0.0.7
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * Returns a new {@code PlainReply} with the given positional arguments set.
     *
     * <p>The text is formatted with {@link java.text.MessageFormat#format(String, Object[])}
     * before being sent, replacing {@code {0}}, {@code {1}}, … with the corresponding args.</p>
     *
     * @param args the positional arguments
     * @return a new {@code PlainReply} with the args set
     * @since 0.0.7
     */
    public PlainReply withArgs(Object... args) {
        return new PlainReply(this.text, args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns the markup ID, or {@code null} if none was set.
     *
     * @return the markup ID; may be {@code null}
     */
    @Override
    public String getMarkupId() {
        return markupId;
    }

    /**
     * Returns the markup factory parameters, or {@code null} if none were set.
     *
     * @return the params map; may be {@code null}
     */
    @Override
    public Map<String, Object> getMarkupParams() {
        return markupParams;
    }

    /**
     * Returns the directly-set {@link ReplyKeyboard}, or {@code null} if none was set.
     *
     * @return the keyboard; may be {@code null}
     */
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
     * Returns {@code true} if the framework should send an {@code AnswerCallbackQuery} for the
     * originating callback query using this reply's text as the popup notification text.
     *
     * @return {@code true} to answer the originating callback query
     * @since 0.0.5
     */
    public boolean isAnswerCallbackQuery() {
        return answerCallbackQuery;
    }

    /**
     * Returns {@code true} if the {@code AnswerCallbackQuery} call should display an alert dialog
     * instead of a toast notification ({@code showAlert = true}).
     *
     * @return {@code true} for alert mode
     * @since 0.0.5
     */
    public boolean isCallbackAlert() {
        return callbackAlert;
    }

    /**
     * Returns the URL to be opened by the Telegram client when answering the callback query,
     * or {@code null} if none was set.
     *
     * @return the callback URL; may be {@code null}
     * @since 0.0.5
     */
    public String getCallbackUrl() {
        return callbackUrl;
    }

    /**
     * Returns the client-side cache duration in seconds for the {@code AnswerCallbackQuery}
     * response, or {@code null} if no cache time was specified.
     *
     * @return the cache time in seconds; may be {@code null}
     * @since 0.0.5
     */
    public Integer getCallbackCacheTime() {
        return callbackCacheTime;
    }

    /**
     * Returns the Telegram parse mode string, or {@code null} if none was set.
     *
     * <p>Typical values: {@code "HTML"}, {@code "MarkdownV2"}, {@code "Markdown"}.</p>
     *
     * @return the parse mode; may be {@code null}
     * @since 0.0.6
     */
    @Override
    public String getParseMode() {
        return parseMode;
    }

    /**
     * Returns whether the message should be sent silently (no sound/vibration), or {@code null}
     * to use Telegram's default.
     *
     * @return {@code true} for silent send; {@code null} for default
     * @since 0.0.7
     */
    public Boolean getDisableNotification() {
        return disableNotification;
    }

    /**
     * Returns a new {@code PlainReply} with the given silent-send flag set.
     *
     * @param disableNotification {@code true} to send silently; {@code null} for default
     * @return a new {@code PlainReply} with the flag set
     * @since 0.0.7
     */
    public PlainReply withDisableNotification(Boolean disableNotification) {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns whether the message content is protected from forwarding and saving, or {@code null}
     * to use Telegram's default.
     *
     * @return {@code true} if content is protected; {@code null} for default
     * @since 0.0.7
     */
    public Boolean getProtectContent() {
        return protectContent;
    }

    /**
     * Returns a new {@code PlainReply} with the given protect-content flag set.
     *
     * @param protectContent {@code true} to protect; {@code null} for default
     * @return a new {@code PlainReply} with the flag set
     * @since 0.0.7
     */
    public PlainReply withProtectContent(Boolean protectContent) {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns the forum topic thread ID, or {@code null} for regular (non-threaded) chats.
     *
     * @return the thread ID; may be {@code null}
     * @since 0.0.7
     */
    public Integer getMessageThreadId() {
        return messageThreadId;
    }

    /**
     * Returns a new {@code PlainReply} with the given forum topic thread ID set.
     *
     * @param messageThreadId the thread ID; {@code null} for regular chats
     * @return a new {@code PlainReply} with the thread ID set
     * @since 0.0.7
     */
    public PlainReply withMessageThreadId(Integer messageThreadId) {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns the reply-to parameters, or {@code null} if this message is not a reply.
     *
     * @return the reply parameters; may be {@code null}
     * @since 0.0.7
     */
    public ReplyParameters getReplyParameters() {
        return replyParameters;
    }

    /**
     * Returns a new {@code PlainReply} configured to appear as a reply to a specific message.
     *
     * @param replyParameters the reply parameters; {@code null} to send without replying
     * @return a new {@code PlainReply} with reply parameters set
     * @since 0.0.7
     */
    public PlainReply withReplyParameters(ReplyParameters replyParameters) {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns the link preview options, or {@code null} to use Telegram's default preview.
     *
     * @return the link preview options; may be {@code null}
     * @since 0.0.7
     */
    public LinkPreviewOptions getLinkPreviewOptions() {
        return linkPreviewOptions;
    }

    /**
     * Returns a new {@code PlainReply} with the given link preview options set.
     *
     * @param linkPreviewOptions the link preview options; {@code null} for default preview
     * @return a new {@code PlainReply} with link preview options set
     * @since 0.0.7
     */
    public PlainReply withLinkPreviewOptions(LinkPreviewOptions linkPreviewOptions) {
        return new PlainReply(this.text, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup,
                this.editMessage, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, linkPreviewOptions);
    }
}

