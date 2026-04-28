package uz.osoncode.easygram.core.i18n;

import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.ReplyParameters;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.markup.MarkupAware;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object representing a localised reply where the content is a simple
 * message bundle key and arguments.
 *
 * <p>The {@code key} is resolved directly against the message bundle, and {@code args}
 * are forwarded to {@code MessageSource.getMessage(key, args, locale)} for standard
 * Java {@code MessageFormat} substitution ({@code {0}}, {@code {1}}, …).</p>
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

    private LocalizedReply(String key, Object[] args, String markupId,
                            Map<String, Object> markupParams, ReplyKeyboard keyboard,
                            boolean removeMarkup, boolean editMessage,
                            boolean answerCallbackQuery, boolean callbackAlert,
                            String callbackUrl, Integer callbackCacheTime, String parseMode,
                            Boolean disableNotification, Boolean protectContent, Integer messageThreadId,
                            ReplyParameters replyParameters, LinkPreviewOptions linkPreviewOptions) {
        this.key = key;
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
     * Creates a new {@link Builder} for {@code LocalizedReply}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LocalizedReply}.
     *
     * <pre>{@code
     * LocalizedReply reply = LocalizedReply.builder()
     *         .key("welcome.message")
     *         .args(user.getFirstName())
     *         .markupId("main_menu")
     *         .build();
     * }</pre>
     */
    public static final class Builder {

        private String key;
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
         * Sets the message bundle key.
         *
         * @param key the i18n message key; must not be {@code null}
         * @return this builder
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the optional arguments for the message format.
         *
         * @param args the format arguments
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
         * Activates {@code AnswerCallbackQuery} emission. The resolved i18n message is used as
         * the popup text. Has no effect when the update is not a callback query.
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
         * Sets {@code showAlert = true} on the {@code AnswerCallbackQuery} call. Implicitly
         * sets {@code answerCallbackQuery = true}.
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
         * Sets the {@code url} parameter on the {@code AnswerCallbackQuery} call. Implicitly
         * sets {@code answerCallbackQuery = true}.
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
         * @param callbackCacheTime cache duration in seconds
         * @return this builder
         * @since 0.0.5
         */
        public Builder callbackCacheTime(Integer callbackCacheTime) {
            if (callbackCacheTime != null) this.answerCallbackQuery = true;
            this.callbackCacheTime = callbackCacheTime;
            return this;
        }

        /**
         * Sets the Telegram parse mode (e.g. {@code "HTML"}, {@code "MarkdownV2"}).
         *
         * @param parseMode the parse mode string; may be {@code null}
         * @return this builder
         * @since 0.0.6
         */
        public Builder parseMode(String parseMode) {
            this.parseMode = parseMode;
            return this;
        }

        /**
         * Sets whether to send the message silently (no sound or vibration).
         *
         * @param disableNotification {@code true} to send silently; {@code null} for default
         * @return this builder
         * @since 0.0.6
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
         * @since 0.0.6
         */
        public Builder protectContent(Boolean protectContent) {
            this.protectContent = protectContent;
            return this;
        }

        /**
         * Sets the forum topic thread ID. Only applicable in supergroups with topics enabled.
         *
         * @param messageThreadId the thread ID; {@code null} for regular chats
         * @return this builder
         * @since 0.0.6
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
         * @since 0.0.6
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
         * @since 0.0.6
         */
        public Builder linkPreviewOptions(LinkPreviewOptions linkPreviewOptions) {
            this.linkPreviewOptions = linkPreviewOptions;
            return this;
        }

        /**
         * Builds and returns the immutable {@link LocalizedReply}.
         *
         * @return a new {@code LocalizedReply} instance
         */
        public LocalizedReply build() {
            Objects.requireNonNull(key, "key must not be null");
            return new LocalizedReply(key, args, markupId, markupParams, keyboard, removeMarkup, editMessage,
                    answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime, parseMode,
                    disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
        }
    }

    /**
     * Creates a {@code LocalizedReply} with the given message key and arguments.
     *
     * @param key  the message bundle key (e.g. "welcome.message"); must not be {@code null}
     * @param args optional arguments for the message format
     * @return a new {@code LocalizedReply} instance
     */
    public static LocalizedReply of(String key, Object... args) {
        return new LocalizedReply(key, args, null, null, null, false, false, false, false, null, null, null, null, null, null, null, null);
    }

    @Override
    public LocalizedReply withMarkup(String markupId) {
        return new LocalizedReply(this.key, this.args, markupId, null, null, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
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
        return new LocalizedReply(this.key, this.args, markupId, params, null, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code LocalizedReply} with the given {@link ReplyKeyboard} attached directly.
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code LocalizedReply} with the keyboard set
     */
    @Override
    public LocalizedReply withKeyboard(ReplyKeyboard keyboard) {
        return new LocalizedReply(this.key, this.args, null, null, keyboard, false, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    @Override
    public LocalizedReply removeMarkup() {
        return new LocalizedReply(this.key, this.args, null, null, null, true, this.editMessage,
                this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code LocalizedReply} with the {@code editMessage} flag set to {@code true}.
     *
     * <p>When this flag is set and the request originates from a callback query, the framework
     * will edit the originating message instead of sending a new one.</p>
     *
     * @return a new {@code LocalizedReply} with {@code editMessage = true}
     * @since 0.0.2
     */
    public LocalizedReply withEditMessage() {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, true, this.answerCallbackQuery, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code LocalizedReply} that will answer the originating callback query
     * using the resolved i18n message as the popup notification text. If the update is not a
     * callback query the instruction is silently ignored.
     *
     * <p>Chain {@link #withCallbackAlert()}, {@link #withCallbackUrl(String)}, or
     * {@link #withCallbackCacheTime(int)} to further configure the answer.</p>
     *
     * @return a new {@code LocalizedReply} with {@code answerCallbackQuery = true}
     * @since 0.0.5
     */
    public LocalizedReply asAnswerCallbackQuery() {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, true, this.callbackAlert, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code LocalizedReply} with {@code showAlert = true} on the
     * {@code AnswerCallbackQuery} call, displaying an alert dialog instead of a toast.
     * Implicitly activates {@code answerCallbackQuery}.
     *
     * @return a new {@code LocalizedReply} with the alert flag set
     * @since 0.0.5
     */
    public LocalizedReply withCallbackAlert() {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, true, true, this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code LocalizedReply} with the given URL set on the
     * {@code AnswerCallbackQuery} call. Implicitly activates {@code answerCallbackQuery}.
     *
     * @param url the URL to open; must not be {@code null}
     * @return a new {@code LocalizedReply} with the callback URL set
     * @since 0.0.5
     */
    public LocalizedReply withCallbackUrl(String url) {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, true, this.callbackAlert, url, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code LocalizedReply} with the given cache time set on the
     * {@code AnswerCallbackQuery} call. Implicitly activates {@code answerCallbackQuery}.
     *
     * @param cacheTime cache duration in seconds; must be non-negative
     * @return a new {@code LocalizedReply} with the cache time set
     * @since 0.0.5
     */
    public LocalizedReply withCallbackCacheTime(int cacheTime) {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, true, this.callbackAlert, this.callbackUrl, cacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns a new {@code LocalizedReply} with the Telegram parse mode set.
     *
     * <p>Typical values: {@code "HTML"}, {@code "MarkdownV2"}, {@code "Markdown"}.</p>
     *
     * @param parseMode the parse mode string; may be {@code null}
     * @return a new {@code LocalizedReply} with the parse mode set
     * @since 0.0.6
     */
    @Override
    public LocalizedReply withParseMode(String parseMode) {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, this.answerCallbackQuery, this.callbackAlert,
                this.callbackUrl, this.callbackCacheTime, parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
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
     * originating callback query using the resolved i18n message as the popup text.
     *
     * @return {@code true} to answer the originating callback query
     * @since 0.0.5
     */
    public boolean isAnswerCallbackQuery() {
        return answerCallbackQuery;
    }

    /**
     * Returns {@code true} if the {@code AnswerCallbackQuery} call should display an alert
     * dialog instead of a toast notification ({@code showAlert = true}).
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
     * Returns whether the message should be sent silently, or {@code null} for default.
     *
     * @return {@code true} for silent send; {@code null} for default
     * @since 0.0.6
     */
    public Boolean getDisableNotification() {
        return disableNotification;
    }

    /**
     * Returns a new {@code LocalizedReply} with the given silent-send flag set.
     *
     * @param disableNotification {@code true} to send silently; {@code null} for default
     * @return a new {@code LocalizedReply} with the flag set
     * @since 0.0.6
     */
    public LocalizedReply withDisableNotification(Boolean disableNotification) {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, this.answerCallbackQuery, this.callbackAlert,
                this.callbackUrl, this.callbackCacheTime, this.parseMode,
                disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns whether the message content is protected from forwarding and saving.
     *
     * @return {@code true} if content is protected; {@code null} for default
     * @since 0.0.6
     */
    public Boolean getProtectContent() {
        return protectContent;
    }

    /**
     * Returns a new {@code LocalizedReply} with the given protect-content flag set.
     *
     * @param protectContent {@code true} to protect; {@code null} for default
     * @return a new {@code LocalizedReply} with the flag set
     * @since 0.0.6
     */
    public LocalizedReply withProtectContent(Boolean protectContent) {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, this.answerCallbackQuery, this.callbackAlert,
                this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, protectContent, this.messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns the forum topic thread ID, or {@code null} for regular chats.
     *
     * @return the thread ID; may be {@code null}
     * @since 0.0.6
     */
    public Integer getMessageThreadId() {
        return messageThreadId;
    }

    /**
     * Returns a new {@code LocalizedReply} with the given forum topic thread ID set.
     *
     * @param messageThreadId the thread ID; {@code null} for regular chats
     * @return a new {@code LocalizedReply} with the thread ID set
     * @since 0.0.6
     */
    public LocalizedReply withMessageThreadId(Integer messageThreadId) {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, this.answerCallbackQuery, this.callbackAlert,
                this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, messageThreadId, this.replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns the reply-to parameters, or {@code null} if this message is not a reply.
     *
     * @return the reply parameters; may be {@code null}
     * @since 0.0.6
     */
    public ReplyParameters getReplyParameters() {
        return replyParameters;
    }

    /**
     * Returns a new {@code LocalizedReply} configured to appear as a reply to a specific message.
     *
     * @param replyParameters the reply parameters; {@code null} to send without replying
     * @return a new {@code LocalizedReply} with reply parameters set
     * @since 0.0.6
     */
    public LocalizedReply withReplyParameters(ReplyParameters replyParameters) {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, this.answerCallbackQuery, this.callbackAlert,
                this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, replyParameters, this.linkPreviewOptions);
    }

    /**
     * Returns the link preview options, or {@code null} for Telegram's default preview.
     *
     * @return the link preview options; may be {@code null}
     * @since 0.0.6
     */
    public LinkPreviewOptions getLinkPreviewOptions() {
        return linkPreviewOptions;
    }

    /**
     * Returns a new {@code LocalizedReply} with the given link preview options set.
     *
     * @param linkPreviewOptions the link preview options; {@code null} for default preview
     * @return a new {@code LocalizedReply} with link preview options set
     * @since 0.0.6
     */
    public LocalizedReply withLinkPreviewOptions(LinkPreviewOptions linkPreviewOptions) {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard,
                this.removeMarkup, this.editMessage, this.answerCallbackQuery, this.callbackAlert,
                this.callbackUrl, this.callbackCacheTime, this.parseMode,
                this.disableNotification, this.protectContent, this.messageThreadId, this.replyParameters, linkPreviewOptions);
    }
}

