package uz.osoncode.easygram.messaging.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.BotUpdatePublishingFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessagingAutoConfiguration}.
 */
class MessagingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration.class));

    @Test
    void withoutPublisher_doesNotRegisterFilter() {
        runner.run(context -> assertThat(context).doesNotHaveBean(BotUpdatePublishingFilter.class));
    }

    @Test
    void withPublisherBean_registersFilter() {
        BotUpdatePublisher publisher = update -> {};

        runner.withBean(BotUpdatePublisher.class, () -> publisher)
                .run(context -> assertThat(context).hasSingleBean(BotUpdatePublishingFilter.class));
    }

    @Test
    void userProvidedFilter_suppressesDefault() {
        BotUpdatePublisher publisher = update -> {};
        // Provide the filter bean directly — conditionalOnMissingBean must suppress autoconfig
        runner.withBean(BotUpdatePublisher.class, () -> publisher)
                .withBean(BotUpdatePublishingFilter.class,
                        () -> new BotUpdatePublishingFilter(publisher, null))
                .run(context -> assertThat(context).hasSingleBean(BotUpdatePublishingFilter.class));
    }

    @Test
    void withRabbitConsumerTransport_doesNotRegisterFilter_evenWithPublisher() {
        BotUpdatePublisher publisher = update -> {};

        runner.withBean(BotUpdatePublisher.class, () -> publisher)
                .withPropertyValues("easygram.update.transport=RABBIT_CONSUMER")
                .run(context -> assertThat(context)
                        .as("BotUpdatePublishingFilter must be suppressed for RABBIT_CONSUMER to prevent update re-publish loop")
                        .doesNotHaveBean(BotUpdatePublishingFilter.class));
    }

    @Test
    void withKafkaConsumerTransport_doesNotRegisterFilter_evenWithPublisher() {
        BotUpdatePublisher publisher = update -> {};

        runner.withBean(BotUpdatePublisher.class, () -> publisher)
                .withPropertyValues("easygram.update.transport=KAFKA_CONSUMER")
                .run(context -> assertThat(context)
                        .as("BotUpdatePublishingFilter must be suppressed for KAFKA_CONSUMER to prevent update re-publish loop")
                        .doesNotHaveBean(BotUpdatePublishingFilter.class));
    }

    @Test
    void withLongPollingTransport_registersFilter_withPublisher() {
        BotUpdatePublisher publisher = update -> {};

        runner.withBean(BotUpdatePublisher.class, () -> publisher)
                .withPropertyValues("easygram.update.transport=LONG_POLLING")
                .run(context -> assertThat(context)
                        .as("BotUpdatePublishingFilter must be registered for LONG_POLLING transport")
                        .hasSingleBean(BotUpdatePublishingFilter.class));
    }

    @Test
    void withRabbitConsumerTransport_userProvidedFilter_isRegistered() {
        BotUpdatePublisher publisher = update -> {};

        // Users can still force a publishing filter in consumer mode via their own @Bean
        runner.withBean(BotUpdatePublisher.class, () -> publisher)
                .withBean(BotUpdatePublishingFilter.class,
                        () -> new BotUpdatePublishingFilter(publisher, null))
                .withPropertyValues("easygram.update.transport=RABBIT_CONSUMER")
                .run(context -> assertThat(context)
                        .as("User-defined BotUpdatePublishingFilter must not be suppressed by autoconfiguration")
                        .hasSingleBean(BotUpdatePublishingFilter.class));
    }
}
