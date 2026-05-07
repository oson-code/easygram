package uz.osoncode.easygram.messaging.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A {@link SpringBootCondition} that matches only when the configured update transport is
 * <em>not</em> a broker-consumer transport.
 *
 * <p>Consumer transports ({@code RABBIT_CONSUMER}, {@code KAFKA_CONSUMER}) receive updates
 * from a message broker. Registering {@link uz.osoncode.easygram.messaging.BotUpdatePublishingFilter}
 * in such a context would cause every consumed update to be immediately re-published back to
 * the same broker, creating an infinite processing loop.</p>
 *
 * <p>This condition returns {@link ConditionOutcome#noMatch} when
 * {@code easygram.update.transport} is {@code RABBIT_CONSUMER} or {@code KAFKA_CONSUMER},
 * effectively suppressing the publishing-filter autoconfiguration for consumer bots.</p>
 *
 * <p>If the property is absent or set to any other value (e.g. {@code LONG_POLLING},
 * {@code WEBHOOK}), the condition matches and the filter may be registered normally.</p>
 *
 * <p>Users who genuinely need a publishing filter in a consumer-transport context can bypass
 * this guard by registering their own {@link uz.osoncode.easygram.messaging.BotUpdatePublishingFilter}
 * bean — the {@code @ConditionalOnMissingBean} on the autoconfigured bean will then defer to
 * the user-supplied instance.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.9
 * @see MessagingAutoConfiguration#botUpdatePublishingFilter
 */
public class NotConsumerTransportCondition extends SpringBootCondition {

    private static final String TRANSPORT_PROPERTY = "easygram.update.transport";
    private static final String RABBIT_CONSUMER = "RABBIT_CONSUMER";
    private static final String KAFKA_CONSUMER = "KAFKA_CONSUMER";

    /**
     * Evaluates whether the current transport is a consumer transport and returns the
     * appropriate {@link ConditionOutcome}.
     *
     * @param context  the condition evaluation context
     * @param metadata annotation metadata (unused)
     * @return {@link ConditionOutcome#noMatch} when transport is {@code RABBIT_CONSUMER} or
     *         {@code KAFKA_CONSUMER}; {@link ConditionOutcome#match} otherwise
     */
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String transport = context.getEnvironment().getProperty(TRANSPORT_PROPERTY, "");

        if (RABBIT_CONSUMER.equalsIgnoreCase(transport)) {
            return ConditionOutcome.noMatch(ConditionMessage
                    .forCondition("NotConsumerTransportCondition")
                    .because("transport is RABBIT_CONSUMER — BotUpdatePublishingFilter suppressed to prevent update re-publish loop"));
        }

        if (KAFKA_CONSUMER.equalsIgnoreCase(transport)) {
            return ConditionOutcome.noMatch(ConditionMessage
                    .forCondition("NotConsumerTransportCondition")
                    .because("transport is KAFKA_CONSUMER — BotUpdatePublishingFilter suppressed to prevent update re-publish loop"));
        }

        return ConditionOutcome.match(ConditionMessage
                .forCondition("NotConsumerTransportCondition")
                .because("transport '" + transport + "' is not a consumer transport"));
    }
}
