package uz.osoncode.easygram.core.handler.message.text.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotTextPattern;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Metadata resolver for the {@link BotTextPattern} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a plain-text message whose
 * content matches <em>any</em> of the regular expression patterns declared in the
 * {@link BotTextPattern} annotation. Matching is performed via
 * {@link java.util.regex.Matcher#find()}, so patterns need not anchor the full string by default.
 *
 * <p>To avoid the cost of recompiling patterns on every request, compiled
 * {@link Pattern} objects are cached in a {@link ConcurrentHashMap} keyed by pattern string.
 * Each distinct pattern string is compiled at most once per application lifetime.
 *
 * <h2>Registration priority</h2>
 * <p>This resolver participates in the same handler-selection competition as
 * {@link BotTextMetaDataResolver} (exact-match) and is registered as a standard
 * {@code @Bean} in {@code CoreAutoConfiguration}. Handlers are ordered and selected by
 * {@link uz.osoncode.easygram.core.handler.BotHandlerRegistry} priority rules —
 * handlers annotated with {@code @BotOrder} can control relative precedence.
 *
 * @author Islom Mirsaburov
 * @see BotTextPattern
 * @see BotTextMetaDataResolver
 * @since 0.0.1
 */
public class BotTextPatternMetaDataResolver implements BotMetaDataSpecResolver<BotTextPattern> {

    /**
     * Bounded LRU cache of compiled {@link Pattern} objects, keyed by their source regex string.
     * Capped at 512 entries; least-recently-used patterns are evicted when the limit is exceeded,
     * preventing unbounded heap growth from dynamic or user-supplied pattern strings.
     */
    private final Map<String, Pattern> patternCache = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Pattern> eldest) {
                    return size() > 512;
                }
            });

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotTextPattern} annotation class.
     */
    @Override
    public Class<BotTextPattern> getAnnotationType() {
        return BotTextPattern.class;
    }

    /**
     * Returns {@code true} if the update contains a text message that matches any of the
     * regular expression patterns declared in the {@link BotTextPattern} annotation.
     *
     * <p>Pattern compilation results are cached. Matching uses
     * {@link java.util.regex.Matcher#find()}, so a pattern like {@code "\\d{10}"} matches
     * a 10-digit sequence anywhere in the text. Use {@code ^} and {@code $} anchors for
     * full-string matching.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotTextPattern} annotation declared on the handler method.
     * @return {@code true} if the message text matches any pattern; {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotTextPattern annotation) {
        if (!botRequest.getUpdate().hasMessage() || !botRequest.getUpdate().getMessage().hasText()) {
            return false;
        }
        String messageText = botRequest.getUpdate().getMessage().getText();
        for (String regex : annotation.value()) {
            Pattern pattern;
            synchronized (patternCache) {
                pattern = patternCache.computeIfAbsent(regex, Pattern::compile);
            }
            if (pattern.matcher(messageText).find()) {
                return true;
            }
        }
        return false;
    }
}
