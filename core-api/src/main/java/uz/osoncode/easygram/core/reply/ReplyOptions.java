package uz.osoncode.easygram.core.reply;

import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.ReplyParameters;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.returntypehandler.SendReplyOptions;

import java.util.Map;

/**
 * Immutable value object that groups all shared non-content options for a bot reply.
 *
 * <p>Both {@link PlainReply} and {@code LocalizedReply} delegate to a {@code ReplyOptions}
 * instance internally. This means adding a new Telegram Bot API option only requires a new
 * field here — the reply classes themselves do not need to change.</p>
 *
 * <h2>Option groups</h2>
 * <dl>
 *   <dt>Markup</dt>
 *   <dd>{@link #markupId}, {@link #markupParams}, {@link #keyboard}, {@link #removeMarkup}</dd>
 *   <dt>Message behaviour</dt>
 *   <dd>{@link #editMessage}</dd>
 *   <dt>Callback query</dt>
 *   <dd>{@link #answerCallbackQuery}, {@link #callbackAlert}, {@link #callbackUrl},
 *       {@link #callbackCacheTime}</dd>
 *   <dt>Delivery</dt>
 *   <dd>{@link #parseMode}, {@link #disableNotification}, {@link #protectContent},
 *       {@link #messageThreadId}, {@link #replyParameters}, {@link #linkPreviewOptions}</dd>
 * </dl>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
public record ReplyOptions(
        String markupId,
        Map<String, Object> markupParams,
        ReplyKeyboard keyboard,
        boolean removeMarkup,
        boolean editMessage,
        boolean answerCallbackQuery,
        boolean callbackAlert,
        String callbackUrl,
        Integer callbackCacheTime,
        String parseMode,
        Boolean disableNotification,
        Boolean protectContent,
        Integer messageThreadId,
        ReplyParameters replyParameters,
        LinkPreviewOptions linkPreviewOptions) {

    /**
     * A {@code ReplyOptions} with all fields at their default / {@code null} values.
     * Use as the starting point when constructing a reply with no extra options.
     */
    public static final ReplyOptions DEFAULTS =
            new ReplyOptions(null, null, null, false, false, false, false, null, null,
                    null, null, null, null, null, null);

    // -------------------------------------------------------------------------
    // Markup withers
    // -------------------------------------------------------------------------

    /**
     * Returns a copy with the given markup ID set (no factory params).
     * Clears any previously set inline keyboard.
     *
     * @param markupId the pre-registered markup ID; must not be {@code null}
     * @return a new {@code ReplyOptions} with the markup ID set
     */
    public ReplyOptions withMarkupId(String markupId) {
        return new ReplyOptions(markupId, null, null, false,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the given markup ID and factory parameters set.
     * Clears any previously set inline keyboard.
     *
     * @param markupId the pre-registered markup ID; must not be {@code null}
     * @param params   the parameters to forward to the factory; must not be {@code null}
     * @return a new {@code ReplyOptions} with the markup ID and params set
     */
    public ReplyOptions withMarkupId(String markupId, Map<String, Object> params) {
        return new ReplyOptions(markupId, params, null, false,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the given {@link ReplyKeyboard} set directly.
     * Clears any previously set markup ID.
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code ReplyOptions} with the keyboard set
     */
    public ReplyOptions withKeyboard(ReplyKeyboard keyboard) {
        return new ReplyOptions(null, null, keyboard, false,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the remove-markup flag set.
     * Clears any previously set markup ID or inline keyboard.
     *
     * @return a new {@code ReplyOptions} with the remove-markup flag set
     */
    public ReplyOptions withRemoveMarkup() {
        return new ReplyOptions(null, null, null, true,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    // -------------------------------------------------------------------------
    // Message behaviour withers
    // -------------------------------------------------------------------------

    /**
     * Returns a copy with the {@code editMessage} flag set to {@code true}.
     *
     * @return a new {@code ReplyOptions} with {@code editMessage = true}
     */
    public ReplyOptions withEditMessage() {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                true, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    // -------------------------------------------------------------------------
    // Callback withers
    // -------------------------------------------------------------------------

    /**
     * Returns a copy with {@code answerCallbackQuery = true}.
     *
     * @return a new {@code ReplyOptions} with the answer-callback flag set
     */
    public ReplyOptions withAnswerCallbackQuery() {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, true, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with {@code callbackAlert = true} and {@code answerCallbackQuery = true}.
     *
     * @return a new {@code ReplyOptions} with the alert flag set
     */
    public ReplyOptions withCallbackAlert() {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, true, true, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the given callback URL set and {@code answerCallbackQuery = true}.
     *
     * @param url the URL to open; must not be {@code null}
     * @return a new {@code ReplyOptions} with the callback URL set
     */
    public ReplyOptions withCallbackUrl(String url) {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, true, callbackAlert, url, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the given cache time set and {@code answerCallbackQuery = true}.
     *
     * @param cacheTime cache duration in seconds; must be non-negative
     * @return a new {@code ReplyOptions} with the cache time set
     */
    public ReplyOptions withCallbackCacheTime(int cacheTime) {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, true, callbackAlert, callbackUrl, cacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    // -------------------------------------------------------------------------
    // Delivery withers
    // -------------------------------------------------------------------------

    /**
     * Returns a copy with the given Telegram parse mode set.
     *
     * <p>Typical values: {@code "HTML"}, {@code "MarkdownV2"}, {@code "Markdown"}.</p>
     *
     * @param parseMode the parse mode string; must not be {@code null}
     * @return a new {@code ReplyOptions} with the parse mode set
     */
    public ReplyOptions withParseMode(String parseMode) {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the given silent-send flag set.
     *
     * @param disableNotification {@code true} to send silently; {@code null} for default
     * @return a new {@code ReplyOptions} with the flag set
     */
    public ReplyOptions withDisableNotification(Boolean disableNotification) {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the given protect-content flag set.
     *
     * @param protectContent {@code true} to protect; {@code null} for default
     * @return a new {@code ReplyOptions} with the flag set
     */
    public ReplyOptions withProtectContent(Boolean protectContent) {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the given forum topic thread ID set.
     *
     * @param messageThreadId the thread ID; {@code null} for regular chats
     * @return a new {@code ReplyOptions} with the thread ID set
     */
    public ReplyOptions withMessageThreadId(Integer messageThreadId) {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy configured to appear as a reply to a specific message.
     *
     * @param replyParameters the reply parameters; {@code null} to send without replying
     * @return a new {@code ReplyOptions} with the reply parameters set
     */
    public ReplyOptions withReplyParameters(ReplyParameters replyParameters) {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    /**
     * Returns a copy with the given link preview options set.
     *
     * @param linkPreviewOptions the link preview options; {@code null} for default preview
     * @return a new {@code ReplyOptions} with link preview options set
     */
    public ReplyOptions withLinkPreviewOptions(LinkPreviewOptions linkPreviewOptions) {
        return new ReplyOptions(markupId, markupParams, keyboard, removeMarkup,
                editMessage, answerCallbackQuery, callbackAlert, callbackUrl, callbackCacheTime,
                parseMode, disableNotification, protectContent, messageThreadId, replyParameters, linkPreviewOptions);
    }

    // -------------------------------------------------------------------------
    // Conversion helper
    // -------------------------------------------------------------------------

    /**
     * Converts the delivery fields of this {@code ReplyOptions} to a {@link SendReplyOptions}
     * suitable for passing to {@code BotReplyMessageHelper}.
     *
     * @return a {@link SendReplyOptions} containing only the delivery-related fields
     */
    public SendReplyOptions toSendReplyOptions() {
        return new SendReplyOptions(parseMode, disableNotification, protectContent,
                messageThreadId, replyParameters, linkPreviewOptions);
    }
}
