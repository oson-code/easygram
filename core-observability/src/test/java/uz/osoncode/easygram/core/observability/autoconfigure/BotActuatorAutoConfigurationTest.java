package uz.osoncode.easygram.core.observability.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bot.Bot;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.BotTransportType;
import uz.osoncode.easygram.core.observability.BotHealthIndicator;
import uz.osoncode.easygram.core.observability.BotInfoContributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BotActuatorAutoConfiguration}.
 */
class BotActuatorAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=" + BOT_TOKEN)
            .withConfiguration(AutoConfigurations.of(
                    CoreAutoConfiguration.class,
                    BotActuatorAutoConfiguration.class
            ));

    @Test
    void withoutBotBean_doesNotRegisterHealthIndicator() {
        runner.run(context -> assertThat(context).doesNotHaveBean(BotHealthIndicator.class));
    }

    @Test
    void withBotAndConfigurer_registersHealthIndicator() {
        Bot mockBot = mock(Bot.class);
        BotConfigurer configurer = new BotConfigurer(null, BotTransportType.LONG_POLLING);

        runner.withBean(Bot.class, () -> mockBot)
                .withBean(BotConfigurer.class, () -> configurer)
                .run(context -> assertThat(context).hasSingleBean(BotHealthIndicator.class));
    }

    @Test
    void withBotAndConfigurer_registersInfoContributor() {
        Bot mockBot = mock(Bot.class);
        BotConfigurer configurer = new BotConfigurer(null, BotTransportType.LONG_POLLING);

        runner.withBean(Bot.class, () -> mockBot)
                .withBean(BotConfigurer.class, () -> configurer)
                .run(context -> assertThat(context).hasSingleBean(BotInfoContributor.class));
    }

    @Test
    void withBotAndConfigurer_healthIndicatorIsHealthIndicatorType() {
        Bot mockBot = mock(Bot.class);
        BotConfigurer configurer = new BotConfigurer(null, BotTransportType.LONG_POLLING);

        runner.withBean(Bot.class, () -> mockBot)
                .withBean(BotConfigurer.class, () -> configurer)
                .run(context ->
                        assertThat(context.getBean(BotHealthIndicator.class))
                                .isInstanceOf(HealthIndicator.class));
    }

    @Test
    void userProvidedHealthIndicator_suppressesDefault() {
        Bot mockBot = mock(Bot.class);
        BotConfigurer configurer = new BotConfigurer(null, BotTransportType.LONG_POLLING);
        BotHealthIndicator customIndicator = new BotHealthIndicator(mockBot, configurer);

        runner.withBean(Bot.class, () -> mockBot)
                .withBean(BotConfigurer.class, () -> configurer)
                .withBean(BotHealthIndicator.class, () -> customIndicator)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotHealthIndicator.class);
                    assertThat(context.getBean(BotHealthIndicator.class)).isSameAs(customIndicator);
                });
    }

    @Test
    void userProvidedInfoContributor_suppressesDefault() {
        Bot mockBot = mock(Bot.class);
        BotConfigurer configurer = new BotConfigurer(null, BotTransportType.LONG_POLLING);
        BotInfoContributor customContributor = new BotInfoContributor(mockBot, configurer);

        runner.withBean(Bot.class, () -> mockBot)
                .withBean(BotConfigurer.class, () -> configurer)
                .withBean(BotInfoContributor.class, () -> customContributor)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotInfoContributor.class);
                    assertThat(context.getBean(BotInfoContributor.class)).isSameAs(customContributor);
                });
    }
}
