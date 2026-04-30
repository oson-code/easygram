package uz.osoncode.easygram.core.observability.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.BotTransportType;
import uz.osoncode.easygram.core.observability.BotObservabilityFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ObservabilityAutoConfiguration}.
 *
 * <p>Guards against regressions where {@link BotObservabilityFilter} is registered even
 * when no {@link ObservationRegistry} bean is present, or where a user-provided filter
 * is not honoured by {@code @ConditionalOnMissingBean}.</p>
 */
class ObservabilityAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=" + BOT_TOKEN)
            .withConfiguration(AutoConfigurations.of(
                    CoreAutoConfiguration.class,
                    ObservabilityAutoConfiguration.class
            ));

    @Test
    void withoutObservationRegistry_doesNotRegisterObservabilityFilter() {
        runner.run(context -> assertThat(context).doesNotHaveBean(BotObservabilityFilter.class));
    }

    @Test
    void withObservationRegistry_registersObservabilityFilter() {
        ObservationRegistry registry = ObservationRegistry.create();
        runner.withBean(ObservationRegistry.class, () -> registry)
                .run(context -> assertThat(context).hasSingleBean(BotObservabilityFilter.class));
    }

    @Test
    void userProvidedObservabilityFilter_suppressesDefault() {
        ObservationRegistry registry = ObservationRegistry.create();
        BotConfigurer configurer = new BotConfigurer(null, BotTransportType.LONG_POLLING);
        BotObservabilityFilter custom = new BotObservabilityFilter(registry, configurer);

        runner.withBean(ObservationRegistry.class, () -> registry)
                .withBean(BotObservabilityFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotObservabilityFilter.class);
                    assertThat(context.getBean(BotObservabilityFilter.class)).isSameAs(custom);
                });
    }
}
