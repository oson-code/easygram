package uz.osoncode.easygram.messaging.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.messaging.BotPublishingProperties;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.BotUpdatePublishingFilter;

/**
 * Spring Boot auto-configuration for the messaging SPI module.
 *
 * <p>Activates {@link BotPublishingProperties} binding (prefix {@code telegram.bot.messaging})
 * and registers the {@link BotUpdatePublishingFilter} bean when a {@link BotUpdatePublisher}
 * implementation is present in the application context.</p>
 *
 * <p>This auto-configuration class is intentionally minimal: it only wires the filter.
 * The actual {@link BotUpdatePublisher} bean must be provided by a broker-specific module
 * (e.g. {@code messaging-kafka} or {@code messaging-rabbit}) or by the application itself.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@EnableConfigurationProperties(BotPublishingProperties.class)
public class MessagingAutoConfiguration {

    /**
     * Registers the {@link BotUpdatePublishingFilter} that intercepts every incoming update
     * and forwards it to the configured {@link BotUpdatePublisher}.
     *
     * <p>The filter is only created when a {@link BotUpdatePublisher} bean is available in the
     * context, ensuring this auto-configuration has no effect until a broker implementation
     * is present.</p>
     *
     * @param botUpdatePublisher      the publisher implementation that sends updates to a broker
     * @param botPublishingProperties properties controlling publish-only vs. publish-and-process behaviour
     * @param botConfigurer           shared bot configurer that provides the {@code ObjectMapper}
     * @return a configured {@link BotUpdatePublishingFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotUpdatePublishingFilter botUpdatePublishingFilter(
            BotUpdatePublisher botUpdatePublisher,
            BotPublishingProperties botPublishingProperties,
            BotConfigurer botConfigurer) {
        return new BotUpdatePublishingFilter(botUpdatePublisher, botPublishingProperties, botConfigurer.objectMapper());
    }
}
