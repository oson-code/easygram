package uz.osoncode.easygram.core.returntypehandler;

import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.ReplyParameters;

/**
 * Value object that groups Telegram {@code sendMessage} delivery options for use by
 * {@link BotReplyMessageHelper}.
 *
 * <p>Return-type handlers construct this record from the reply object fields and pass it to
 * {@code BotReplyMessageHelper.addReply()}, keeping the helper's parameter list stable as new
 * delivery options are added.</p>
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@link #parseMode} — Telegram parse mode ({@code "HTML"}, {@code "MarkdownV2"}, …)</li>
 *   <li>{@link #disableNotification} — send the message silently (no sound/vibration)</li>
 *   <li>{@link #protectContent} — prevent forwarding and saving the message</li>
 *   <li>{@link #messageThreadId} — forum topic thread ID (supergroups with topics only)</li>
 *   <li>{@link #replyParameters} — reply to a specific message</li>
 *   <li>{@link #linkPreviewOptions} — control link preview (disable or customise)</li>
 * </ul>
 *
 * @param parseMode           Telegram parse-mode string; {@code null} means no formatting
 * @param disableNotification send message silently; {@code null} means default (with sound)
 * @param protectContent      disable forwarding/saving; {@code null} means default
 * @param messageThreadId     forum topic thread ID; {@code null} for regular chats
 * @param replyParameters     reply-to configuration; {@code null} means no reply
 * @param linkPreviewOptions  link preview configuration; {@code null} means default preview
 *
 * @author Islom Mirsaburov
 * @since 0.0.7
 */
public record SendReplyOptions(
        String parseMode,
        Boolean disableNotification,
        Boolean protectContent,
        Integer messageThreadId,
        ReplyParameters replyParameters,
        LinkPreviewOptions linkPreviewOptions
) {

    /**
     * A {@code SendReplyOptions} with all fields {@code null} / default.
     * Use as a no-op placeholder when no delivery options are required.
     */
    public static final SendReplyOptions NONE = new SendReplyOptions(null, null, null, null, null, null);

    /**
     * Creates a {@code SendReplyOptions} with only a parse mode set.
     *
     * @param parseMode the Telegram parse mode; may be {@code null}
     * @return a new {@code SendReplyOptions} instance
     */
    public static SendReplyOptions of(String parseMode) {
        return new SendReplyOptions(parseMode, null, null, null, null, null);
    }
}
