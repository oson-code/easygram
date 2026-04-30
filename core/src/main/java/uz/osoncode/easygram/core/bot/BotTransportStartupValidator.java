package uz.osoncode.easygram.core.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

/**
 * Startup bean that validates the configured update transport and produces fast-fail
 * errors for broken configurations — e.g. setting
 * {@code easygram.update.transport=KAFKA_CONSUMER} without the Kafka library on the
 * classpath, or without the {@code messaging-api} module in the application's dependencies.
 *
 * <p>This bean runs after all singleton beans have been instantiated (via
 * {@link SmartInitializingSingleton}), which guarantees that the consumer bot beans —
 * if any were registered — are already present in the context when the check runs.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.7
 */
@Slf4j
@RequiredArgsConstructor
public class BotTransportStartupValidator implements SmartInitializingSingleton {

    private final EasygramUpdateProperties updateProperties;
    private final ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        BotTransportType transport = updateProperties.transport();
        switch (transport) {
            case LONG_POLLING -> {
                log.info("Easygram transport: LONG_POLLING — polling Telegram getUpdates");
                warnIfProducerMissing();
            }
            case WEBHOOK -> {
                log.info("Easygram transport: WEBHOOK — waiting for Telegram push requests");
                warnIfProducerMissing();
            }
            case KAFKA_CONSUMER -> validateBrokerConsumerPresent(transport,
                    "spring-kafka", "easygram-messaging-kafka-consumer");
            case RABBIT_CONSUMER -> validateBrokerConsumerPresent(transport,
                    "spring-boot-starter-amqp", "easygram-messaging-rabbit-consumer");
            case NONE -> log.info(
                    "Easygram transport: NONE — no direct Telegram transport started. "
                    + "The application is expected to provide its own update ingestion.");
        }
    }

    /**
     * Emits a startup warning when {@code messaging-api} is on the classpath but no
     * {@link uz.osoncode.easygram.messaging.BotUpdatePublisher} bean was registered.
     *
     * <p>This is a common misconfiguration: the user added {@code easygram-messaging-kafka}
     * or {@code easygram-messaging-rabbit} to their POM but forgot to set
     * {@code easygram.messaging.producer.type}. Updates will be dispatched to local handlers
     * but never forwarded to a broker.</p>
     */
    private void warnIfProducerMissing() {
        try {
            Class<?> publisherClass = Class.forName("uz.osoncode.easygram.messaging.BotUpdatePublisher");
            String[] beans = applicationContext.getBeanNamesForType(publisherClass);
            if (beans.length == 0) {
                log.warn("easygram: messaging-api is on the classpath but no BotUpdatePublisher bean was found. "
                        + "Updates will NOT be forwarded to any broker. "
                        + "Set easygram.messaging.producer.type=KAFKA or RABBIT to activate publishing.");
            }
        } catch (ClassNotFoundException ignored) {
            // messaging-api not on classpath — standalone bot, nothing to warn
        }
    }

    private void validateBrokerConsumerPresent(BotTransportType transport,
                                               String springDep, String easygramModule) {
        String[] botBeans = applicationContext.getBeanNamesForType(Bot.class);
        if (botBeans.length == 0) {
            throw new IllegalStateException(
                    "easygram.update.transport=" + transport.name()
                    + " is configured but no Bot consumer bean was found in the application context. "
                    + "Ensure the following are present on the classpath: "
                    + springDep + " and the " + easygramModule + " module. "
                    + "If you intentionally want no update transport, set easygram.update.transport=NONE.");
        }
        log.info("Easygram transport: {} — {} Bot bean(s) registered", transport, botBeans.length);
    }
}
