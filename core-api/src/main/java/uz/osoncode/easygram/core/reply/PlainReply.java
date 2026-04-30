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
 * <p>Internally, all non-content fields ({@code markup}, {@code keyboard}, {@code parseMode},
 * delivery options, and callback-query options) are stored in an immutable {@link ReplyOptions}
 * instance. Dispatch to the actual Telegram Bot API method(s) is performed by the
 * {@code BotReplyActionChain} — a sorted list of {@code BotReplyAction} beans each responsible
 * for one kind of Bot API call ({@code sendMessage}, {@code editMessageText},
 * {@code answerCallbackQuery}, or any custom action).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public final class PlainReply implements MarkupAware {

    private final String text;
    private final Object[] args;
    private final ReplyOptions options;

    private PlainReply(String text, Object[] args, ReplyOptions options) {
        this.text = text;
        this.args = args;
        this.options = options;
    }

    /**
     * Returns the {@link ReplyOptions} carried by this reply.
     * Provides access to all shared options (markup, delivery, callback, behaviour).
     *
     * @return the options; never {@code null}
     * @since 0.0.6
     */
    public ReplyOptions getOptions() {
        return options;
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
        private ReplyOptions options = ReplyOptions.DEFAULTS;

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
         * @since 0.0.6
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
            options = new ReplyOptions(options.markupId(), options.markupParams(), keyboard,
                    options.removeMarkup(), options.editMessage(), options.answerCallbackQuery(),
                    options.callbackAlert(), options.callbackUrl(), options.callbackCacheTime(),
                    options.parseMode(), options.disableNotification(), options.protectContent(),
                    options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
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
         * Activates {@code AnswerCallbackQuery} emission. The reply text is used as the
         * popup text. Has no effect when the update is not a callback query.
         *
         * @param answerCallbackQuery {@code true} to answer the callback query
         * @return this builder
         * @since 0.0.5
         */
        public Builder answerCallbackQuery(boolean answerCallbackQuery) {
            options = new ReplyOptions(options.markupId(), options.markupParams(), options.keyboard(),
                    options.removeMarkup(), options.editMessage(), answerCallbackQuery,
                    options.callbackAlert(), options.callbackUrl(), options.callbackCacheTime(),
                    options.parseMode(), options.disableNotification(), options.protectContent(),
                    options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
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
            if (callbackAlert) {
                options = options.withCallbackAlert();
            } else {
                options = new ReplyOptions(options.markupId(), options.markupParams(), options.keyboard(),
                        options.removeMarkup(), options.editMessage(), options.answerCallbackQuery(),
                        false, options.callbackUrl(), options.callbackCacheTime(),
                        options.parseMode(), options.disableNotification(), options.protectContent(),
                        options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
            }
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
            options = callbackUrl != null ? options.withCallbackUrl(callbackUrl)
                    : new ReplyOptions(options.markupId(), options.markupParams(), options.keyboard(),
                            options.removeMarkup(), options.editMessage(), options.answerCallbackQuery(),
                            options.callbackAlert(), null, options.callbackCacheTime(),
                            options.parseMode(), options.disableNotification(), options.protectContent(),
                            options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
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
            options = callbackCacheTime != null ? options.withCallbackCacheTime(callbackCacheTime)
                    : new ReplyOptions(options.markupId(), options.markupParams(), options.keyboard(),
                            options.removeMarkup(), options.editMessage(), options.answerCallbackQuery(),
                            options.callbackAlert(), options.callbackUrl(), null,
                            options.parseMode(), options.disableNotification(), options.protectContent(),
                            options.messageThreadId(), options.replyParameters(), options.linkPreviewOptions());
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
            options = options.withParseMode(parseMode);
            return this;
        }

        /**
         * Sets whether to send the message silently (no sound or vibration on the receiver's device).
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
         * Sets the forum topic thread ID. Only applicable in supergroups with forum topics enabled.
         *
         * @param messageThreadId the thread ID; {@code null} for regular (non-threaded) chats
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
         * Builds and returns the immutable {@link PlainReply}.
         *
         * @return a new {@code PlainReply} instance
         */
        public PlainReply build() {
            return new PlainReply(text, args, options);
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
        return new PlainReply(text, null, ReplyOptions.DEFAULTS);
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
     * @since 0.0.6
     */
    public static PlainReply of(String text, Object... args) {
        Objects.requireNonNull(text, "text must not be null");
        return new PlainReply(text, args, ReplyOptions.DEFAULTS);
    }

    /**
     * Returns a new {@code PlainReply} with the same text and the given markup ID.
     *
     * @param markupId the ID of a pre-registered markup; must not be {@code null}
     * @return a new {@code PlainReply} with the markup ID set
     */
    @Override
    public PlainReply withMarkup(String markupId) {
        return new PlainReply(this.text, this.args, options.withMarkupId(markupId));
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
        return new PlainReply(this.text, this.args, options.withMarkupId(markupId, params));
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
        return new PlainReply(this.text, this.args, options.withKeyboard(keyboard));
    }

    /**
     * Returns a new {@code PlainReply} with the instruction to remove the markup.
     *
     * @return a new {@code PlainReply} with the remove-markup flag set
     */
    @Override
    public PlainReply removeMarkup() {
        return new PlainReply(this.text, this.args, options.withRemoveMarkup());
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
        return new PlainReply(this.text, this.args, options.withEditMessage());
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
        return new PlainReply(this.text, this.args, options.withAnswerCallbackQuery());
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
        return new PlainReply(this.text, this.args, options.withCallbackAlert());
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
        return new PlainReply(this.text, this.args, options.withCallbackUrl(url));
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
        return new PlainReply(this.text, this.args, options.withCallbackCacheTime(cacheTime));
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
        return new PlainReply(this.text, this.args, options.withParseMode(parseMode));
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
     * @since 0.0.6
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
     * @since 0.0.6
     */
    public PlainReply withArgs(Object... args) {
        return new PlainReply(this.text, args, this.options);
    }

    /**
     * Returns the markup ID, or {@code null} if none was set.
     *
     * @return the markup ID; may be {@code null}
     */
    @Override
    public String getMarkupId() {
        return options.markupId();
    }

    /**
     * Returns the markup factory parameters, or {@code null} if none were set.
     *
     * @return the params map; may be {@code null}
     */
    @Override
    public Map<String, Object> getMarkupParams() {
        return options.markupParams();
    }

    /**
     * Returns the directly-set {@link ReplyKeyboard}, or {@code null} if none was set.
     *
     * @return the keyboard; may be {@code null}
     */
    @Override
    public ReplyKeyboard getKeyboard() {
        return options.keyboard();
    }

    /**
     * Returns {@code true} if this reply instructs to remove the current markup.
     *
     * @return {@code true} if markup should be removed
     */
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
     * originating callback query using this reply's text as the popup notification text.
     *
     * @return {@code true} to answer the originating callback query
     * @since 0.0.5
     */
    public boolean isAnswerCallbackQuery() {
        return options.answerCallbackQuery();
    }

    /**
     * Returns {@code true} if the {@code AnswerCallbackQuery} call should display an alert dialog
     * instead of a toast notification ({@code showAlert = true}).
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
     * Returns whether the message should be sent silently (no sound/vibration), or {@code null}
     * to use Telegram's default.
     *
     * @return {@code true} for silent send; {@code null} for default
     * @since 0.0.6
     */
    public Boolean getDisableNotification() {
        return options.disableNotification();
    }

    /**
     * Returns a new {@code PlainReply} with the given silent-send flag set.
     *
     * @param disableNotification {@code true} to send silently; {@code null} for default
     * @return a new {@code PlainReply} with the flag set
     * @since 0.0.6
     */
    public PlainReply withDisableNotification(Boolean disableNotification) {
        return new PlainReply(this.text, this.args, options.withDisableNotification(disableNotification));
    }

    /**
     * Returns whether the message content is protected from forwarding and saving, or {@code null}
     * to use Telegram's default.
     *
     * @return {@code true} if content is protected; {@code null} for default
     * @since 0.0.6
     */
    public Boolean getProtectContent() {
        return options.protectContent();
    }

    /**
     * Returns a new {@code PlainReply} with the given protect-content flag set.
     *
     * @param protectContent {@code true} to protect; {@code null} for default
     * @return a new {@code PlainReply} with the flag set
     * @since 0.0.6
     */
    public PlainReply withProtectContent(Boolean protectContent) {
        return new PlainReply(this.text, this.args, options.withProtectContent(protectContent));
    }

    /**
     * Returns the forum topic thread ID, or {@code null} for regular (non-threaded) chats.
     *
     * @return the thread ID; may be {@code null}
     * @since 0.0.6
     */
    public Integer getMessageThreadId() {
        return options.messageThreadId();
    }

    /**
     * Returns a new {@code PlainReply} with the given forum topic thread ID set.
     *
     * @param messageThreadId the thread ID; {@code null} for regular chats
     * @return a new {@code PlainReply} with the thread ID set
     * @since 0.0.6
     */
    public PlainReply withMessageThreadId(Integer messageThreadId) {
        return new PlainReply(this.text, this.args, options.withMessageThreadId(messageThreadId));
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
     * Returns a new {@code PlainReply} configured to appear as a reply to a specific message.
     *
     * @param replyParameters the reply parameters; {@code null} to send without replying
     * @return a new {@code PlainReply} with reply parameters set
     * @since 0.0.6
     */
    public PlainReply withReplyParameters(ReplyParameters replyParameters) {
        return new PlainReply(this.text, this.args, options.withReplyParameters(replyParameters));
    }

    /**
     * Returns the link preview options, or {@code null} to use Telegram's default preview.
     *
     * @return the link preview options; may be {@code null}
     * @since 0.0.6
     */
    public LinkPreviewOptions getLinkPreviewOptions() {
        return options.linkPreviewOptions();
    }

    /**
     * Returns a new {@code PlainReply} with the given link preview options set.
     *
     * @param linkPreviewOptions the link preview options; {@code null} for default preview
     * @return a new {@code PlainReply} with link preview options set
     * @since 0.0.6
     */
    public PlainReply withLinkPreviewOptions(LinkPreviewOptions linkPreviewOptions) {
        return new PlainReply(this.text, this.args, options.withLinkPreviewOptions(linkPreviewOptions));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlainReply that)) return false;
        return Objects.equals(text, that.text)
                && java.util.Arrays.equals(args, that.args)
                && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(text, options);
        result = 31 * result + java.util.Arrays.hashCode(args);
        return result;
    }

    @Override
    public String toString() {
        return "PlainReply{text='" + text + "', args=" + java.util.Arrays.toString(args)
                + ", options=" + options + '}';
    }
}
