package uz.osoncode.easygram.core.i18n.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.handler.message.replybutton.BotReplyButtonMatcher;
import uz.osoncode.easygram.core.i18n.BotI18nProperties;
import uz.osoncode.easygram.core.i18n.BotLocaleResolver;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.filter.BotLocaleSetterFilter;
import uz.osoncode.easygram.core.i18n.keyboard.BotKeyboardFactory;
import uz.osoncode.easygram.core.i18n.resolver.BotLocaleArgumentResolver;
import uz.osoncode.easygram.core.i18n.resolver.UserLanguageCodeLocaleResolver;
import uz.osoncode.easygram.core.i18n.returntypehandler.BotLocalizedReplyReturnTypeHandler;
import uz.osoncode.easygram.core.i18n.returntypehandler.BotLocalizedTemplateReturnTypeHandler;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;

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
 *   <li>{@link BotLocalizedTemplateReturnTypeHandler} - resolves {@link uz.osoncode.easygram.core.i18n.LocalizedTemplate}
 *       return values using inline {@code ${key}} / {@code #{index}} template syntax</li>
 *   <li>{@link BotLocalizedReplyReturnTypeHandler} - resolves {@link uz.osoncode.easygram.core.i18n.LocalizedReply}
 *       return values using direct key lookup</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration(after = MessageSourceAutoConfiguration.class,
        beforeName = "uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration")
@EnableConfigurationProperties(BotI18nProperties.class)
public class BotI18nAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BotLocaleResolver.class)
    public BotLocaleResolver botLocaleResolver(BotI18nProperties properties) {
        return new UserLanguageCodeLocaleResolver(properties.defaultLocale());
    }

    @Bean
    @ConditionalOnMissingBean(BotMessageSource.class)
    public BotMessageSource botMessageSource(MessageSource messageSource,
                                             BotLocaleResolver localeResolver) {
        return new BotMessageSource(messageSource, localeResolver);
    }

    @Bean
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
    public BotKeyboardFactory botKeyboardFactory(BotMessageSource botMessageSource) {
        return new BotKeyboardFactory(botMessageSource);
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
            String incomingText = request.getUpdate().getMessage().getText();
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
     * Return-type handler that resolves {@link uz.osoncode.easygram.core.i18n.LocalizedTemplate}
     * objects returned from handler methods.
     *
     * <p>Supports inline template syntax:</p>
     * <ul>
     *   <li>{@code ${key}} — resolved from the message bundle in the user's locale</li>
     *   <li>{@code #{index}} — replaced with the corresponding positional argument</li>
     * </ul>
     *
     * <p>Also attaches registered markups if a markup ID is present on the reply.</p>
     *
     * @param botMessageSource the locale-aware message source used to resolve {@code ${key}} tokens
     * @param markupRegistry   registry of markup factories, used to resolve {@code .withMarkup("id")}
     * @return a {@link BotLocalizedTemplateReturnTypeHandler} registered for use in the handler pipeline
     */
    @Bean
    @ConditionalOnMissingBean(BotLocalizedTemplateReturnTypeHandler.class)
    public BotLocalizedTemplateReturnTypeHandler botLocalizedTemplateReturnTypeHandler(
            BotMessageSource botMessageSource,
            Optional<BotMarkupRegistry> markupRegistry) {
        return new BotLocalizedTemplateReturnTypeHandler(botMessageSource, markupRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(BotLocalizedReplyReturnTypeHandler.class)
    public BotLocalizedReplyReturnTypeHandler botLocalizedReplyReturnTypeHandler(
            BotMessageSource botMessageSource,
            Optional<BotMarkupRegistry> markupRegistry) {
        return new BotLocalizedReplyReturnTypeHandler(botMessageSource, markupRegistry);
    }
}
