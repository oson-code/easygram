package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to one or more plain-text Telegram messages matched by regular expression.
 *
 * <p>The annotated method is invoked when an incoming message's text matches <em>any</em> of the
 * regular expressions declared in {@link #value()}. Matching is performed via
 * {@link java.util.regex.Matcher#find()} so patterns do not need to anchor the full string by
 * default; use {@code ^} and {@code $} anchors when exact full-string matching is required.</p>
 *
 * <p>This annotation complements {@link BotText} (exact/literal matching) and
 * {@link BotTextDefault} (catch-all fallback). Use {@code @BotTextPattern} when the trigger
 * condition is dynamic or structural, e.g. a phone number, order ID, or free-form input
 * following a known prefix.</p>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // Matches any 10-digit phone number
 * @BotTextPattern("^\\d{10}$")
 * public String handlePhone(@BotTextValue String phone) {
 *     return "Got phone: " + phone;
 * }
 *
 * // Matches messages starting with "buy" or "order" (case-sensitive)
 * @BotTextPattern({"^buy .+", "^order .+"})
 * public String handleOrder(@BotTextValue String text) {
 *     return "Processing: " + text;
 * }
 * }</pre>
 *
 * <p>The full message text can be injected into the handler method via
 * {@link BotTextValue @BotTextValue}. Compiled {@link java.util.regex.Pattern} objects are
 * cached by the resolver, so each distinct pattern string is compiled only once per
 * application lifetime.</p>
 *
 * @author Islom Mirsaburov
 * @see BotText
 * @see BotTextDefault
 * @see BotTextValue
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotTextPattern {

    /**
     * One or more regular expression patterns that trigger this handler.
     *
     * <p>The handler method is invoked when the incoming message text matches <em>any</em>
     * of the listed patterns via {@link java.util.regex.Matcher#find()}.</p>
     *
     * @return an array of regex pattern strings; must not be empty
     */
    String[] value();
}
