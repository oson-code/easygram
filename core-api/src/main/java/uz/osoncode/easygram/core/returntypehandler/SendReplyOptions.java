package uz.osoncode.easygram.core.returntypehandler;

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
 * </ul>
 *
 * @param parseMode Telegram parse-mode string; {@code null} means no formatting
 *
 * @author Islom Mirsaburov
 * @since 0.0.7
 */
public record SendReplyOptions(
        String parseMode
) {

    /**
     * A {@code SendReplyOptions} with all fields {@code null} / default.
     * Use as a no-op placeholder when no delivery options are required.
     */
    public static final SendReplyOptions NONE = new SendReplyOptions(null);

    /**
     * Creates a {@code SendReplyOptions} with only a parse mode set.
     *
     * @param parseMode the Telegram parse mode; may be {@code null}
     * @return a new {@code SendReplyOptions} instance
     */
    public static SendReplyOptions of(String parseMode) {
        return new SendReplyOptions(parseMode);
    }
}
