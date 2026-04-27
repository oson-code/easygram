package uz.osoncode.easygram.core.i18n.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.i18n.BotLocaleResolver;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.filter.BotLocaleSetterFilter;
import uz.osoncode.easygram.core.i18n.keyboard.BotKeyboardFactory;
import uz.osoncode.easygram.core.i18n.resolver.UserLanguageCodeLocaleResolver;

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
