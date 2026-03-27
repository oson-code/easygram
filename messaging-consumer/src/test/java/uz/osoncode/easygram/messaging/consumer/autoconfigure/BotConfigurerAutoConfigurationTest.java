package uz.osoncode.easygram.messaging.consumer.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.BotTransportType;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Regression tests for the {@link BotConfigurer} autoconfiguration ordering between
 * {@link CoreAutoConfiguration} (LONG_POLLING default) and
 * {@link MessagingConsumerAutoConfiguration} (messaging-transport override).
 *
 * <p>{@link MessagingConsumerAutoConfiguration} declares
 * {@code @AutoConfigureBefore(CoreAutoConfiguration)} so its {@link BotConfigurer} bean is
 * always registered first. {@link CoreAutoConfiguration}'s
 * {@code @ConditionalOnMissingBean(BotConfigurer.class)} then correctly defers to the
 * messaging transport's configurer.</p>
 */
class BotConfigurerAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner();

    /**
     * When only {@link CoreAutoConfiguration} is present, the registered {@link BotConfigurer}
     * must use the transport from {@code telegram.bot.transport} (defaults to
     * {@link BotTransportType#LONG_POLLING}).
     */
    @Test
    void coreOnly_defaultsToLongPolling() {
        runner.withPropertyValues("telegram.bot.token=" + BOT_TOKEN)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotConfigurer.class);
                    assertThat(context.getBean(BotConfigurer.class).transportType())
                            .isEqualTo(BotTransportType.LONG_POLLING);
                });
    }

    /**
     * When {@link MessagingConsumerAutoConfiguration} is present alongside
     * {@link CoreAutoConfiguration} and {@code consumer-type=kafka} is configured, the
     * {@link BotConfigurer} from {@link MessagingConsumerAutoConfiguration} must win.
     *
     * <p>A mock {@link BotTelegramClientProvider} is supplied to prevent the
     * {@link uz.osoncode.easygram.core.bot.Bot#afterPropertiesSet()} lifecycle method from
     * making a real {@code getMe} call to the Telegram API.</p>
     */
    @Test
    @SuppressWarnings("unchecked")
    void kafkaConsumer_kafkaConfigurerTakesPrecedence() throws Exception {
        TelegramClient mockClient = mock(TelegramClient.class);
        doReturn(mock(User.class)).when(mockClient).execute(any(GetMe.class));

        runner.withPropertyValues(
                        "telegram.bot.token=" + BOT_TOKEN,
                        "telegram.bot.messaging.consumer.consumer-type=kafka",
                        "telegram.bot.kafka-consumer.topic=test-topic",
                        "telegram.bot.kafka-consumer.create-if-absent=false"
                )
                .withBean(BotTelegramClientProvider.class, () -> (BotTelegramClientProvider) token -> mockClient)
                .withConfiguration(AutoConfigurations.of(
                        CoreAutoConfiguration.class,
                        MessagingConsumerAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotConfigurer.class);
                    assertThat(context.getBean(BotConfigurer.class).transportType())
                            .isEqualTo(BotTransportType.KAFKA_CONSUMER);
                });
    }

    /**
     * A user-provided {@link BotConfigurer} bean must suppress both the core default and any
     * messaging-consumer override — {@code @ConditionalOnMissingBean} must respect user beans.
     */
    @Test
    void userProvidedConfigurer_customBeanTakesPrecedence() {
        BotConfigurer customConfigurer = new BotConfigurer(null, BotTransportType.KAFKA_CONSUMER);

        runner.withPropertyValues("telegram.bot.token=" + BOT_TOKEN)
                .withBean(BotConfigurer.class, () -> customConfigurer)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotConfigurer.class);
                    assertThat(context.getBean(BotConfigurer.class)).isSameAs(customConfigurer);
                });
    }
}
