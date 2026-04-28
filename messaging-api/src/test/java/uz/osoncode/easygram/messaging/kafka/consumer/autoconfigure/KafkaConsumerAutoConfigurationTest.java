package uz.osoncode.easygram.messaging.kafka.consumer.autoconfigure;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaBotUpdateListener;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaConsumerBot;
import uz.osoncode.easygram.messaging.kafka.provider.BotKafkaConsumerFactoryProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for {@link KafkaConsumerAutoConfiguration}.
 */
class KafkaConsumerAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private static final Map<String, Object> FAKE_CONSUMER_PROPS = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
            ConsumerConfig.GROUP_ID_CONFIG, "test-group",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            org.apache.kafka.common.serialization.StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            org.apache.kafka.common.serialization.StringDeserializer.class
    );

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(
                    "easygram.token=" + BOT_TOKEN,
                    "easygram.messaging.type=CONSUMER",
                    "easygram.messaging.consumer.type=KAFKA"
            )
            .withConfiguration(AutoConfigurations.of(
                    CoreAutoConfiguration.class,
                    KafkaConsumerAutoConfiguration.class
            ))
            .withBean(BotTelegramClientProvider.class, KafkaConsumerAutoConfigurationTest::fakeTelegramClientProvider)
            .withBean(ConsumerFactory.class,
                    () -> new DefaultKafkaConsumerFactory<String, String>(FAKE_CONSUMER_PROPS));

    @Test
    void withRequiredProperties_registersKafkaConsumerBot() {
        runner.run(context -> assertThat(context).hasSingleBean(KafkaConsumerBot.class));
    }

    @Test
    void withRequiredProperties_registersKafkaListener() {
        runner.run(context -> assertThat(context).hasSingleBean(KafkaBotUpdateListener.class));
    }

    @Test
    void withRequiredProperties_registersListenerContainer() {
        runner.run(context -> assertThat(context).hasSingleBean(ConcurrentMessageListenerContainer.class));
    }

    @Test
    void noTopicProperty_usesDefaultTopic() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(KafkaConsumerBot.class);
            var props = context.getBean(uz.osoncode.easygram.messaging.kafka.EasygramKafkaProperties.class);
            assertThat(props.topic()).isEqualTo("easygram-updates");
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
                        KafkaConsumerAutoConfiguration.class
                ))
                .withBean(BotTelegramClientProvider.class, KafkaConsumerAutoConfigurationTest::fakeTelegramClientProvider)
                .withBean(ConsumerFactory.class,
                        () -> new DefaultKafkaConsumerFactory<String, String>(FAKE_CONSUMER_PROPS))
                .run(context -> assertThat(context).doesNotHaveBean(KafkaConsumerBot.class));
    }

    @Test
    void userProvidedConsumerFactoryProvider_suppressesDefault() {
        BotKafkaConsumerFactoryProvider customProvider =
                () -> new DefaultKafkaConsumerFactory<>(FAKE_CONSUMER_PROPS);

        runner.withBean(BotKafkaConsumerFactoryProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotKafkaConsumerFactoryProvider.class);
                    assertThat(context.getBean(BotKafkaConsumerFactoryProvider.class))
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
