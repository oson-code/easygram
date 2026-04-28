package uz.osoncode.easygram.messaging.rabbit.consumer.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitBotUpdateListener;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitConsumerBot;
import uz.osoncode.easygram.messaging.rabbit.provider.BotRabbitConnectionFactoryProvider;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for {@link RabbitConsumerAutoConfiguration}.
 */
class RabbitConsumerAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(
                    "easygram.token=" + BOT_TOKEN,
                    "easygram.messaging.type=CONSUMER",
                    "easygram.messaging.consumer.type=RABBIT"
            )
            .withConfiguration(AutoConfigurations.of(
                    CoreAutoConfiguration.class,
                    RabbitConsumerAutoConfiguration.class
            ))
            .withBean(BotTelegramClientProvider.class, RabbitConsumerAutoConfigurationTest::fakeTelegramClientProvider)
            .withBean(ConnectionFactory.class, () -> mock(CachingConnectionFactory.class));

    @Test
    void withRequiredProperties_registersRabbitConsumerBot() {
        runner.run(context -> assertThat(context).hasSingleBean(RabbitConsumerBot.class));
    }

    @Test
    void withRequiredProperties_registersRabbitListener() {
        runner.run(context -> assertThat(context).hasSingleBean(RabbitBotUpdateListener.class));
    }

    @Test
    void withRequiredProperties_registersListenerContainer() {
        runner.run(context -> assertThat(context).hasSingleBean(SimpleMessageListenerContainer.class));
    }

    @Test
    void noExchangeProperty_usesDefaultExchange() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RabbitConsumerBot.class);
            var props = context.getBean(uz.osoncode.easygram.messaging.rabbit.EasygramRabbitProperties.class);
            assertThat(props.exchange()).isEqualTo("easygram-exchange");
        });
    }

    @Test
    void withoutConsumerType_doesNotRegisterBot() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.messaging.type=CONSUMER"
                )
                .withConfiguration(AutoConfigurations.of(
                        CoreAutoConfiguration.class,
                        RabbitConsumerAutoConfiguration.class
                ))
                .withBean(BotTelegramClientProvider.class, RabbitConsumerAutoConfigurationTest::fakeTelegramClientProvider)
                .withBean(ConnectionFactory.class, () -> mock(CachingConnectionFactory.class))
                .run(context -> assertThat(context).doesNotHaveBean(RabbitConsumerBot.class));
    }

    @Test
    void userProvidedConnectionFactoryProvider_suppressesDefault() {
        BotRabbitConnectionFactoryProvider customProvider = () -> mock(CachingConnectionFactory.class);

        runner.withBean(BotRabbitConnectionFactoryProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotRabbitConnectionFactoryProvider.class);
                    assertThat(context.getBean(BotRabbitConnectionFactoryProvider.class))
                            .isSameAs(customProvider);
                });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static BotTelegramClientProvider fakeTelegramClientProvider() {
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
