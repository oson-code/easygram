package uz.osoncode.easygram.messaging.rabbit.consumer.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitBotUpdateListener;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitConsumerBot;
import uz.osoncode.easygram.messaging.rabbit.provider.EasygramRabbitConnectionFactoryProvider;
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
                    "easygram.update.transport=RABBIT_CONSUMER"
            )
            .withConfiguration(AutoConfigurations.of(
                    CoreAutoConfiguration.class,
                    RabbitConsumerAutoConfiguration.class
            ))
            .withBean(EasygramTelegramClientProvider.class, RabbitConsumerAutoConfigurationTest::fakeTelegramClientProvider)
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
    void withoutRabbitConsumerTransport_doesNotRegisterBot() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "easygram.token=" + BOT_TOKEN
                )
                .withConfiguration(AutoConfigurations.of(
                        CoreAutoConfiguration.class,
                        RabbitConsumerAutoConfiguration.class
                ))
                .withBean(EasygramTelegramClientProvider.class, RabbitConsumerAutoConfigurationTest::fakeTelegramClientProvider)
                .withBean(ConnectionFactory.class, () -> mock(CachingConnectionFactory.class))
                .run(context -> assertThat(context).doesNotHaveBean(RabbitConsumerBot.class));
    }

    @Test
    void userProvidedConnectionFactoryProvider_suppressesDefault() {
        EasygramRabbitConnectionFactoryProvider customProvider = () -> mock(CachingConnectionFactory.class);

        runner.withBean(EasygramRabbitConnectionFactoryProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramRabbitConnectionFactoryProvider.class);
                    assertThat(context.getBean(EasygramRabbitConnectionFactoryProvider.class))
                            .isSameAs(customProvider);
                });
    }

    @Test
    void userProvidedRabbitListener_suppressesDefault() {
        RabbitBotUpdateListener custom = mock(RabbitBotUpdateListener.class);
        runner.withBean(RabbitBotUpdateListener.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(RabbitBotUpdateListener.class);
                    assertThat(context.getBean(RabbitBotUpdateListener.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedRabbitConsumerBot_suppressesDefault() {
        RabbitConsumerBot custom = mock(RabbitConsumerBot.class);
        runner.withBean(RabbitConsumerBot.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(RabbitConsumerBot.class);
                    assertThat(context.getBean(RabbitConsumerBot.class)).isSameAs(custom);
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
