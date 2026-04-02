package uz.osoncode.easygram.core.i18n.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.handler.message.replybutton.BotReplyButtonMatcher;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the {@link BotReplyButtonMatcher} autoconfiguration ordering between
 * {@link CoreAutoConfiguration} (exact-text default) and {@link BotI18nAutoConfiguration}
 * (locale-aware override).
 *
 * <p>These tests guard against the silent ordering bug where {@code CoreAutoConfiguration}'s
 * exact-text matcher could register first, causing {@code BotI18nAutoConfiguration}'s
 * {@code @ConditionalOnMissingBean} to be skipped even when {@code core-i18n} is present.</p>
 */
class BotReplyButtonMatcherAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner();

    /**
     * When only {@code CoreAutoConfiguration} is loaded (no i18n on classpath), the registered
     * {@link BotReplyButtonMatcher} must be the exact-text implementation.
     * It compares incoming button text against the provided values without any message-key
     * resolution, and no {@link BotMessageSource} should be present.
     */
    @Test
    void coreOnly_registersExactTextMatcher() {
        runner.withPropertyValues("telegram.bot.token=" + BOT_TOKEN)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotReplyButtonMatcher.class);
                    assertThat(context).doesNotHaveBean(BotMessageSource.class);

                    BotReplyButtonMatcher matcher = context.getBean(BotReplyButtonMatcher.class);

                    // exact-text: "hello" == "hello" → true
                    assertThat(matcher.matches(new String[]{"hello"}, requestWithText("hello"))).isTrue();
                    // exact-text: "btn.yes" ≠ "Yes" — no message-key resolution → false
                    assertThat(matcher.matches(new String[]{"btn.yes"}, requestWithText("Yes"))).isFalse();
                });
    }

    /**
     * When both {@code CoreAutoConfiguration} and {@code BotI18nAutoConfiguration} are loaded,
     * the i18n locale-aware matcher must win (exactly one bean, the i18n one). The ordering fix
     * ensures {@code BotI18nAutoConfiguration} runs <em>before</em> {@code CoreAutoConfiguration}
     * so the default exact-text bean is suppressed by {@code @ConditionalOnMissingBean}.
     *
     * <p>The i18n matcher resolves message keys via the configured {@link BotMessageSource}, so
     * {@code "btn.yes"} (a message key) must match a request whose text is {@code "Yes"}
     * (the resolved value).</p>
     */
    @Test
    void coreAndI18n_i18nMatcherTakesPrecedence() {
        runner.withPropertyValues(
                        "telegram.bot.token=" + BOT_TOKEN,
                        "telegram.bot.i18n.default-locale=en"
                )
                .withUserConfiguration(FakeMessageSourceConfig.class)
                .withConfiguration(AutoConfigurations.of(
                        CoreAutoConfiguration.class,
                        BotI18nAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotReplyButtonMatcher.class);
                    assertThat(context).hasSingleBean(BotMessageSource.class);

                    BotReplyButtonMatcher matcher = context.getBean(BotReplyButtonMatcher.class);

                    // i18n matcher: "btn.yes" resolves to "Yes" in en → matches text "Yes"
                    assertThat(matcher.matches(new String[]{"btn.yes"}, requestWithTextAndLangCode("Yes", "en"))).isTrue();
                    // the literal string "btn.yes" does NOT match resolved text "Yes"
                    assertThat(matcher.matches(new String[]{"btn.yes"}, requestWithTextAndLangCode("btn.yes", "en"))).isFalse();
                });
    }

    /**
     * A user-provided {@link BotReplyButtonMatcher} bean must take precedence over both the
     * core default and the i18n override — {@code @ConditionalOnMissingBean} must suppress both
     * autoconfigured implementations when a custom bean is already present.
     */
    @Test
    void userProvidedMatcher_customBeanTakesPrecedence() {
        BotReplyButtonMatcher customMatcher = (values, request) -> true;

        runner.withPropertyValues(
                        "telegram.bot.token=" + BOT_TOKEN,
                        "telegram.bot.i18n.default-locale=en"
                )
                .withBean(BotReplyButtonMatcher.class, () -> customMatcher)
                .withUserConfiguration(FakeMessageSourceConfig.class)
                .withConfiguration(AutoConfigurations.of(
                        CoreAutoConfiguration.class,
                        BotI18nAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotReplyButtonMatcher.class);
                    BotReplyButtonMatcher registered = context.getBean(BotReplyButtonMatcher.class);
                    assertThat(registered).isSameAs(customMatcher);
                });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static BotRequest requestWithText(String text) {
        return requestWithTextAndLangCode(text, "en");
    }

    private static BotRequest requestWithTextAndLangCode(String text, String langCode) {
        Message message = mock(Message.class);
        when(message.getText()).thenReturn(text);

        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);

        User telegramUser = mock(User.class);
        when(telegramUser.getLanguageCode()).thenReturn(langCode);

        BotRequest request = new BotRequest();
        request.setUpdate(update);
        request.setUser(telegramUser);
        return request;
    }

    // ── test configurations ───────────────────────────────────────────────────

    @Configuration
    static class FakeMessageSourceConfig {

        @Bean
        StaticMessageSource messageSource() {
            StaticMessageSource source = new StaticMessageSource();
            source.addMessage("btn.yes", Locale.ENGLISH, "Yes");
            source.addMessage("btn.no", Locale.ENGLISH, "No");
            return source;
        }
    }
}
