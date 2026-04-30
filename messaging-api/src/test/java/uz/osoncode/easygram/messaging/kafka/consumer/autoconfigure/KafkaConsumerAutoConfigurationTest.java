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
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaBotUpdateListener;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaConsumerBot;
import uz.osoncode.easygram.messaging.kafka.provider.EasygramKafkaConsumerFactoryProvider;

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
                    "easygram.update.transport=KAFKA_CONSUMER"
            )
            .withConfiguration(AutoConfigurations.of(
                    CoreAutoConfiguration.class,
                    KafkaConsumerAutoConfiguration.class
            ))
            .withBean(EasygramTelegramClientProvider.class, KafkaConsumerAutoConfigurationTest::fakeTelegramClientProvider)
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
    void withoutKafkaConsumerTransport_doesNotRegisterBot() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "easygram.token=" + BOT_TOKEN
                )
                .withConfiguration(AutoConfigurations.of(
                        CoreAutoConfiguration.class,
                        KafkaConsumerAutoConfiguration.class
                ))
                .withBean(EasygramTelegramClientProvider.class, KafkaConsumerAutoConfigurationTest::fakeTelegramClientProvider)
                .withBean(ConsumerFactory.class,
                        () -> new DefaultKafkaConsumerFactory<String, String>(FAKE_CONSUMER_PROPS))
                .run(context -> assertThat(context).doesNotHaveBean(KafkaConsumerBot.class));
    }

    @Test
    void userProvidedConsumerFactoryProvider_suppressesDefault() {
        EasygramKafkaConsumerFactoryProvider customProvider =
                () -> new DefaultKafkaConsumerFactory<>(FAKE_CONSUMER_PROPS);

        runner.withBean(EasygramKafkaConsumerFactoryProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramKafkaConsumerFactoryProvider.class);
                    assertThat(context.getBean(EasygramKafkaConsumerFactoryProvider.class))
                            .isSameAs(customProvider);
                });
    }

    @Test
    void userProvidedKafkaListener_suppressesDefault() {
        KafkaBotUpdateListener custom = mock(KafkaBotUpdateListener.class);
        runner.withBean(KafkaBotUpdateListener.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaBotUpdateListener.class);
                    assertThat(context.getBean(KafkaBotUpdateListener.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedKafkaConsumerBot_suppressesDefault() {
        KafkaConsumerBot custom = mock(KafkaConsumerBot.class);
        runner.withBean(KafkaConsumerBot.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaConsumerBot.class);
                    assertThat(context.getBean(KafkaConsumerBot.class)).isSameAs(custom);
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
