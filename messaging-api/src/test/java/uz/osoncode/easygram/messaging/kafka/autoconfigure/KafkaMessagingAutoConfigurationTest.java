package uz.osoncode.easygram.messaging.kafka.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.BotTransportType;
import uz.osoncode.easygram.messaging.BotUpdatePublisher;
import uz.osoncode.easygram.messaging.autoconfigure.MessagingAutoConfiguration;
import uz.osoncode.easygram.messaging.kafka.KafkaBotUpdatePublisher;
import uz.osoncode.easygram.messaging.kafka.provider.BotKafkaProducerFactoryProvider;
import uz.osoncode.easygram.messaging.kafka.provider.BotKafkaTemplateProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaMessagingAutoConfiguration}.
 */
class KafkaMessagingAutoConfigurationTest {

    private static final Map<String, Object> FAKE_PRODUCER_PROPS = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            org.apache.kafka.common.serialization.StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            org.apache.kafka.common.serialization.StringSerializer.class
    );

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(
                    "easygram.messaging.type=PRODUCER",
                    "easygram.messaging.producer.type=KAFKA"
            )
            .withConfiguration(AutoConfigurations.of(
                    KafkaMessagingAutoConfiguration.class,
                    MessagingAutoConfiguration.class
            ))
            .withBean(ProducerFactory.class,
                    () -> new DefaultKafkaProducerFactory<String, String>(FAKE_PRODUCER_PROPS))
            .withBean(BotConfigurer.class,
                    () -> new BotConfigurer(new ObjectMapper(), BotTransportType.LONG_POLLING));

    @Test
    void withRequiredProperties_registersKafkaPublisher() {
        runner.run(context ->
                assertThat(context).hasSingleBean(KafkaBotUpdatePublisher.class));
    }

    @Test
    void withRequiredProperties_publisherIsBotUpdatePublisher() {
        runner.run(context -> {
            assertThat(context.getBean(BotUpdatePublisher.class))
                    .isInstanceOf(KafkaBotUpdatePublisher.class);
        });
    }

    @Test
    void withRequiredProperties_registersPublishingFilter() {
        runner.run(context ->
                assertThat(context).hasBean("botUpdatePublishingFilter"));
    }

    @Test
    void noTopicProperty_usesDefaultTopic() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(KafkaBotUpdatePublisher.class);
            var props = context.getBean(uz.osoncode.easygram.messaging.kafka.EasygramKafkaProperties.class);
            assertThat(props.topic()).isEqualTo("easygram-updates");
        });
    }

    @Test
    void withoutProducerType_doesNotRegisterPublisher() {
        new ApplicationContextRunner()
                .withPropertyValues(
                        "easygram.messaging.type=PRODUCER"
                )
                .withConfiguration(AutoConfigurations.of(KafkaMessagingAutoConfiguration.class))
                .withBean(ProducerFactory.class,
                        () -> new DefaultKafkaProducerFactory<String, String>(FAKE_PRODUCER_PROPS))
                .run(context -> assertThat(context).doesNotHaveBean(KafkaBotUpdatePublisher.class));
    }

    @Test
    void userProvidedTemplateProvider_suppressesDefault() {
        BotKafkaTemplateProvider customProvider = () -> null;

        runner.withBean(BotKafkaTemplateProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotKafkaTemplateProvider.class);
                    assertThat(context.getBean(BotKafkaTemplateProvider.class))
                            .isSameAs(customProvider);
                });
    }

    @Test
    void userProvidedProducerFactoryProvider_suppressesDefault() {
        BotKafkaProducerFactoryProvider customProvider = () -> new DefaultKafkaProducerFactory<>(FAKE_PRODUCER_PROPS);

        runner.withBean(BotKafkaProducerFactoryProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotKafkaProducerFactoryProvider.class);
                    assertThat(context.getBean(BotKafkaProducerFactoryProvider.class))
                            .isSameAs(customProvider);
                });
    }
}
