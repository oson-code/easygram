package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Specifies the Telegram
 * <a href="https://core.telegram.org/bots/api#formatting-options">parse mode</a>
 * to apply to the outgoing message produced by the annotated handler method.
 *
 * <p>Supported values correspond directly to Telegram Bot API parse mode strings:</p>
 * <ul>
 *   <li>{@code "HTML"}</li>
 *   <li>{@code "MarkdownV2"}</li>
 *   <li>{@code "Markdown"} (legacy)</li>
 * </ul>
 *
 * <p>The annotation is applied to the outgoing {@code SendMessage} or {@code EditMessageText}
 * before it is dispatched to Telegram. It works with <em>all</em> handler return types:</p>
 * <ul>
 *   <li>{@link String}</li>
 *   <li>{@link uz.osoncode.easygram.core.reply.PlainReply}</li>
 *   <li>{@code LocalizedReply} (from {@code core-i18n})</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @BotCommand("/start")
 * @BotParseMode("HTML")
 * public String onStart(User user) {
 *     return "Hello, <b>" + user.getFirstName() + "</b>!";
 * }
 *
 * @BotText("markdown")
 * @BotParseMode("MarkdownV2")
 * public PlainReply onMarkdown() {
 *     return PlainReply.of("*bold* _italic_");
 * }
 * }</pre>
 *
 * <p>When combined with {@link BotReplyMarkup}, the parse mode and the markup are both applied
 * to the same outgoing message — the annotation order does not matter.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotParseMode {

    /**
     * The Telegram parse mode string to use.
     *
     * @return one of {@code "HTML"}, {@code "MarkdownV2"}, or {@code "Markdown"}
     */
    String value();
}
