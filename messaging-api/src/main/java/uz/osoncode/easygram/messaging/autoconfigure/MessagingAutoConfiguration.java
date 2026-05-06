package uz.osoncode.easygram.messaging.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import uz.osoncode.easygram.messaging.EasygramMessagingProperties;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.BotUpdatePublishingFilter;
import uz.osoncode.easygram.messaging.kafka.autoconfigure.KafkaMessagingAutoConfiguration;
import uz.osoncode.easygram.messaging.rabbit.autoconfigure.RabbitMessagingAutoConfiguration;

/**
 * Spring Boot auto-configuration for the messaging SPI module.
 *
 * <p>Activates {@link EasygramMessagingProperties} binding (prefix {@code easygram.messaging})
 * and registers the {@link BotUpdatePublishingFilter} bean when a {@link BotUpdatePublisher}
 * implementation is present in the application context.</p>
 *
 * <p>This auto-configuration class is intentionally minimal: it only wires the filter.
 * The actual {@link BotUpdatePublisher} bean must be provided by a broker-specific module
 * (e.g. {@code messaging-kafka} or {@code messaging-rabbit}) or by the application itself.</p>
 *
 * <p>Ordering: this configuration runs <em>after</em>
 * {@link KafkaMessagingAutoConfiguration} and {@link RabbitMessagingAutoConfiguration} to
 * guarantee that the publisher bean — if any — is already registered when
 * {@link #botUpdatePublishingFilter} evaluates its {@link ConditionalOnBean} condition.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration(after = {KafkaMessagingAutoConfiguration.class, RabbitMessagingAutoConfiguration.class})
@EnableConfigurationProperties(EasygramMessagingProperties.class)
public class MessagingAutoConfiguration {

    /**
     * Registers the {@link BotUpdatePublishingFilter} that intercepts every incoming update
     * and forwards it to the configured {@link BotUpdatePublisher}.
     *
     * <p>The filter is only created when a {@link BotUpdatePublisher} bean is available in the
     * context <em>and</em> the configured transport is not a broker-consumer transport.</p>
     *
     * <p>Consumer transports ({@code RABBIT_CONSUMER}, {@code KAFKA_CONSUMER}) must never
     * republish their consumed updates — doing so would send every update back to the same
     * broker queue, creating an infinite processing loop that causes the bot reply to be sent
     * repeatedly. {@link NotConsumerTransportCondition} suppresses this bean for those
     * transports automatically.</p>
     *
     * <p>To use a publishing filter in a consumer-transport context (advanced relay scenarios),
     * register your own {@link BotUpdatePublishingFilter} bean — the
     * {@code @ConditionalOnMissingBean} on this method will defer to it.</p>
     *
     * @param botUpdatePublisher      the publisher implementation that sends updates to a broker
     * @param botPublishingProperties properties controlling publish-only vs. publish-and-process behaviour
     * @return a configured {@link BotUpdatePublishingFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(BotUpdatePublisher.class)
    @Conditional(NotConsumerTransportCondition.class)
    public BotUpdatePublishingFilter botUpdatePublishingFilter(
            BotUpdatePublisher botUpdatePublisher,
            EasygramMessagingProperties botPublishingProperties) {
        return new BotUpdatePublishingFilter(botUpdatePublisher, botPublishingProperties);
    }
}
