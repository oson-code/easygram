package uz.osoncode.easygram.messaging.rabbit.consumer.autoconfigure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uz.osoncode.easygram.core.bot.BotProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.provider.BotExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.BotObjectMapperProvider;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitBotUpdateListener;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitConsumerBot;
import uz.osoncode.easygram.messaging.rabbit.consumer.RabbitConsumerBotProperties;

import java.util.List;

/**
 * Spring Boot auto-configuration for the RabbitMQ consumer transport module.
 *
 * <p>Activated only when {@link RabbitListener} is present on the classpath.
 * Enables {@link RabbitConsumerBotProperties} binding and registers:</p>
 * <ul>
 *   <li>{@link RabbitConsumerBot} — the bot that authenticates with Telegram and processes updates.</li>
 *   <li>{@link RabbitBotUpdateListener} — the AMQP listener that feeds deserialized updates into the bot.</li>
 *   <li>A {@link TopicExchange}, {@link Queue}, and {@link Binding} — auto-created by
 *       {@link RabbitAdmin} on startup when {@code create-if-absent=true} (default).</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnClass(RabbitListener.class)
@ConditionalOnProperty(prefix = "telegram.bot", name = "transport", havingValue = "RABBIT_CONSUMER")
@EnableConfigurationProperties(RabbitConsumerBotProperties.class)
public class RabbitConsumerAutoConfiguration {

    /**
     * Provides the default {@code botRabbitListenerContainerFactory} used by
     * {@link uz.osoncode.easygram.messaging.rabbit.consumer.RabbitBotUpdateListener}.
     *
     * <p>This fallback factory has no observation support. It is skipped when the
     * {@link RabbitConsumerObservationConfig} inner class registers its own observed variant
     * (i.e. when Micrometer is on the classpath and configured).</p>
     *
     * @param connectionFactory the auto-configured RabbitMQ connection factory
     * @return a basic {@link SimpleRabbitListenerContainerFactory}
     */
    @Bean(name = "botRabbitListenerContainerFactory")
    @ConditionalOnMissingBean(name = "botRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory botRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }

    /**
     * Inner configuration that registers a Micrometer-observation-enabled
     * {@code botRabbitListenerContainerFactory}. Only activated when an
     * {@link ObservationRegistry} bean is present.
     *
     * <p>When active, the listener container extracts the W3C {@code traceparent} header
     * from each incoming AMQP message and continues the distributed trace started on the
     * producer side, creating a {@code spring.rabbit.listener} parent span for the
     * subsequent {@code telegram.bot.update} observation.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ObservationRegistry.class)
    @ConditionalOnBean(ObservationRegistry.class)
    static class RabbitConsumerObservationConfig {

        /**
         * Registers an observation-enabled {@link SimpleRabbitListenerContainerFactory}.
         *
         * @param connectionFactory   the auto-configured RabbitMQ connection factory
         * @param observationRegistry the active Micrometer observation registry
         * @return a factory with {@code observationEnabled=true}
         */
        @Bean(name = "botRabbitListenerContainerFactory")
        @ConditionalOnMissingBean(name = "botRabbitListenerContainerFactory")
        public SimpleRabbitListenerContainerFactory botRabbitListenerContainerFactory(
                ConnectionFactory connectionFactory,
                ObservationRegistry observationRegistry) {
            var factory = new SimpleRabbitListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            factory.setObservationEnabled(true);
            return factory;
        }
    }

    /**
     * Registers the {@link RabbitConsumerBot} wired from fine-grained provider beans.
     *
     * @param botProperties               common bot properties containing the token
     * @param properties                  properties holding the queue and broker settings
     * @param triggers                    startup triggers executed once after authentication
     * @param filters                     filters applied to every incoming update
     * @param botDispatcher               dispatcher that routes updates to handler methods
     * @param botExceptionHandlerRegistry registry of exception handler methods
     * @param telegramClientProvider      provider for the outbound {@code TelegramClient}
     * @param executorServiceProvider     provider for the update-processing thread pool
     * @return a fully configured {@link RabbitConsumerBot} instance
     */
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

    /**
     * Registers the {@link RabbitBotUpdateListener} that feeds deserialized updates into the bot.
     *
     * @param rabbitConsumerBot    the bot instance that processes updates
     * @param objectMapperProvider provider whose {@code ObjectMapper} deserializes payloads
     * @return a new {@link RabbitBotUpdateListener} instance
     */
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
     * <p>Skipped when {@code telegram.bot.rabbit-consumer.create-if-absent=false}.</p>
     *
     * @param props the RabbitMQ consumer properties containing the exchange name
     * @return a durable {@link TopicExchange}
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitConsumerExchange")
    @ConditionalOnProperty(
            prefix = "telegram.bot.rabbit-consumer",
            name = "create-if-absent",
            havingValue = "true",
            matchIfMissing = true)
    public TopicExchange rabbitConsumerExchange(RabbitConsumerBotProperties props) {
        return new TopicExchange(props.exchange(), true, false);
    }

    /**
     * Declares the durable queue so that {@link RabbitAdmin} creates it if absent.
     *
     * <p>Skipped when {@code telegram.bot.rabbit-consumer.create-if-absent=false}.</p>
     *
     * @param props the RabbitMQ consumer properties containing the queue name
     * @return a durable {@link Queue}
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitConsumerQueue")
    @ConditionalOnProperty(
            prefix = "telegram.bot.rabbit-consumer",
            name = "create-if-absent",
            havingValue = "true",
            matchIfMissing = true)
    public Queue rabbitConsumerQueue(RabbitConsumerBotProperties props) {
        return QueueBuilder.durable(props.queue()).build();
    }

    /**
     * Declares the binding between exchange and queue so that {@link RabbitAdmin} creates it if absent.
     *
     * <p>Skipped when {@code telegram.bot.rabbit-consumer.create-if-absent=false}.</p>
     *
     * @param rabbitConsumerQueue    the queue to bind
     * @param rabbitConsumerExchange the exchange to bind to
     * @param props                  the RabbitMQ consumer properties containing the routing key
     * @return a {@link Binding} connecting the queue to the exchange
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitConsumerBinding")
    @ConditionalOnProperty(
            prefix = "telegram.bot.rabbit-consumer",
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
