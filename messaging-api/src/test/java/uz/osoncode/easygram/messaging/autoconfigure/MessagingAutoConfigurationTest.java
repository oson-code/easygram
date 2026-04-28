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
        BotUpdatePublishingFilter customFilter = null;
        // Provide the filter bean directly — conditionalOnMissingBean must suppress autoconfig
        runner.withBean(BotUpdatePublisher.class, () -> publisher)
                .withBean(BotUpdatePublishingFilter.class,
                        () -> new BotUpdatePublishingFilter(publisher, null))
                .run(context -> assertThat(context).hasSingleBean(BotUpdatePublishingFilter.class));
    }
}
