package uz.osoncode.easygram.messaging.kafka.consumer.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.provider.BotExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.BotObjectMapperProvider;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;
import uz.osoncode.easygram.messaging.kafka.EasygramKafkaProperties;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaBotUpdateListener;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaConsumerBot;
import uz.osoncode.easygram.messaging.kafka.provider.BotKafkaConsumerFactoryProvider;

import java.util.List;

/**
 * Spring Boot auto-configuration for the Kafka consumer transport module.
 *
 * <p>Activated when {@link ConcurrentMessageListenerContainer} is present on the classpath,
 * {@code easygram.messaging.type=CONSUMER}, and {@code easygram.messaging.consumer.type=KAFKA}.
 * Enables {@link EasygramKafkaProperties} binding (prefix {@code easygram.messaging.kafka})
 * and registers the following beans:</p>
 * <ul>
 *   <li>{@link KafkaConsumerBot} — the bot instance that authenticates with Telegram and processes updates.</li>
 *   <li>{@link KafkaBotUpdateListener} — the listener that feeds deserialized updates into the bot.</li>
 *   <li>{@code botKafkaListenerContainer} — the programmatic {@link ConcurrentMessageListenerContainer}
 *       wired from {@link BotKafkaConsumerFactoryProvider} and {@link EasygramKafkaProperties}.</li>
 * </ul>
 *
 * <p>All beans are guarded by {@link ConditionalOnMissingBean} so applications can supply
 * their own implementations.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnClass(ConcurrentMessageListenerContainer.class)
@ConditionalOnProperty(prefix = "easygram.messaging", name = "type", havingValue = "CONSUMER")
@ConditionalOnProperty(prefix = "easygram.messaging.consumer", name = "type", havingValue = "KAFKA")
@EnableConfigurationProperties(EasygramKafkaProperties.class)
public class KafkaConsumerAutoConfiguration {

    /**
     * Registers the default {@link BotKafkaConsumerFactoryProvider} if none is defined.
     * This simply returns Spring Boot's auto-configured {@link ConsumerFactory}.
     *
     * <p>Override this bean to provide a custom consumer factory — for example one
     * pointing at a different Kafka cluster or using custom deserializers.</p>
     *
     * @param consumerFactory the auto-configured Kafka consumer factory
     * @return a provider wrapping the default consumer factory
     */
    @Bean
    @ConditionalOnMissingBean
    public BotKafkaConsumerFactoryProvider botKafkaConsumerFactoryProvider(
            ConsumerFactory<Object, Object> consumerFactory) {
        return () -> consumerFactory;
    }

    /**
     * Provides the default {@code botKafkaListenerContainer}: a programmatic
     * {@link ConcurrentMessageListenerContainer} wired from
     * {@link BotKafkaConsumerFactoryProvider} and {@link EasygramKafkaProperties}.
     *
     * <p>This fallback container has no observation support. It is skipped when the
     * {@link KafkaConsumerObservationConfig} inner class registers its own observed variant
     * (i.e. when Micrometer is on the classpath and configured).</p>
     *
     * @param consumerFactoryProvider the provider for the Kafka consumer factory
     * @param kafkaProperties         topic and group ID for the container
     * @param kafkaBotUpdateListener  the listener to register with the container
     * @return a {@link ConcurrentMessageListenerContainer} ready for Spring lifecycle management
     */
    @Bean(name = "botKafkaListenerContainer")
    @ConditionalOnMissingBean(name = "botKafkaListenerContainer")
    public ConcurrentMessageListenerContainer<Object, Object> botKafkaListenerContainer(
            BotKafkaConsumerFactoryProvider consumerFactoryProvider,
            EasygramKafkaProperties kafkaProperties,
            KafkaBotUpdateListener kafkaBotUpdateListener) {
        ContainerProperties containerProps = new ContainerProperties(kafkaProperties.topic());
        containerProps.setGroupId(kafkaProperties.groupId());
        containerProps.setMessageListener(kafkaBotUpdateListener);
        return new ConcurrentMessageListenerContainer<>(consumerFactoryProvider.provide(), containerProps);
    }

    /**
     * Inner configuration that registers a Micrometer-observation-enabled
     * {@code botKafkaListenerContainer}. Only activated when an
     * {@link ObservationRegistry} bean is present.
     *
     * <p>When active, the listener container extracts the W3C {@code traceparent} header from
     * each incoming Kafka message and continues the distributed trace started on the producer
     * side, creating a {@code spring.kafka.consumer} parent span for the subsequent
     * {@code easygram.update} observation.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ObservationRegistry.class)
    @ConditionalOnBean(ObservationRegistry.class)
    static class KafkaConsumerObservationConfig {

        /**
         * Registers an observation-enabled {@link ConcurrentMessageListenerContainer}.
         *
         * @param consumerFactoryProvider the provider for the Kafka consumer factory
         * @param kafkaProperties         topic and group ID for the container
         * @param kafkaBotUpdateListener  the listener to register with the container
         * @param observationRegistry     the active Micrometer observation registry
         * @return a container with {@code observationEnabled=true}
         */
        @Bean(name = "botKafkaListenerContainer")
        @ConditionalOnMissingBean(name = "botKafkaListenerContainer")
        public ConcurrentMessageListenerContainer<Object, Object> botKafkaListenerContainer(
                BotKafkaConsumerFactoryProvider consumerFactoryProvider,
                EasygramKafkaProperties kafkaProperties,
                KafkaBotUpdateListener kafkaBotUpdateListener,
                ObservationRegistry observationRegistry) {
            ContainerProperties containerProps = new ContainerProperties(kafkaProperties.topic());
            containerProps.setGroupId(kafkaProperties.groupId());
            containerProps.setObservationEnabled(true);
            containerProps.setMessageListener(kafkaBotUpdateListener);
            return new ConcurrentMessageListenerContainer<>(consumerFactoryProvider.provide(), containerProps);
        }
    }

    /**
     * Registers the {@link KafkaConsumerBot} that processes updates received from Kafka.
     *
     * @param botProperties               common bot properties containing the token
     * @param kafkaProperties             properties holding the topic name
     * @param triggers                    startup triggers executed after authentication
     * @param filters                     filters applied to every incoming update
     * @param botDispatcher               dispatcher that routes updates to handlers
     * @param botExceptionHandlerRegistry registry of exception handler methods
     * @param telegramClientProvider      provider for the outbound {@code TelegramClient}
     * @param executorServiceProvider     provider for the update-processing thread pool
     * @return a fully configured {@link KafkaConsumerBot} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaConsumerBot kafkaConsumerBot(
            EasygramProperties botProperties,
            EasygramKafkaProperties kafkaProperties,
            List<BotStartTrigger> triggers,
            List<BotFilter> filters,
            BotDispatcher botDispatcher,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            BotTelegramClientProvider telegramClientProvider,
            BotExecutorServiceProvider executorServiceProvider) {
        return new KafkaConsumerBot(botProperties, kafkaProperties, triggers, filters, botDispatcher,
                botExceptionHandlerRegistry, telegramClientProvider, executorServiceProvider);
    }

    /**
     * Registers the {@link KafkaBotUpdateListener} that consumes messages from the Kafka topic
     * and forwards deserialized updates to {@link KafkaConsumerBot}.
     *
     * @param kafkaConsumerBot     the bot instance that processes updates
     * @param objectMapperProvider provider whose {@code ObjectMapper} deserializes payloads
     * @return a new {@link KafkaBotUpdateListener} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaBotUpdateListener kafkaBotUpdateListener(
            KafkaConsumerBot kafkaConsumerBot,
            BotObjectMapperProvider objectMapperProvider) {
        return new KafkaBotUpdateListener(kafkaConsumerBot, objectMapperProvider);
    }

    /**
     * Registers a {@code NewTopic} bean so that Kafka creates the consumer
     * topic on startup if it does not already exist.
     *
     * <p>Skipped when {@code easygram.messaging.kafka.create-if-absent=false}.</p>
     *
     * @param props the Kafka properties
     * @return a {@code NewTopic} descriptor for the consumer topic
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaConsumerTopic")
    @ConditionalOnProperty(
            prefix = "easygram.messaging.kafka",
            name = "create-if-absent",
            havingValue = "true",
            matchIfMissing = true)
    public org.apache.kafka.clients.admin.NewTopic kafkaConsumerTopic(EasygramKafkaProperties props) {
        return TopicBuilder.name(props.topic())
                .partitions(props.partitions())
                .replicas(props.replicationFactor())
                .build();
    }
}
