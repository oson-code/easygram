package uz.osoncode.easygram.core.handler.inlinequery.metadataresolver;

import lombok.RequiredArgsConstructor;
import uz.osoncode.easygram.core.bind.annotation.BotInlineQuery;
import uz.osoncode.easygram.core.handler.inlinequery.BotInlineQueryMatcher;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotInlineQuery} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains an inline query that should
 * be routed to a {@code @BotInlineQuery}-annotated handler. When
 * {@link BotInlineQuery#value()} is empty all inline queries are matched; when non-empty
 * the actual matching logic is delegated to the configured {@link BotInlineQueryMatcher}
 * strategy, making the resolver independent of whether the annotation values are exact
 * strings or message-bundle keys.</p>
 *
 * <p>The default matcher (registered by {@code CoreAutoConfiguration}) performs exact
 * case-sensitive text comparison. When {@code core-i18n} is on the classpath it registers
 * a locale-aware matcher that resolves annotation values as message-bundle keys before
 * comparing, so a single {@code @BotInlineQuery("search.key")} covers all locales.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotInlineQueryMetaDataResolver implements BotMetaDataSpecResolver<BotInlineQuery> {

    private final BotInlineQueryMatcher matcher;

    @Override
    public Class<BotInlineQuery> getAnnotationType() {
        return BotInlineQuery.class;
    }

    @Override
    public boolean support(BotRequest botRequest, BotInlineQuery annotation) {
        if (!botRequest.getUpdate().hasInlineQuery()) {
            return false;
        }
        if (annotation.value().length == 0) {
            return true;
        }
        return matcher.matches(annotation.value(), botRequest);
    }
}

