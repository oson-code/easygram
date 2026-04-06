package uz.osoncode.easygram.messaging.kafka.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.kafka.KafkaBotPublisherProperties;
import uz.osoncode.easygram.messaging.kafka.KafkaBotUpdatePublisher;
import uz.osoncode.easygram.messaging.kafka.provider.BotKafkaTemplateProvider;

/**
 * Spring Boot auto-configuration for the Kafka {@code BotUpdatePublisher} implementation.
 *
 * <p>Activated only when {@link KafkaTemplate} is present on the classpath.
 * Enables {@link KafkaBotPublisherProperties} binding and registers:</p>
 * <ul>
 *   <li>{@link KafkaBotUpdatePublisher} — forwards every update to the configured topic.</li>
 *   <li>{@code NewTopic} — auto-creates the topic if {@code create-if-absent=true} (default)
 *       and the broker grants admin permissions. Picked up automatically by
 *       {@link KafkaAdmin}.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaBotPublisherProperties.class)
@ConditionalOnProperty(
        prefix = "easygram.messaging.producer",
        name = "producer-type",
        havingValue = "kafka"
)
public class KafkaMessagingAutoConfiguration {

    /**
     * Registers the default {@link BotKafkaTemplateProvider} if none is defined.
     * This simply returns the auto-configured {@link KafkaTemplate}.
     *
     * @param kafkaTemplate the default Kafka template
     * @return a provider returning the default template
     */
    @Bean
    @ConditionalOnMissingBean
    public BotKafkaTemplateProvider botKafkaTemplateProvider(KafkaTemplate<String, String> kafkaTemplate) {
        return () -> kafkaTemplate;
    }

    /**
     * Registers the Kafka-backed {@link KafkaBotUpdatePublisher} bean.
     *
     * @param templateProvider           the provider for the Kafka template
     * @param kafkaBotPublisherProperties properties holding the target topic name
     * @param botConfigurer              shared bot configurer that provides the {@code ObjectMapper}
     * @return a configured {@link KafkaBotUpdatePublisher} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotUpdatePublisher.class)
    public KafkaBotUpdatePublisher kafkaBotUpdatePublisher(
            BotKafkaTemplateProvider templateProvider,
            KafkaBotPublisherProperties kafkaBotPublisherProperties,
            BotConfigurer botConfigurer) {
        return new KafkaBotUpdatePublisher(templateProvider, kafkaBotPublisherProperties, botConfigurer.objectMapper());
    }

    /**
     * Registers a {@code NewTopic} bean so that Spring's {@link KafkaAdmin} creates the
     * configured topic on startup if it does not already exist.
     *
     * <p>Skipped when {@code easygram.messaging.kafka.create-if-absent=false}.</p>
     *
     * @param props the Kafka publisher properties
     * @return a {@code NewTopic} descriptor for the configured topic
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaPublisherTopic")
    @ConditionalOnProperty(
            prefix = "easygram.messaging.kafka",
            name = "create-if-absent",
            havingValue = "true",
            matchIfMissing = true)
    public org.apache.kafka.clients.admin.NewTopic kafkaPublisherTopic(KafkaBotPublisherProperties props) {
        return TopicBuilder.name(props.topic())
                .partitions(props.partitions())
                .replicas(props.replicationFactor())
                .build();
    }

    /**
     * Inner configuration that enables Micrometer observation on the {@link KafkaTemplate} used
     * by the publisher. Only activated when an {@link ObservationRegistry} bean is present,
     * which means {@code core-observability} (or any Micrometer-backed tracing) is on the
     * classpath and configured.
     *
     * <p>When active, every {@code KafkaTemplate.send()} call creates a
     * {@code spring.kafka.producer} child span inside the current {@code easygram.update}
     * observation and injects a W3C {@code traceparent} header into the Kafka message so the
     * consumer side can continue the trace.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ObservationRegistry.class)
    @ConditionalOnBean(ObservationRegistry.class)
    static class KafkaPublisherObservationConfig {

        /**
         * Returns a {@link SmartInitializingSingleton} that enables observation on the
         * {@link KafkaTemplate} after all beans have been instantiated.
         *
         * @param templateProvider the provider wrapping the auto-configured Kafka template
         * @return a post-instantiation hook that calls {@code setObservationEnabled(true)}
         */
        @Bean
        public SmartInitializingSingleton botKafkaTemplateObservationConfigurer(
                BotKafkaTemplateProvider templateProvider) {
            return () -> templateProvider.provide().setObservationEnabled(true);
        }
    }
}
