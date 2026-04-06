package uz.osoncode.easygram.messaging.consumer.autoconfigure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.BotProperties;
import uz.osoncode.easygram.core.bot.BotTransportType;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.provider.BotExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.BotObjectMapperProvider;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;
import uz.osoncode.easygram.messaging.consumer.MessagingConsumerProperties;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaBotUpdateListener;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaConsumerBot;
import uz.osoncode.easygram.messaging.kafka.consumer.KafkaConsumerBotProperties;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitBotUpdateListener;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitConsumerBot;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitConsumerBotProperties;

import java.util.List;


/**
 * Spring Boot auto-configuration for the {@code messaging-consumer} module.
 *
 * <p>Selects the correct consumer transport based on
 * {@code easygram.messaging.consumer.consumer-type}:</p>
 * <ul>
 *   <li>{@code kafka} — Kafka consumer + optional topic auto-creation.</li>
 *   <li>{@code rabbit} — RabbitMQ consumer + optional exchange/queue/binding auto-creation.</li>
 * </ul>
 *
 * <p>Also registers a {@link BotConfigurer} with the correct {@link BotTransportType} so
 * users do not need to set {@code easygram.transport} explicitly.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@AutoConfigureBefore(name = {
        "uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration",
        "uz.osoncode.easygram.messaging.kafka.consumer.autoconfigure.KafkaConsumerAutoConfiguration",
        "uz.osoncode.easygram.messaging.rabbit.consumer.autoconfigure.RabbitConsumerAutoConfiguration"
})
@EnableConfigurationProperties(MessagingConsumerProperties.class)
public class MessagingConsumerAutoConfiguration {

    /**
     * Inner configuration activated when {@code consumer-type=kafka} and
     * {@link KafkaListener} is present on the classpath.
     */
    @Configuration
    @ConditionalOnClass(KafkaListener.class)
    @ConditionalOnProperty(
            prefix = "easygram.messaging.consumer",
            name = "consumer-type",
            havingValue = "kafka"
    )
    @EnableConfigurationProperties(KafkaConsumerBotProperties.class)
    static class KafkaConsumerConfig {

        /**
         * Provides a {@link BotConfigurer} with {@link BotTransportType#KAFKA_CONSUMER} so
         * users do not need to set {@code easygram.transport} explicitly.
         */
        @Bean
        @ConditionalOnMissingBean
        public BotConfigurer botConfigurer(BotObjectMapperProvider objectMapperProvider) {
            return new BotConfigurer(objectMapperProvider.provide(), BotTransportType.KAFKA_CONSUMER);
        }

        @Bean
        @ConditionalOnMissingBean
        public KafkaConsumerBot kafkaConsumerBot(
                BotProperties botProperties,
                KafkaConsumerBotProperties properties,
                List<BotStartTrigger> triggers,
                List<BotFilter> filters,
                BotDispatcher botDispatcher,
                BotExceptionHandlerRegistry botExceptionHandlerRegistry,
                BotTelegramClientProvider telegramClientProvider,
                BotExecutorServiceProvider executorServiceProvider) {
            return new KafkaConsumerBot(botProperties, properties, triggers, filters, botDispatcher,
                    botExceptionHandlerRegistry, telegramClientProvider, executorServiceProvider);
        }

        @Bean
        @ConditionalOnMissingBean
        public KafkaBotUpdateListener kafkaBotUpdateListener(
                KafkaConsumerBot kafkaConsumerBot,
                BotObjectMapperProvider objectMapperProvider) {
            return new KafkaBotUpdateListener(kafkaConsumerBot, objectMapperProvider);
        }

        /**
         * Auto-creates the Kafka topic if {@code create-if-absent=true}.
         */
        @Bean
        @ConditionalOnMissingBean(name = "kafkaConsumerTopic")
        @ConditionalOnProperty(
                prefix = "easygram.kafka-consumer",
                name = "create-if-absent",
                havingValue = "true",
                matchIfMissing = true)
        public NewTopic kafkaConsumerTopic(KafkaConsumerBotProperties props) {
            return TopicBuilder.name(props.topic())
                    .partitions(props.partitions())
                    .replicas(props.replicationFactor())
                    .build();
        }
    }

    /**
     * Inner configuration activated when {@code consumer-type=rabbit} and
     * {@link RabbitListener} is present on the classpath.
     */
    @Configuration
    @ConditionalOnClass(RabbitListener.class)
    @ConditionalOnProperty(
            prefix = "easygram.messaging.consumer",
            name = "consumer-type",
            havingValue = "rabbit"
    )
    @EnableConfigurationProperties(RabbitConsumerBotProperties.class)
    static class RabbitConsumerConfig {

        /**
         * Provides a {@link BotConfigurer} with {@link BotTransportType#RABBIT_CONSUMER} so
         * users do not need to set {@code easygram.transport} explicitly.
         */
        @Bean
        @ConditionalOnMissingBean
        public BotConfigurer botConfigurer(BotObjectMapperProvider objectMapperProvider) {
            return new BotConfigurer(objectMapperProvider.provide(), BotTransportType.RABBIT_CONSUMER);
        }

        @Bean
        @ConditionalOnMissingBean
        public RabbitConsumerBot rabbitConsumerBot(
                BotProperties botProperties,
                RabbitConsumerBotProperties properties,
                List<BotStartTrigger> triggers,
                List<BotFilter> filters,
                BotDispatcher botDispatcher,
                BotExceptionHandlerRegistry botExceptionHandlerRegistry,
                BotTelegramClientProvider telegramClientProvider,
                BotExecutorServiceProvider executorServiceProvider) {
            return new RabbitConsumerBot(botProperties, properties, triggers, filters, botDispatcher,
                    botExceptionHandlerRegistry, telegramClientProvider, executorServiceProvider);
        }

        @Bean
        @ConditionalOnMissingBean
        public RabbitBotUpdateListener rabbitBotUpdateListener(
                RabbitConsumerBot rabbitConsumerBot,
                BotObjectMapperProvider objectMapperProvider) {
            return new RabbitBotUpdateListener(rabbitConsumerBot, objectMapperProvider);
        }

        /**
         * Declares the topic exchange so that {@link RabbitAdmin} creates it if absent.
         *
         * <p>Skipped when {@code easygram.rabbit-consumer.create-if-absent=false}.</p>
         */
        @Bean
        @ConditionalOnMissingBean(name = "rabbitConsumerExchange")
        @ConditionalOnProperty(
                prefix = "easygram.rabbit-consumer",
                name = "create-if-absent",
                havingValue = "true",
                matchIfMissing = true)
        public TopicExchange rabbitConsumerExchange(RabbitConsumerBotProperties props) {
            return new TopicExchange(props.exchange(), true, false);
        }

        /**
         * Declares the durable queue so that {@link RabbitAdmin} creates it if absent.
         *
         * <p>Skipped when {@code easygram.rabbit-consumer.create-if-absent=false}.</p>
         */
        @Bean
        @ConditionalOnMissingBean(name = "rabbitConsumerQueue")
        @ConditionalOnProperty(
                prefix = "easygram.rabbit-consumer",
                name = "create-if-absent",
                havingValue = "true",
                matchIfMissing = true)
        public Queue rabbitConsumerQueue(RabbitConsumerBotProperties props) {
            return QueueBuilder.durable(props.queue()).build();
        }

        /**
         * Declares the binding between exchange and queue so that {@link RabbitAdmin} creates
         * it if absent.
         *
         * <p>Skipped when {@code easygram.rabbit-consumer.create-if-absent=false}.</p>
         */
        @Bean
        @ConditionalOnMissingBean(name = "rabbitConsumerBinding")
        @ConditionalOnProperty(
                prefix = "easygram.rabbit-consumer",
                name = "create-if-absent",
                havingValue = "true",
                matchIfMissing = true)
        public Binding rabbitConsumerBinding(
                Queue rabbitConsumerQueue,
                TopicExchange rabbitConsumerExchange,
                RabbitConsumerBotProperties props) {
            return BindingBuilder.bind(rabbitConsumerQueue).to(rabbitConsumerExchange).with(props.routingKey());
        }
    }
}

