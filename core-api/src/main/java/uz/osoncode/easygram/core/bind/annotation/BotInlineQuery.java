package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to inline queries sent to the bot.
 *
 * <p>Annotate a method with {@code @BotInlineQuery} to handle incoming inline queries.
 * When {@link #value()} is empty all inline queries are matched; when non-empty the
 * configured {@link uz.osoncode.easygram.core.handler.inlinequery.BotInlineQueryMatcher}
 * strategy is used to decide whether a query matches.</p>
 *
 * <h2>Default behaviour (without {@code core-i18n})</h2>
 * <p>Values are compared directly against the incoming query text (exact match):</p>
 * <pre>{@code
 * @BotInlineQuery("search")
 * public void onSearchInline(InlineQuery query, @BotInlineQueryValue String text) {
 *     // matches only when user types "@YourBot search"
 * }
 * }</pre>
 *
 * <h2>With {@code core-i18n} on the classpath</h2>
 * <p>Values are treated as <em>message-bundle keys</em>. The framework resolves each key in
 * the user's locale and compares the result against the incoming query text — so a single
 * annotation covers all supported languages automatically:</p>
 * <pre>{@code
 * // messages/bot_en.properties: inline.search=search
 * // messages/bot_ru.properties: inline.search=поиск
 * @BotInlineQuery("inline.search")
 * public void onSearchInline(InlineQuery query, @BotInlineQueryValue String text) {
 *     // matches "@YourBot search" for English users
 *     // and "@YourBot поиск" for Russian users
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see uz.osoncode.easygram.core.handler.inlinequery.BotInlineQueryMatcher
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotInlineQuery {
    /**
     * Optional filter values. An empty array matches all inline queries; a non-empty
     * array restricts matching to queries that satisfy the configured
     * {@link uz.osoncode.easygram.core.handler.inlinequery.BotInlineQueryMatcher}.
     *
     * <p>Without {@code core-i18n}: treated as exact query text strings.<br>
     * With {@code core-i18n}: treated as message-bundle keys resolved per user locale.</p>
     *
     * @return array of query text values or message-bundle keys to match
     */
    String[] value() default {};
}
