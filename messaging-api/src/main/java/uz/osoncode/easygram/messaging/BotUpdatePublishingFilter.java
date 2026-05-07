package uz.osoncode.easygram.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.BotFilterChain;
import uz.osoncode.easygram.core.filter.BotFilterOrder;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

/**
 * A {@link BotFilter} that publishes every incoming Telegram {@link Update} to an external
 * message broker via the configured {@link BotUpdatePublisher}.
 *
 * <p>This filter runs at order {@code Integer.MIN_VALUE + 1000} ({@link BotFilterOrder#PUBLISHING}),
 * which places it after the MDC, context-setter, observation, and API-sender filters in the
 * default chain. Running after the context-setter ensures that the full
 * {@link uz.osoncode.easygram.core.model.BotRequest} context (user, chat) is available when
 * the update is forwarded to the broker.</p>
 *
 * <p>Behaviour is governed by {@link EasygramMessagingProperties#forwardOnly()}:</p>
 * <ul>
 *   <li>{@code false} (default): the update is published <em>and</em> the filter chain
 *       continues normally so bot handler methods still receive the update.</li>
 *   <li>{@code true}: the update is published and the chain is stopped — bot handler
 *       methods are skipped entirely. Useful when the bot acts purely as a relay.</li>
 * </ul>
 *
 * <p><strong>Consumer-transport note:</strong> autoconfiguration suppresses this filter when
 * {@code easygram.update.transport} is {@code RABBIT_CONSUMER} or {@code KAFKA_CONSUMER}.
 * Registering the filter in those contexts would cause every consumed update to be immediately
 * re-published to the same broker queue, creating an infinite processing loop. To use a
 * publishing filter in consumer mode anyway, register your own {@code BotUpdatePublishingFilter}
 * bean — the {@code @ConditionalOnMissingBean} guard will defer to it.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class BotUpdatePublishingFilter implements BotFilter {

    private static final int ORDER = BotFilterOrder.PUBLISHING;

    private final BotUpdatePublisher botUpdatePublisher;
    private final EasygramMessagingProperties botPublishingProperties;

    /**
     * Publishes the incoming update and optionally stops the filter chain.
     *
     * @param request  the current bot request containing the Telegram update
     * @param response the current bot response accumulator
     * @param chain    the remaining filter chain
     */
    @Override
    public void doFilter(BotRequest request, BotResponse response, BotFilterChain chain) {
        Update update = request.getUpdate();
        try {
            botUpdatePublisher.publish(update);
            log.debug("Published update id={} to message broker", update.getUpdateId());
        } catch (Exception e) {
            log.error("Failed to publish update id={} to message broker", update.getUpdateId(), e);
            if (Boolean.TRUE.equals(botPublishingProperties.failOnPublishError())) {
                throw new RuntimeException(
                        "Broker publish failed for update id=" + update.getUpdateId()
                        + "; easygram.messaging.fail-on-publish-error=true", e);
            }
        }

        if (!Boolean.TRUE.equals(botPublishingProperties.forwardOnly())) {
            chain.doFilter(request, response);
        }
    }

    /**
     * Returns the order of this filter.
     * A very low value ensures this filter runs before all other filters.
     *
     * @return {@code Integer.MIN_VALUE + 1000}
     */
    @Override
    public int getOrder() {
        return ORDER;
    }
}
