package uz.osoncode.easygram.messaging.kafka.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.kafka.core.ProducerFactory;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.kafka.EasygramKafkaProperties;
import uz.osoncode.easygram.messaging.kafka.KafkaBotUpdatePublisher;
import uz.osoncode.easygram.messaging.kafka.provider.EasygramKafkaProducerFactoryProvider;
import uz.osoncode.easygram.messaging.kafka.provider.EasygramKafkaTemplateProvider;

/**
 * Spring Boot auto-configuration for the Kafka {@code BotUpdatePublisher} implementation.
 *
 * <p>Activated when {@link KafkaTemplate} is present on the classpath and
 * {@code easygram.messaging.producer.type=KAFKA} is set. This producer activates
 * independently of the update transport — any transport (long-polling, webhook, or
 * custom) can publish updates to Kafka. Enables {@link EasygramKafkaProperties} binding
 * and registers:</p>
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
@Slf4j
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(EasygramKafkaProperties.class)
@ConditionalOnProperty(prefix = "easygram.messaging.producer", name = "type", havingValue = "KAFKA")
public class KafkaMessagingAutoConfiguration {

    /**
     * Registers the default {@link EasygramKafkaProducerFactoryProvider} if none is defined.
     * This simply returns Spring Boot's auto-configured {@link ProducerFactory}.
     *
     * <p>Override this bean to provide a custom producer factory — for example one
     * pointing at a different Kafka cluster or using custom serializers.</p>
     *
     * @param producerFactory the auto-configured Kafka producer factory
     * @return a provider wrapping the default producer factory
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramKafkaProducerFactoryProvider botKafkaProducerFactoryProvider(
            ProducerFactory<String, String> producerFactory) {
        return () -> producerFactory;
    }

    /**
     * Registers the default {@link EasygramKafkaTemplateProvider} if none is defined.
     * The template is created from the {@link EasygramKafkaProducerFactoryProvider}, so
     * customising the producer factory automatically affects the template.
     *
     * @param producerFactoryProvider the provider for the Kafka producer factory
     * @return a provider returning a {@link KafkaTemplate} built from the factory
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramKafkaTemplateProvider botKafkaTemplateProvider(
            EasygramKafkaProducerFactoryProvider producerFactoryProvider) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactoryProvider.provide());
        return () -> template;
    }

    /**
     * Registers the Kafka-backed {@link KafkaBotUpdatePublisher} bean.
     *
     * @param templateProvider  the provider for the Kafka template
     * @param kafkaProperties   properties holding the target topic name
     * @param botConfigurer     shared bot configurer that provides the {@code ObjectMapper}
     * @return a configured {@link KafkaBotUpdatePublisher} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotUpdatePublisher.class)
    public KafkaBotUpdatePublisher kafkaBotUpdatePublisher(
            EasygramKafkaTemplateProvider templateProvider,
            EasygramKafkaProperties kafkaProperties,
            BotConfigurer botConfigurer) {
        KafkaBotUpdatePublisher publisher =
                new KafkaBotUpdatePublisher(templateProvider, kafkaProperties, botConfigurer.objectMapper());
        if (kafkaProperties.replicationFactor() < 2) {
            log.warn("easygram: kafka replication-factor is {} — topic '{}' has no fault tolerance. "
                    + "Set easygram.messaging.kafka.replication-factor >= 2 for production environments.",
                    kafkaProperties.replicationFactor(), kafkaProperties.topic());
        }
        return publisher;
    }

    /**
     * Registers a {@code NewTopic} bean so that Spring's {@link KafkaAdmin} creates the
     * configured topic on startup if it does not already exist.
     *
     * <p>Skipped when {@code easygram.messaging.kafka.create-if-absent=false}.</p>
     *
     * @param props the Kafka properties
     * @return a {@code NewTopic} descriptor for the configured topic
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaPublisherTopic")
    @ConditionalOnProperty(
            prefix = "easygram.messaging.kafka",
            name = "create-if-absent",
            havingValue = "true",
            matchIfMissing = true)
    public org.apache.kafka.clients.admin.NewTopic kafkaPublisherTopic(EasygramKafkaProperties props) {
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
                EasygramKafkaTemplateProvider templateProvider) {
            return () -> templateProvider.provide().setObservationEnabled(true);
        }
    }
}
