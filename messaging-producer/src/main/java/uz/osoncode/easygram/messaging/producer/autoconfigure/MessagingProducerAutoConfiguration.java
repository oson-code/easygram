package uz.osoncode.easygram.messaging.producer.autoconfigure;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.kafka.KafkaBotPublisherProperties;
import uz.osoncode.easygram.messaging.kafka.KafkaBotUpdatePublisher;
import uz.osoncode.easygram.messaging.kafka.provider.BotKafkaTemplateProvider;
import uz.osoncode.easygram.messaging.producer.MessagingProducerProperties;
import uz.osoncode.easygram.messaging.rabbit.RabbitBotPublisherProperties;
import uz.osoncode.easygram.messaging.rabbit.RabbitBotUpdatePublisher;
import uz.osoncode.easygram.messaging.rabbit.provider.BotRabbitTemplateProvider;

/**
 * Spring Boot auto-configuration for the {@code messaging-producer} module.
 *
 * <p>Selects the correct {@link BotUpdatePublisher} implementation based on
 * {@code telegram.bot.messaging.producer.producer-type}. Runs before the individual
 * {@code messaging-kafka} / {@code messaging-rabbit} autoconfiguration classes so that
 * the chosen publisher bean is registered first; those classes then skip their own
 * publisher registration via {@code @ConditionalOnMissingBean(BotUpdatePublisher.class)}
 * and continue to handle their own infrastructure (topics, exchanges, queues, bindings).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@AutoConfigureBefore(name = {
        "uz.osoncode.easygram.messaging.kafka.autoconfigure.KafkaMessagingAutoConfiguration",
        "uz.osoncode.easygram.messaging.rabbit.autoconfigure.RabbitMessagingAutoConfiguration"
})
@EnableConfigurationProperties(MessagingProducerProperties.class)
public class MessagingProducerAutoConfiguration {

    /**
     * Inner configuration activated when {@code producer-type=kafka} and
     * {@link KafkaTemplate} is present on the classpath.
     *
     * <p>Only registers the publisher bean. Topic auto-creation is delegated to
     * {@code KafkaMessagingAutoConfiguration}.</p>
     */
    @Configuration
    @ConditionalOnClass(KafkaTemplate.class)
    @ConditionalOnProperty(
            prefix = "telegram.bot.messaging.producer",
            name = "producer-type",
            havingValue = "kafka"
    )
    @EnableConfigurationProperties(KafkaBotPublisherProperties.class)
    static class KafkaProducerConfig {

        @Bean
        @ConditionalOnMissingBean(BotUpdatePublisher.class)
        public KafkaBotUpdatePublisher kafkaBotUpdatePublisher(
                KafkaTemplate<String, String> kafkaTemplate,
                KafkaBotPublisherProperties kafkaBotPublisherProperties,
                BotConfigurer botConfigurer) {
            BotKafkaTemplateProvider templateProvider = () -> kafkaTemplate;
            return new KafkaBotUpdatePublisher(templateProvider, kafkaBotPublisherProperties, botConfigurer.objectMapper());
        }
    }

    /**
     * Inner configuration activated when {@code producer-type=rabbit} and
     * {@link RabbitTemplate} is present on the classpath.
     *
     * <p>Only registers the publisher bean. Exchange, queue, and binding auto-creation
     * is delegated to {@code RabbitMessagingAutoConfiguration}.</p>
     */
    @Configuration
    @ConditionalOnClass(RabbitTemplate.class)
    @ConditionalOnProperty(
            prefix = "telegram.bot.messaging.producer",
            name = "producer-type",
            havingValue = "rabbit"
    )
    @EnableConfigurationProperties(RabbitBotPublisherProperties.class)
    static class RabbitProducerConfig {

        @Bean
        @ConditionalOnMissingBean(BotUpdatePublisher.class)
        public RabbitBotUpdatePublisher rabbitBotUpdatePublisher(
                RabbitTemplate rabbitTemplate,
                RabbitBotPublisherProperties rabbitBotPublisherProperties,
                BotConfigurer botConfigurer) {
            BotRabbitTemplateProvider templateProvider = () -> rabbitTemplate;
            return new RabbitBotUpdatePublisher(templateProvider, rabbitBotPublisherProperties, botConfigurer.objectMapper());
        }
    }
}

