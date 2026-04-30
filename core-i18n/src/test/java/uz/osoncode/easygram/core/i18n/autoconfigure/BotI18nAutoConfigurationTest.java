package uz.osoncode.easygram.core.i18n.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.handler.inlinequery.BotInlineQueryMatcher;
import uz.osoncode.easygram.core.i18n.BotLocaleResolver;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.filter.BotLocaleSetterFilter;
import uz.osoncode.easygram.core.i18n.keyboard.BotKeyboardFactory;
import uz.osoncode.easygram.core.i18n.resolver.BotLocaleArgumentResolver;
import uz.osoncode.easygram.core.i18n.resolver.UserLanguageCodeLocaleResolver;
import uz.osoncode.easygram.core.i18n.returntypehandler.BotLocalizedReplyReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReplyActionChain;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BotI18nAutoConfiguration}.
 */
class BotI18nAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FakeMessageSourceConfig.class)
            .withConfiguration(AutoConfigurations.of(
                    BotI18nAutoConfiguration.class,
                    CoreAutoConfiguration.class
            ));

    @Test
    void withoutEnabledProperty_doesNotRegisterBeans() {
        runner.withPropertyValues("easygram.token=" + BOT_TOKEN)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(BotMessageSource.class);
                    assertThat(context).doesNotHaveBean(BotLocaleResolver.class);
                });
    }

    @Test
    void withEnabledTrue_registersBotMessageSource() {
        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .run(context -> assertThat(context).hasSingleBean(BotMessageSource.class));
    }

    @Test
    void withEnabledTrue_registersLocaleResolver() {
        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(BotLocaleResolver.class);
                    assertThat(context.getBean(BotLocaleResolver.class))
                            .isInstanceOf(UserLanguageCodeLocaleResolver.class);
                });
    }

    @Test
    void withEnabledTrue_registersLocaleSetterFilter() {
        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .run(context -> assertThat(context).hasSingleBean(BotLocaleSetterFilter.class));
    }

    @Test
    void withEnabledTrue_registersBotKeyboardFactory() {
        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .run(context -> assertThat(context).hasSingleBean(BotKeyboardFactory.class));
    }

    @Test
    void userProvidedLocaleResolver_suppressesDefault() {
        BotLocaleResolver custom = request -> Locale.FRENCH;

        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .withBean(BotLocaleResolver.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotLocaleResolver.class);
                    assertThat(context.getBean(BotLocaleResolver.class)).isSameAs(custom);
                });
    }

    @Test
    void withEnabledTrue_registersBotLocaleArgumentResolver() {
        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .run(context -> assertThat(context).hasSingleBean(BotLocaleArgumentResolver.class));
    }

    @Test
    void withEnabledTrue_registersBotInlineQueryMatcher() {
        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .run(context -> assertThat(context).hasSingleBean(BotInlineQueryMatcher.class));
    }

    @Test
    void withEnabledTrue_registersBotLocalizedReplyReturnTypeHandler() {
        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .run(context -> assertThat(context).hasSingleBean(BotLocalizedReplyReturnTypeHandler.class));
    }

    @Test
    void userProvidedBotMessageSource_suppressesDefault() {
        StaticMessageSource staticSource = new StaticMessageSource();
        BotMessageSource custom = new BotMessageSource(staticSource, request -> Locale.FRENCH);

        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .withBean(BotMessageSource.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotMessageSource.class);
                    assertThat(context.getBean(BotMessageSource.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedBotLocaleSetterFilter_suppressesDefault() {
        BotLocaleSetterFilter custom = new BotLocaleSetterFilter(request -> Locale.GERMAN);

        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .withBean(BotLocaleSetterFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotLocaleSetterFilter.class);
                    assertThat(context.getBean(BotLocaleSetterFilter.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedBotKeyboardFactory_suppressesDefault() {
        StaticMessageSource staticSource = new StaticMessageSource();
        BotMessageSource messageSource = new BotMessageSource(staticSource, request -> Locale.ENGLISH);
        BotKeyboardFactory custom = new BotKeyboardFactory(messageSource, null);

        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .withBean(BotKeyboardFactory.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotKeyboardFactory.class);
                    assertThat(context.getBean(BotKeyboardFactory.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedBotLocaleArgumentResolver_suppressesDefault() {
        BotLocaleArgumentResolver custom = new BotLocaleArgumentResolver(request -> Locale.JAPANESE);

        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .withBean(BotLocaleArgumentResolver.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotLocaleArgumentResolver.class);
                    assertThat(context.getBean(BotLocaleArgumentResolver.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedBotInlineQueryMatcher_suppressesDefault() {
        BotInlineQueryMatcher custom = (values, request) -> true;

        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .withBean(BotInlineQueryMatcher.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotInlineQueryMatcher.class);
                    assertThat(context.getBean(BotInlineQueryMatcher.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedBotLocalizedReplyReturnTypeHandler_suppressesDefault() {
        StaticMessageSource staticSource = new StaticMessageSource();
        BotMessageSource messageSource = new BotMessageSource(staticSource, request -> Locale.ENGLISH);
        BotLocalizedReplyReturnTypeHandler custom =
                new BotLocalizedReplyReturnTypeHandler(messageSource, new BotReplyActionChain(List.of()));

        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.i18n.enabled=true",
                        "easygram.i18n.default-locale=en"
                )
                .withBean(BotLocalizedReplyReturnTypeHandler.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotLocalizedReplyReturnTypeHandler.class);
                    assertThat(context.getBean(BotLocalizedReplyReturnTypeHandler.class)).isSameAs(custom);
                });
    }

    // ── test config ───────────────────────────────────────────────────────────

    @Configuration
    static class FakeMessageSourceConfig {

        @Bean
        StaticMessageSource messageSource() {
            StaticMessageSource source = new StaticMessageSource();
            source.addMessage("key", Locale.ENGLISH, "Value");
            return source;
        }
    }
}
