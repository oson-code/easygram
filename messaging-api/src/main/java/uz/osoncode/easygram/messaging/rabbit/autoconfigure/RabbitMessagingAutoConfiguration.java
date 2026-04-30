package uz.osoncode.easygram.messaging.rabbit.autoconfigure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.rabbit.EasygramRabbitProperties;
import uz.osoncode.easygram.messaging.rabbit.RabbitBotUpdatePublisher;
import uz.osoncode.easygram.messaging.rabbit.provider.EasygramRabbitConnectionFactoryProvider;
import uz.osoncode.easygram.messaging.rabbit.provider.EasygramRabbitTemplateProvider;

/**
 * Spring Boot auto-configuration for the RabbitMQ {@code BotUpdatePublisher} implementation.
 *
 * <p>Activated when {@link RabbitTemplate} is present on the classpath and
 * {@code easygram.messaging.producer.type=RABBIT} is set. This producer activates
 * independently of the update transport — any transport (long-polling, webhook, or
 * custom) can publish updates to RabbitMQ. Enables {@link EasygramRabbitProperties} binding
 * and registers:</p>
 * <ul>
 *   <li>{@link RabbitBotUpdatePublisher} — forwards every update to the configured exchange.</li>
 *   <li>A {@link TopicExchange}, {@link Queue}, and {@link Binding} — auto-created by
 *       {@link RabbitAdmin} on startup when {@code create-if-absent=true} (default)
 *       and the user has permissions.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(EasygramRabbitProperties.class)
@ConditionalOnProperty(prefix = "easygram.messaging.producer", name = "type", havingValue = "RABBIT")
public class RabbitMessagingAutoConfiguration {

    /**
     * Registers the default {@link EasygramRabbitConnectionFactoryProvider} if none is defined.
     * This simply returns Spring Boot's auto-configured {@link ConnectionFactory}.
     *
     * <p>Override this bean to provide a custom connection factory — for example one
     * pointing at a different RabbitMQ cluster, vhost, or using TLS.</p>
     *
     * @param connectionFactory the auto-configured RabbitMQ connection factory
     * @return a provider wrapping the default connection factory
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramRabbitConnectionFactoryProvider botRabbitConnectionFactoryProvider(
            ConnectionFactory connectionFactory) {
        return () -> connectionFactory;
    }

    /**
     * Registers the default {@link EasygramRabbitTemplateProvider} if none is defined.
     * The template is created from the {@link EasygramRabbitConnectionFactoryProvider}, so
     * customising the connection factory automatically affects the template.
     *
     * @param connectionFactoryProvider the provider for the RabbitMQ connection factory
     * @return a provider returning a {@link RabbitTemplate} built from the factory
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramRabbitTemplateProvider botRabbitTemplateProvider(
            EasygramRabbitConnectionFactoryProvider connectionFactoryProvider) {
        RabbitTemplate template = new RabbitTemplate(connectionFactoryProvider.provide());
        return () -> template;
    }

    /**
     * Registers the RabbitMQ-backed {@link RabbitBotUpdatePublisher} bean.
     *
     * @param templateProvider  the provider for the RabbitMQ template
     * @param rabbitProperties  properties holding the target exchange and routing key
     * @param botConfigurer     shared bot configurer that provides the {@code ObjectMapper}
     * @return a configured {@link RabbitBotUpdatePublisher} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotUpdatePublisher.class)
    public RabbitBotUpdatePublisher rabbitBotUpdatePublisher(
            EasygramRabbitTemplateProvider templateProvider,
            EasygramRabbitProperties rabbitProperties,
            BotConfigurer botConfigurer) {
        return new RabbitBotUpdatePublisher(templateProvider, rabbitProperties, botConfigurer.objectMapper());
    }

    /**
     * Declares the topic exchange so that {@link RabbitAdmin} creates it if absent.
     *
     * <p>Skipped when {@code easygram.messaging.rabbit.create-if-absent=false}.</p>
     *
     * @param props the RabbitMQ properties
     * @return a durable {@link TopicExchange} named after {@code props.exchange()}
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitPublisherExchange")
    @ConditionalOnProperty(
            prefix = "easygram.messaging.rabbit",
            name = "create-if-absent",
            havingValue = "true",
            matchIfMissing = true)
    public TopicExchange rabbitPublisherExchange(EasygramRabbitProperties props) {
        return new TopicExchange(props.exchange(), true, false);
    }

    /**
     * Declares the durable queue so that {@link RabbitAdmin} creates it if absent.
     *
     * <p>Skipped when {@code easygram.messaging.rabbit.create-if-absent=false}.</p>
     *
     * @param props the RabbitMQ properties
     * @return a durable {@link Queue} named after {@code props.queue()}
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitPublisherQueue")
    @ConditionalOnProperty(
            prefix = "easygram.messaging.rabbit",
            name = "create-if-absent",
            havingValue = "true",
            matchIfMissing = true)
    public Queue rabbitPublisherQueue(EasygramRabbitProperties props) {
        return QueueBuilder.durable(props.queue()).build();
    }

    /**
     * Declares the binding between exchange and queue so that {@link RabbitAdmin} creates
     * it if absent.
     *
     * <p>Skipped when {@code easygram.messaging.rabbit.create-if-absent=false}.</p>
     *
     * @param rabbitPublisherQueue    the queue declared by {@link #rabbitPublisherQueue}
     * @param rabbitPublisherExchange the exchange declared by {@link #rabbitPublisherExchange}
     * @param props                   the RabbitMQ properties
     * @return a {@link Binding} connecting the queue to the exchange with {@code props.routingKey()}
     */
    @Bean
    @ConditionalOnMissingBean(name = "rabbitPublisherBinding")
    @ConditionalOnProperty(
            prefix = "easygram.messaging.rabbit",
            name = "create-if-absent",
            havingValue = "true",
            matchIfMissing = true)
    public Binding rabbitPublisherBinding(
            Queue rabbitPublisherQueue,
            TopicExchange rabbitPublisherExchange,
            EasygramRabbitProperties props) {
        return BindingBuilder.bind(rabbitPublisherQueue).to(rabbitPublisherExchange).with(props.routingKey());
    }

    /**
     * Inner configuration that enables Micrometer observation on the {@link RabbitTemplate}
     * used by the publisher. Only activated when an {@link ObservationRegistry} bean is present.
     *
     * <p>When active, every {@code RabbitTemplate.convertAndSend()} call creates a
     * {@code spring.rabbit.producer} child span inside the current {@code easygram.update}
     * observation and injects a W3C {@code traceparent} header into the AMQP message
     * properties so the consumer side can continue the trace.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ObservationRegistry.class)
    @ConditionalOnBean(ObservationRegistry.class)
    static class RabbitPublisherObservationConfig {

        /**
         * Returns a {@link SmartInitializingSingleton} that enables observation on the
         * {@link RabbitTemplate} after all beans have been instantiated.
         *
         * @param templateProvider the provider wrapping the auto-configured RabbitMQ template
         * @return a post-instantiation hook that calls {@code setObservationEnabled(true)}
         */
        @Bean
        public SmartInitializingSingleton botRabbitTemplateObservationConfigurer(
                EasygramRabbitTemplateProvider templateProvider) {
            return () -> templateProvider.provide().setObservationEnabled(true);
        }
    }
}
