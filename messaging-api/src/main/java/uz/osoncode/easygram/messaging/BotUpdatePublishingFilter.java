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
 * <p>This filter is placed at the very beginning of the filter chain (order
 * {@code Integer.MIN_VALUE + 1000}) so that every update — regardless of which transport
 * delivered it (long-polling or webhook) — is forwarded to the broker.</p>
 *
 * <p>Behaviour is governed by {@link EasygramMessagingProperties#forwardOnly()}:</p>
 * <ul>
 *   <li>{@code false} (default): the update is published <em>and</em> the filter chain
 *       continues normally so bot handler methods still receive the update.</li>
 *   <li>{@code true}: the update is published and the chain is stopped — bot handler
 *       methods are skipped entirely. Useful when the bot acts purely as a relay.</li>
 * </ul>
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
