package uz.osoncode.easygram.webhook.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bot.BotTransportStartupValidator;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.webhook.WebhookBot;
import uz.osoncode.easygram.webhook.WebhookController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for {@link WebhookAutoConfiguration}.
 *
 * <p>All tests mock {@link EasygramTelegramClientProvider} so that {@code Bot.afterPropertiesSet()}
 * returns a fake {@link User} without making real HTTP calls.</p>
 */
class WebhookAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(
                    "easygram.token=" + BOT_TOKEN,
                    "easygram.update.transport=WEBHOOK",
                    "easygram.update.webhook.url=https://example.com/webhook"
            )
            .withConfiguration(AutoConfigurations.of(
                    CoreAutoConfiguration.class,
                    WebhookAutoConfiguration.class
            ))
            .withBean(EasygramTelegramClientProvider.class, WebhookAutoConfigurationTest::fakeTelegramClientProvider);

    @Test
    void webhookTransport_registersWebhookBot() {
        runner.run(context -> assertThat(context).hasSingleBean(WebhookBot.class));
    }

    @Test
    void webhookTransport_registersWebhookController() {
        runner.run(context -> assertThat(context).hasSingleBean(WebhookController.class));
    }

    @Test
    void longPollingTransport_doesNotRegisterWebhookBot() {
        runner.withPropertyValues("easygram.update.transport=LONG_POLLING")
                .run(context -> assertThat(context).doesNotHaveBean(WebhookBot.class));
    }

    @Test
    void kafkaConsumerTransport_doesNotRegisterWebhookBot() {
        runner.withBean(BotTransportStartupValidator.class, () -> mock(BotTransportStartupValidator.class))
                .withPropertyValues("easygram.update.transport=KAFKA_CONSUMER")
                .run(context -> assertThat(context).doesNotHaveBean(WebhookBot.class));
    }

    @Test
    void noneTransport_doesNotRegisterWebhookBot() {
        runner.withPropertyValues("easygram.update.transport=NONE")
                .run(context -> assertThat(context).doesNotHaveBean(WebhookBot.class));
    }

    @Test
    void noTransportProperty_doesNotRegisterWebhookBot() {
        new ApplicationContextRunner()
                .withPropertyValues("easygram.token=" + BOT_TOKEN)
                .withConfiguration(AutoConfigurations.of(
                        CoreAutoConfiguration.class,
                        WebhookAutoConfiguration.class
                ))
                .run(context -> assertThat(context).doesNotHaveBean(WebhookBot.class));
    }

    @Test
    void userProvidedWebhookBot_suppressesDefault() {
        WebhookBot custom = mock(WebhookBot.class);
        runner.withBean(WebhookBot.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(WebhookBot.class);
                    assertThat(context.getBean(WebhookBot.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedWebhookController_suppressesDefault() {
        WebhookController custom = mock(WebhookController.class);
        runner.withBean(WebhookController.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(WebhookController.class);
                    assertThat(context.getBean(WebhookController.class)).isSameAs(custom);
                });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static EasygramTelegramClientProvider fakeTelegramClientProvider() {
        TelegramClient client = mock(TelegramClient.class);
        User fakeUser = User.builder()
                .id(123L)
                .firstName("TestBot")
                .userName("test_bot")
                .isBot(true)
                .build();
        try {
            doReturn(fakeUser).when(client).execute(any(GetMe.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return token -> client;
    }
}
