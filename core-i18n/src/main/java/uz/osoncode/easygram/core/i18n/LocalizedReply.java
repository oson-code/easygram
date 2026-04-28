package uz.osoncode.easygram.core.i18n;

import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.ReplyParameters;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.markup.MarkupAware;
import uz.osoncode.easygram.core.reply.ReplyOptions;

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
    private final ReplyOptions options;

    private LocalizedReply(String key, Object[] args, ReplyOptions options) {
        this.key = key;
        this.args = args;
        this.options = options;
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
        private ReplyOptions options = ReplyOptions.DEFAULTS;

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
            options = options.withMarkupId(markupId);
            return this;
        }

        /**
         * Sets the markup factory parameters forwarded to the {@code @BotMarkup} factory.
         *
         * @param markupParams the parameters map
         * @return this builder
         */
        public Builder markupParams(Map<String, Object> markupParams) {
            options = new ReplyOptions(options.markupId(), markupParams, options.keyboard(),
                    options.removeMarkup(), options.editMessage(), options.answerCallbackQuery(),
                    options.callbackAlert(), options.callbackUrl(), options.callbackCacheTime(),
                    options.parseMode(), options.disableNotification(), options.protectContent(),
                    options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
            return this;
        }

        /**
         * Sets a directly-built keyboard (takes precedence over {@link #markupId}).
         *
         * @param keyboard the keyboard to attach
         * @return this builder
         */
        public Builder keyboard(ReplyKeyboard keyboard) {
            options = options.withKeyboard(keyboard);
            return this;
        }

        /**
         * Instructs the framework to send a {@code ReplyKeyboardRemove}.
         *
         * @return this builder
         */
        public Builder removeMarkup() {
            options = options.withRemoveMarkup();
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
            options = editMessage ? options.withEditMessage()
                    : new ReplyOptions(options.markupId(), options.markupParams(), options.keyboard(),
                            options.removeMarkup(), false, options.answerCallbackQuery(),
                            options.callbackAlert(), options.callbackUrl(), options.callbackCacheTime(),
                            options.parseMode(), options.disableNotification(), options.protectContent(),
                            options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
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
            options = answerCallbackQuery ? options.withAnswerCallbackQuery()
                    : new ReplyOptions(options.markupId(), options.markupParams(), options.keyboard(),
                            options.removeMarkup(), options.editMessage(), false,
                            options.callbackAlert(), options.callbackUrl(), options.callbackCacheTime(),
                            options.parseMode(), options.disableNotification(), options.protectContent(),
                            options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
            return this;
        }

        /**
         * Sets {@code showAlert = true} on the {@code AnswerCallbackQuery} call. Implicitly
         * sets {@code answerCallbackQuery = true}.
         *
         * @param callbackAlert {@code true} to show an alert dialog
         * @return this builder
         * @since 0.0.5
         */
        public Builder callbackAlert(boolean callbackAlert) {
            options = callbackAlert ? options.withCallbackAlert()
                    : new ReplyOptions(options.markupId(), options.markupParams(), options.keyboard(),
                            options.removeMarkup(), options.editMessage(), options.answerCallbackQuery(),
                            false, options.callbackUrl(), options.callbackCacheTime(),
                            options.parseMode(), options.disableNotification(), options.protectContent(),
                            options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
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
            options = options.withCallbackUrl(callbackUrl);
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
            options = options.withCallbackCacheTime(callbackCacheTime);
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
            options = options.withParseMode(parseMode);
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
            options = options.withDisableNotification(disableNotification);
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
            options = options.withProtectContent(protectContent);
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
            options = options.withMessageThreadId(messageThreadId);
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
            options = options.withReplyParameters(replyParameters);
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
            options = options.withLinkPreviewOptions(linkPreviewOptions);
            return this;
        }

        /**
         * Builds and returns the immutable {@link LocalizedReply}.
         *
         * @return a new {@code LocalizedReply} instance
         */
        public LocalizedReply build() {
            Objects.requireNonNull(key, "key must not be null");
            return new LocalizedReply(key, args, options);
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
        return new LocalizedReply(key, args, ReplyOptions.DEFAULTS);
    }

    @Override
    public LocalizedReply withMarkup(String markupId) {
        return new LocalizedReply(this.key, this.args, options.withMarkupId(markupId));
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
        return new LocalizedReply(this.key, this.args,
                new ReplyOptions(markupId, params, null, false, options.editMessage(),
                        options.answerCallbackQuery(), options.callbackAlert(), options.callbackUrl(),
                        options.callbackCacheTime(), options.parseMode(), options.disableNotification(),
                        options.protectContent(), options.messageThreadId(), options.replyParameters(),
                        options.linkPreviewOptions()));
    }

    /**
     * Returns a new {@code LocalizedReply} with the given {@link ReplyKeyboard} attached directly.
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code LocalizedReply} with the keyboard set
     */
    @Override
    public LocalizedReply withKeyboard(ReplyKeyboard keyboard) {
        return new LocalizedReply(this.key, this.args, options.withKeyboard(keyboard));
    }

    @Override
    public LocalizedReply removeMarkup() {
        return new LocalizedReply(this.key, this.args, options.withRemoveMarkup());
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
        return new LocalizedReply(this.key, this.args, options.withEditMessage());
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
        return new LocalizedReply(this.key, this.args, options.withAnswerCallbackQuery());
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
        return new LocalizedReply(this.key, this.args, options.withCallbackAlert());
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
        return new LocalizedReply(this.key, this.args, options.withCallbackUrl(url));
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
        return new LocalizedReply(this.key, this.args, options.withCallbackCacheTime(cacheTime));
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
        return new LocalizedReply(this.key, this.args, options.withParseMode(parseMode));
    }

    /**
     * Returns the underlying {@link ReplyOptions} for this reply.
     *
     * @return the reply options; never {@code null}
     * @since 0.0.6
     */
    public ReplyOptions getOptions() {
        return options;
    }

    public String getKey() {
        return key;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public String getMarkupId() {
        return options.markupId();
    }

    @Override
    public Map<String, Object> getMarkupParams() {
        return options.markupParams();
    }

    @Override
    public ReplyKeyboard getKeyboard() {
        return options.keyboard();
    }

    @Override
    public boolean isRemoveMarkup() {
        return options.removeMarkup();
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
        return options.editMessage();
    }

    /**
     * Returns {@code true} if the framework should send an {@code AnswerCallbackQuery} for the
     * originating callback query using the resolved i18n message as the popup text.
     *
     * @return {@code true} to answer the originating callback query
     * @since 0.0.5
     */
    public boolean isAnswerCallbackQuery() {
        return options.answerCallbackQuery();
    }

    /**
     * Returns {@code true} if the {@code AnswerCallbackQuery} call should display an alert
     * dialog instead of a toast notification ({@code showAlert = true}).
     *
     * @return {@code true} for alert mode
     * @since 0.0.5
     */
    public boolean isCallbackAlert() {
        return options.callbackAlert();
    }

    /**
     * Returns the URL to be opened by the Telegram client when answering the callback query,
     * or {@code null} if none was set.
     *
     * @return the callback URL; may be {@code null}
     * @since 0.0.5
     */
    public String getCallbackUrl() {
        return options.callbackUrl();
    }

    /**
     * Returns the client-side cache duration in seconds for the {@code AnswerCallbackQuery}
     * response, or {@code null} if no cache time was specified.
     *
     * @return the cache time in seconds; may be {@code null}
     * @since 0.0.5
     */
    public Integer getCallbackCacheTime() {
        return options.callbackCacheTime();
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
        return options.parseMode();
    }

    /**
     * Returns whether the message should be sent silently, or {@code null} for default.
     *
     * @return {@code true} for silent send; {@code null} for default
     * @since 0.0.6
     */
    public Boolean getDisableNotification() {
        return options.disableNotification();
    }

    /**
     * Returns a new {@code LocalizedReply} with the given silent-send flag set.
     *
     * @param disableNotification {@code true} to send silently; {@code null} for default
     * @return a new {@code LocalizedReply} with the flag set
     * @since 0.0.6
     */
    public LocalizedReply withDisableNotification(Boolean disableNotification) {
        return new LocalizedReply(this.key, this.args, options.withDisableNotification(disableNotification));
    }

    /**
     * Returns whether the message content is protected from forwarding and saving.
     *
     * @return {@code true} if content is protected; {@code null} for default
     * @since 0.0.6
     */
    public Boolean getProtectContent() {
        return options.protectContent();
    }

    /**
     * Returns a new {@code LocalizedReply} with the given protect-content flag set.
     *
     * @param protectContent {@code true} to protect; {@code null} for default
     * @return a new {@code LocalizedReply} with the flag set
     * @since 0.0.6
     */
    public LocalizedReply withProtectContent(Boolean protectContent) {
        return new LocalizedReply(this.key, this.args, options.withProtectContent(protectContent));
    }

    /**
     * Returns the forum topic thread ID, or {@code null} for regular chats.
     *
     * @return the thread ID; may be {@code null}
     * @since 0.0.6
     */
    public Integer getMessageThreadId() {
        return options.messageThreadId();
    }

    /**
     * Returns a new {@code LocalizedReply} with the given forum topic thread ID set.
     *
     * @param messageThreadId the thread ID; {@code null} for regular chats
     * @return a new {@code LocalizedReply} with the thread ID set
     * @since 0.0.6
     */
    public LocalizedReply withMessageThreadId(Integer messageThreadId) {
        return new LocalizedReply(this.key, this.args, options.withMessageThreadId(messageThreadId));
    }

    /**
     * Returns the reply-to parameters, or {@code null} if this message is not a reply.
     *
     * @return the reply parameters; may be {@code null}
     * @since 0.0.6
     */
    public ReplyParameters getReplyParameters() {
        return options.replyParameters();
    }

    /**
     * Returns a new {@code LocalizedReply} configured to appear as a reply to a specific message.
     *
     * @param replyParameters the reply parameters; {@code null} to send without replying
     * @return a new {@code LocalizedReply} with reply parameters set
     * @since 0.0.6
     */
    public LocalizedReply withReplyParameters(ReplyParameters replyParameters) {
        return new LocalizedReply(this.key, this.args, options.withReplyParameters(replyParameters));
    }

    /**
     * Returns the link preview options, or {@code null} for Telegram's default preview.
     *
     * @return the link preview options; may be {@code null}
     * @since 0.0.6
     */
    public LinkPreviewOptions getLinkPreviewOptions() {
        return options.linkPreviewOptions();
    }

    /**
     * Returns a new {@code LocalizedReply} with the given link preview options set.
     *
     * @param linkPreviewOptions the link preview options; {@code null} for default preview
     * @return a new {@code LocalizedReply} with link preview options set
     * @since 0.0.6
     */
    public LocalizedReply withLinkPreviewOptions(LinkPreviewOptions linkPreviewOptions) {
        return new LocalizedReply(this.key, this.args, options.withLinkPreviewOptions(linkPreviewOptions));
    }
}
