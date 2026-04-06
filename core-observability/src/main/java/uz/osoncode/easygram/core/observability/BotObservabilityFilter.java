package uz.osoncode.easygram.core.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.BotFilterChain;
import uz.osoncode.easygram.core.filter.BotFilterOrder;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.Objects;

/**
 * {@link BotFilter} that wraps each Telegram update in a Micrometer {@link Observation},
 * producing both metrics and distributed traces when the appropriate bridges are on the
 * classpath.
 *
 * <p><b>Metrics produced:</b></p>
 * <ul>
 *   <li>{@code easygram.update} — timer recording count, total duration and error rate.</li>
 * </ul>
 *
 * <p><b>Low-cardinality tags</b> (included in both metrics and spans):</p>
 * <ul>
 *   <li>{@code update.type} — e.g. {@code message}, {@code callback_query}, {@code inline_query}.</li>
 *   <li>{@code transport.type} — e.g. {@code LONG_POLLING}, {@code WEBHOOK}, {@code KAFKA_CONSUMER}.</li>
 * </ul>
 * <p><b>High-cardinality tags</b> (included in spans/traces only, not in metric labels):</p>
 * <ul>
 *   <li>{@code user.id} — Telegram user ID (when resolvable from the update).</li>
 *   <li>{@code chat.id} — Telegram chat ID (when resolvable from the update).</li>
 * </ul>
 *
 * <p><b>Tracing:</b>
 * When a Brave or OpenTelemetry bridge is on the classpath, this filter automatically
 * creates a span named {@code easygram.update} for each processed update with no
 * additional configuration.</p>
 *
 * <p>This filter runs at order {@link BotFilterOrder#OBSERVATION}, just after
 * {@link BotFilterOrder#CONTEXT_SETTER} so that user/chat context is already populated
 * when the observation is opened.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotObservabilityFilter implements BotFilter {

    /** The Micrometer observation registry used to create and track observations. */
    private final ObservationRegistry observationRegistry;

    /** Provides the active transport type for the {@code transport.type} tag. */
    private final BotConfigurer botConfigurer;

    /**
     * Creates a new {@code BotObservabilityFilter}.
     *
     * @param observationRegistry the registry to record observations into
     * @param botConfigurer       the bot configurer carrying the active transport type
     */
    public BotObservabilityFilter(ObservationRegistry observationRegistry, BotConfigurer botConfigurer) {
        this.observationRegistry = observationRegistry;
        this.botConfigurer = botConfigurer;
    }

    /**
     * Returns the order of this filter — just after {@link BotFilterOrder#CONTEXT_SETTER}
     * so that user/chat context is already populated when the observation is opened.
     */
    @Override
    public int getOrder() {
        return BotFilterOrder.OBSERVATION;
    }

    /**
     * Wraps the remaining filter chain in a Micrometer {@link Observation}.
     *
     * <p>The observation is started before the chain runs. After the chain returns (all
     * exceptions are swallowed by {@code DefaultBotFilterChain}), the error is read from
     * {@link BotRequest#getThrowable()} and recorded on the observation so that error-rate
     * metrics and span status are set correctly.</p>
     *
     * @param botRequest  the current request context
     * @param botResponse the mutable response object
     * @param filterChain the remaining filter/handler pipeline to invoke
     */
    @Override
    public void doFilter(BotRequest botRequest, BotResponse botResponse, BotFilterChain filterChain) {
        Observation observation = Observation
                .createNotStarted("easygram.update", observationRegistry)
                .lowCardinalityKeyValue("update.type", resolveUpdateType(botRequest.getUpdate()))
                .lowCardinalityKeyValue("transport.type", botConfigurer.transportType().name());

        if (Objects.nonNull(botRequest.getUser())) {
            observation.highCardinalityKeyValue("user.id", String.valueOf(botRequest.getUser().getId()));
        }
        if (Objects.nonNull(botRequest.getChat())) {
            observation.highCardinalityKeyValue("chat.id", String.valueOf(botRequest.getChat().getId()));
        }

        observation.start();
        try {
            filterChain.doFilter(botRequest, botResponse);
            if (Objects.nonNull(botRequest.getThrowable())) {
                observation.error(botRequest.getThrowable());
            }
        } catch (Exception e) {
            observation.error(e);
            throw e;
        } finally {
            observation.stop();
        }
    }

    /**
     * Resolves a low-cardinality string label for the Telegram update type.
     *
     * @param update the incoming Telegram update
     * @return a lowercase string identifying the update type
     */
    private String resolveUpdateType(Update update) {
        if (Objects.isNull(update))             return "unknown";
        if (update.hasMessage())        return "message";
        if (update.hasCallbackQuery())  return "callback_query";
        if (update.hasInlineQuery())    return "inline_query";
        if (update.hasEditedMessage())  return "edited_message";
        if (update.hasChannelPost())    return "channel_post";
        if (update.hasEditedChannelPost()) return "edited_channel_post";
        if (update.hasChosenInlineQuery()) return "chosen_inline_result";
        if (update.hasShippingQuery())  return "shipping_query";
        if (update.hasPreCheckoutQuery()) return "pre_checkout_query";
        if (update.hasPoll())           return "poll";
        if (update.hasPollAnswer())     return "poll_answer";
        if (update.hasMyChatMember())   return "my_chat_member";
        if (update.hasChatMember())     return "chat_member";
        if (update.hasChatJoinRequest()) return "chat_join_request";
        if (update.hasBusinessConnection()) return "business_connection";
        if (update.hasBusinessMessage()) return "business_message";
        if (update.hasEditedBusinessMessage()) return "edited_business_message";
        if (update.hasDeletedBusinessMessage()) return "deleted_business_message";
        if (update.hasPaidMediaPurchased()) return "paid_media_purchased";
        return "unknown";
    }
}
