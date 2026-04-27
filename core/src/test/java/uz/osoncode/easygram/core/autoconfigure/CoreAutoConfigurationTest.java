package uz.osoncode.easygram.core.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.TelegramUrl;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.handler.BotHandlerRegistry;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.provider.BotTelegramUrlProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CoreAutoConfiguration}.
 */
class CoreAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=" + BOT_TOKEN)
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class));

    @Test
    void registersHandlerRegistry() {
        runner.run(context -> assertThat(context).hasSingleBean(BotHandlerRegistry.class));
    }

    @Test
    void registersExceptionHandlerRegistry() {
        runner.run(context -> assertThat(context).hasSingleBean(BotExceptionHandlerRegistry.class));
    }

    @Test
    void registersDispatcher() {
        runner.run(context -> assertThat(context).hasSingleBean(BotDispatcher.class));
    }

    @Test
    void registersMarkupRegistry() {
        runner.run(context -> assertThat(context).hasSingleBean(BotMarkupRegistry.class));
    }

    @Test
    void userProvidedDispatcher_suppressesDefault() {
        BotHandlerRegistry registry = new BotHandlerRegistry();
        BotDispatcher custom = new BotDispatcher(registry);

        runner.withBean(BotDispatcher.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotDispatcher.class);
                    assertThat(context.getBean(BotDispatcher.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedHandlerRegistry_suppressesDefault() {
        BotHandlerRegistry custom = new BotHandlerRegistry();

        runner.withBean(BotHandlerRegistry.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotHandlerRegistry.class);
                    assertThat(context.getBean(BotHandlerRegistry.class)).isSameAs(custom);
                });
    }

    @Test
    void noTelegramUrlProperties_providesDefaultUrl() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(BotTelegramUrlProvider.class);
            TelegramUrl url = context.getBean(BotTelegramUrlProvider.class).provide();
            assertThat(url).isEqualTo(TelegramUrl.DEFAULT_URL);
        });
    }

    @Test
    void telegramUrlProperties_hostSet_buildsCustomUrl() {
        runner.withPropertyValues(
                        "easygram.telegram-url.host=my-local-bot-api.example.com",
                        "easygram.telegram-url.port=8443",
                        "easygram.telegram-url.schema=https",
                        "easygram.telegram-url.test-server=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(BotTelegramUrlProvider.class);
                    TelegramUrl url = context.getBean(BotTelegramUrlProvider.class).provide();
                    assertThat(url.getHost()).isEqualTo("my-local-bot-api.example.com");
                    assertThat(url.getPort()).isEqualTo(8443);
                    assertThat(url.getSchema()).isEqualTo("https");
                    assertThat(url.isTestServer()).isFalse();
                });
    }

    @Test
    void userProvidedTelegramUrlProvider_suppressesPropertyDriven() {
        TelegramUrl custom = new TelegramUrl("https", "custom.example.com", 9443, false);
        BotTelegramUrlProvider customProvider = () -> custom;

        runner.withPropertyValues("easygram.telegram-url.host=should-be-ignored.example.com")
                .withBean(BotTelegramUrlProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotTelegramUrlProvider.class);
                    assertThat(context.getBean(BotTelegramUrlProvider.class).provide().getHost())
                            .isEqualTo("custom.example.com");
                });
    }
}
