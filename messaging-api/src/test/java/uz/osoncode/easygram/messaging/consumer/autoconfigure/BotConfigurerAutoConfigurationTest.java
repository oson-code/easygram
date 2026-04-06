package uz.osoncode.easygram.messaging.consumer.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.BotTransportType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for {@link BotConfigurer} autoconfiguration ordering.
 *
 * <p>In the new design, {@link CoreAutoConfiguration} reads the transport from
 * {@code easygram.update.transport} (via {@code EasygramUpdateProperties}). The messaging
 * role is expressed separately via {@code easygram.messaging.type}, not in the transport type.
 * Consumer bots simply omit {@code update.transport} — no transport auto-configuration starts.</p>
 */
class BotConfigurerAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner();

    /**
     * When only {@link CoreAutoConfiguration} is present, the registered {@link BotConfigurer}
     * must use {@link BotTransportType#LONG_POLLING} (the default).
     */
    @Test
    void coreOnly_defaultsToLongPolling() {
        runner.withPropertyValues("easygram.token=" + BOT_TOKEN)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotConfigurer.class);
                    assertThat(context.getBean(BotConfigurer.class).transportType())
                            .isEqualTo(BotTransportType.LONG_POLLING);
                });
    }

    /**
     * When {@code easygram.update.transport=WEBHOOK}, the registered {@link BotConfigurer}
     * must reflect {@link BotTransportType#WEBHOOK}.
     */
    @Test
    void webhookTransport_configurerReflectsWebhook() {
        runner.withPropertyValues(
                        "easygram.token=" + BOT_TOKEN,
                        "easygram.update.transport=WEBHOOK"
                )
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotConfigurer.class);
                    assertThat(context.getBean(BotConfigurer.class).transportType())
                            .isEqualTo(BotTransportType.WEBHOOK);
                });
    }

    /**
     * A user-provided {@link BotConfigurer} bean must suppress the core default —
     * {@code @ConditionalOnMissingBean} must respect user beans.
     */
    @Test
    void userProvidedConfigurer_customBeanTakesPrecedence() {
        BotConfigurer customConfigurer = new BotConfigurer(null, BotTransportType.LONG_POLLING);

        runner.withPropertyValues("easygram.token=" + BOT_TOKEN)
                .withBean(BotConfigurer.class, () -> customConfigurer)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(BotConfigurer.class);
                    assertThat(context.getBean(BotConfigurer.class)).isSameAs(customConfigurer);
                });
    }
}
