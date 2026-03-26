package uz.osoncode.easygram.core.handler.inlinequery;

import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Strategy interface for matching a {@link uz.osoncode.easygram.core.bind.annotation.BotInlineQuery}
 * annotation against an incoming inline-query update.
 *
 * <p>The default implementation (registered in {@code core}) performs an exact string comparison
 * between each annotation value and the incoming query text.</p>
 *
 * <p>When {@code core-i18n} is on the classpath it registers a replacement implementation that
 * treats annotation values as message-bundle keys, resolves them in the user's locale before
 * comparing — so a single {@code @BotInlineQuery("search.query")} annotation handles all
 * configured languages automatically.</p>
 *
 * <p>Declare a bean of this type to provide a completely custom matching strategy:</p>
 * <pre>{@code
 * @Bean
 * public BotInlineQueryMatcher myMatcher() {
 *     return (values, request) -> {
 *         String query = request.getUpdate().getInlineQuery().getQuery();
 *         return Arrays.stream(values).anyMatch(v -> query.toLowerCase().contains(v.toLowerCase()));
 *     };
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.2
 * @see uz.osoncode.easygram.core.bind.annotation.BotInlineQuery
 */
@FunctionalInterface
public interface BotInlineQueryMatcher {

    /**
     * Returns {@code true} if the incoming inline query matches at least one of the annotation
     * values.
     *
     * <p>Implementations may assume that the update already contains an inline query —
     * the presence check is performed by the caller before this method is invoked.
     * An empty {@code values} array means "match all" and this method will not be called.</p>
     *
     * @param values  the values declared on the {@code @BotInlineQuery} annotation; never empty
     * @param request the current bot request; {@code request.getUpdate().getInlineQuery()} is non-null
     * @return {@code true} if the handler should process this request
     */
    boolean matches(String[] values, BotRequest request);
}
