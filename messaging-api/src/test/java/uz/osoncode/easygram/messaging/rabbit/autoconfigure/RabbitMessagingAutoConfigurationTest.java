package uz.osoncode.easygram.messaging.rabbit.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.BotTransportType;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.autoconfigure.MessagingAutoConfiguration;
import uz.osoncode.easygram.messaging.rabbit.RabbitBotUpdatePublisher;
import uz.osoncode.easygram.messaging.rabbit.provider.EasygramRabbitConnectionFactoryProvider;
import uz.osoncode.easygram.messaging.rabbit.provider.EasygramRabbitTemplateProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RabbitMessagingAutoConfiguration}.
 */
class RabbitMessagingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(
                    "easygram.messaging.producer.type=RABBIT"
            )
            .withConfiguration(AutoConfigurations.of(
                    RabbitMessagingAutoConfiguration.class,
                    MessagingAutoConfiguration.class
            ))
            .withBean(ConnectionFactory.class, () -> mock(CachingConnectionFactory.class))
            .withBean(BotConfigurer.class,
                    () -> new BotConfigurer(new ObjectMapper(), BotTransportType.LONG_POLLING));

    @Test
    void withRequiredProperties_registersRabbitPublisher() {
        runner.run(context ->
                assertThat(context).hasSingleBean(RabbitBotUpdatePublisher.class));
    }

    @Test
    void withRequiredProperties_publisherIsBotUpdatePublisher() {
        runner.run(context ->
                assertThat(context.getBean(BotUpdatePublisher.class))
                        .isInstanceOf(RabbitBotUpdatePublisher.class));
    }

    @Test
    void withRequiredProperties_registersPublishingFilter() {
        runner.run(context ->
                assertThat(context).hasBean("botUpdatePublishingFilter"));
    }

    @Test
    void noExchangeProperty_usesDefaultExchange() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RabbitBotUpdatePublisher.class);
            var props = context.getBean(uz.osoncode.easygram.messaging.rabbit.EasygramRabbitProperties.class);
            assertThat(props.exchange()).isEqualTo("easygram-exchange");
        });
    }

    @Test
    void withoutProducerType_doesNotRegisterPublisher() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RabbitMessagingAutoConfiguration.class))
                .withBean(ConnectionFactory.class, () -> mock(CachingConnectionFactory.class))
                .run(context -> assertThat(context).doesNotHaveBean(RabbitBotUpdatePublisher.class));
    }

    @Test
    void userProvidedTemplateProvider_suppressesDefault() {
        EasygramRabbitTemplateProvider customProvider = () -> null;

        runner.withBean(EasygramRabbitTemplateProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramRabbitTemplateProvider.class);
                    assertThat(context.getBean(EasygramRabbitTemplateProvider.class))
                            .isSameAs(customProvider);
                });
    }

    @Test
    void userProvidedConnectionFactoryProvider_suppressesDefault() {
        EasygramRabbitConnectionFactoryProvider customProvider = () -> mock(CachingConnectionFactory.class);

        runner.withBean(EasygramRabbitConnectionFactoryProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramRabbitConnectionFactoryProvider.class);
                    assertThat(context.getBean(EasygramRabbitConnectionFactoryProvider.class))
                            .isSameAs(customProvider);
                });
    }
}
