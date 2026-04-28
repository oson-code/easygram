package uz.osoncode.easygram.core.i18n.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.handler.inlinequery.BotInlineQueryMatcher;
import uz.osoncode.easygram.core.handler.message.replybutton.BotReplyButtonMatcher;
import uz.osoncode.easygram.core.i18n.EasygramI18nProperties;
import uz.osoncode.easygram.core.i18n.BotLocaleResolver;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.filter.BotLocaleSetterFilter;
import uz.osoncode.easygram.core.i18n.keyboard.BotKeyboardFactory;
import uz.osoncode.easygram.core.i18n.resolver.BotLocaleArgumentResolver;
import uz.osoncode.easygram.core.i18n.resolver.UserLanguageCodeLocaleResolver;
import uz.osoncode.easygram.core.i18n.returntypehandler.BotLocalizedReplyReturnTypeHandler;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackQueryService;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReplyActionChain;

import java.util.Locale;
import java.util.Optional;

/**
 * Spring Boot auto-configuration for Easygram internationalisation support.
 *
 * <p>Registers the following beans (all replaceable via {@code @ConditionalOnMissingBean}):</p>
 * <ul>
 *   <li>{@link BotLocaleResolver} - resolves locale from {@code user.getLanguageCode()}
 *       (default: {@link UserLanguageCodeLocaleResolver})</li>
 *   <li>{@link BotMessageSource} - locale-aware wrapper around Spring's {@link MessageSource}</li>
 *   <li>{@link BotLocaleArgumentResolver} - injects {@link java.util.Locale} into handler methods</li>
 *   <li>{@link BotLocaleSetterFilter} - sets {@code LocaleContextHolder} for each request</li>
 *   <li>{@link BotKeyboardFactory} - builds localised InlineKeyboardMarkup / ReplyKeyboardMarkup</li>
 *   <li>{@link BotReplyButtonMatcher} - locale-aware matcher that replaces the default exact-text
 *       matcher in {@code core} for {@code @BotReplyButton} routing</li>
 *   <li>{@link BotInlineQueryMatcher} - locale-aware matcher that replaces the default exact-text
 *       matcher in {@code core} for {@code @BotInlineQuery} value routing</li>
 *   <li>{@link BotLocalizedReplyReturnTypeHandler} - resolves {@link uz.osoncode.easygram.core.i18n.LocalizedReply}
 *       return values using direct key lookup</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration(after = MessageSourceAutoConfiguration.class,
        beforeName = "uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration")
@ConditionalOnProperty(prefix = "easygram.i18n", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(EasygramI18nProperties.class)
public class BotI18nAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BotLocaleResolver.class)
    public BotLocaleResolver botLocaleResolver(EasygramI18nProperties properties) {
        return new UserLanguageCodeLocaleResolver(properties.defaultLocale());
    }

    @Bean
    @ConditionalOnMissingBean(BotMessageSource.class)
    public BotMessageSource botMessageSource(MessageSource messageSource,
                                             BotLocaleResolver localeResolver) {
        return new BotMessageSource(messageSource, localeResolver);
    }

    @Bean
    @ConditionalOnMissingBean(BotLocaleArgumentResolver.class)
    public BotLocaleArgumentResolver botLocaleArgumentResolver(BotLocaleResolver localeResolver) {
        return new BotLocaleArgumentResolver(localeResolver);
    }

    @Bean
    @ConditionalOnMissingBean(BotLocaleSetterFilter.class)
    public BotLocaleSetterFilter botLocaleSetterFilter(BotLocaleResolver localeResolver) {
        return new BotLocaleSetterFilter(localeResolver);
    }

    @Bean
    @ConditionalOnMissingBean(BotKeyboardFactory.class)
    public BotKeyboardFactory botKeyboardFactory(BotMessageSource botMessageSource,
                                                 Optional<BotDynamicCallbackQueryService> dynamicService) {
        return new BotKeyboardFactory(botMessageSource, dynamicService.orElse(null));
    }

    /**
     * Locale-aware {@link BotReplyButtonMatcher} that replaces the default exact-text matcher
     * from {@code core} when {@code core-i18n} is on the classpath.
     *
     * <p>Treats {@code @BotReplyButton} values as message-bundle keys, resolves them in the
     * user's locale, and compares against the incoming message text.</p>
     *
     * <p>The {@code beforeName} ordering on this auto-configuration class guarantees that this
     * bean is always registered before {@code CoreAutoConfiguration} is processed, so
     * {@code CoreAutoConfiguration}'s {@code @ConditionalOnMissingBean(BotReplyButtonMatcher.class)}
     * correctly defers to this locale-aware implementation when {@code core-i18n} is on the
     * classpath.</p>
     *
     * @param botMessageSource the message source used to resolve localised button labels
     * @return a locale-aware {@link BotReplyButtonMatcher}
     */
    @Bean
    @ConditionalOnMissingBean(BotReplyButtonMatcher.class)
    public BotReplyButtonMatcher botReplyButtonMatcher(BotMessageSource botMessageSource) {
        return (values, request) -> {
            if (!request.getUpdate().hasMessage()) return false;
            String incomingText = request.getUpdate().getMessage().getText();
            if (incomingText == null) return false;
            for (String key : values) {
                String resolved = botMessageSource.getMessage(key, request);
                if (incomingText.equals(resolved)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Locale-aware {@link BotInlineQueryMatcher} that replaces the default exact-text matcher
     * from {@code core} when {@code core-i18n} is on the classpath.
     *
     * <p>Treats {@code @BotInlineQuery} values as message-bundle keys, resolves them in the
     * user's locale, and compares against the incoming inline query text — so a single
     * {@code @BotInlineQuery("search.query")} annotation handles all supported languages
     * automatically.</p>
     *
     * <p>The {@code beforeName} ordering on this auto-configuration class guarantees that this
     * bean is registered before {@code CoreAutoConfiguration} is processed, so the
     * {@code @ConditionalOnMissingBean(BotInlineQueryMatcher.class)} there correctly defers
     * to this locale-aware implementation.</p>
     *
     * @param botMessageSource the message source used to resolve localised query values
     * @return a locale-aware {@link BotInlineQueryMatcher}
     */
    @Bean
    @ConditionalOnMissingBean(BotInlineQueryMatcher.class)
    public BotInlineQueryMatcher botInlineQueryMatcher(BotMessageSource botMessageSource) {
        return (values, request) -> {
            if (!request.getUpdate().hasInlineQuery()) return false;
            String queryText = request.getUpdate().getInlineQuery().getQuery();
            if (queryText == null) return false;
            for (String key : values) {
                String resolved = botMessageSource.getMessage(key, request);
                if (queryText.equals(resolved)) {
                    return true;
                }
            }
            return false;
        };
    }


    @Bean
    @ConditionalOnMissingBean(BotLocalizedReplyReturnTypeHandler.class)
    public BotLocalizedReplyReturnTypeHandler botLocalizedReplyReturnTypeHandler(
            BotMessageSource botMessageSource,
            BotReplyActionChain botReplyActionChain) {
        return new BotLocalizedReplyReturnTypeHandler(botMessageSource, botReplyActionChain);
    }
}
