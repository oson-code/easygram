package uz.osoncode.easygram.core.dispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.handler.BotHandler;
import uz.osoncode.easygram.core.handler.BotHandlerRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Central dispatcher responsible for routing an incoming {@link BotRequest} to
 * the appropriate {@link uz.osoncode.easygram.core.handler.BotHandler}.
 *
 * <p>On each call to {@link #dispatch}, this dispatcher searches the three handler tiers
 * in order:</p>
 * <ol>
 *   <li><em>State handlers</em> — handlers that require a specific chat state; checked first.</li>
 *   <li><em>Specific handlers</em> — state-agnostic handlers targeting a concrete update
 *       condition (command, text pattern, callback data, …); checked second.</li>
 *   <li><em>Default handlers</em> — catch-all fallbacks; checked last.</li>
 * </ol>
 * <p>If no handler in any tier supports the request, an {@link IllegalStateException} is thrown.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public final class BotDispatcher {

    /**
     * Registry holding all registered state, specific, and default bot handlers.
     */
    private final BotHandlerRegistry botHandlerRegistry;

    /**
     * Dispatches the given {@link BotRequest} to the first matching handler.
     *
     * <p>The three tiers of the registry are searched in order:
     * {@code stateHandlers} →
     * {@code botHandlers} →
     * {@code defaultHandlers}.
     * The first matching handler's
     * {@link uz.osoncode.easygram.core.handler.BotHandler#handle} method is invoked.
     *
     * @param botRequest  the incoming bot request containing the Telegram {@code Update}.
     * @param botResponse the response object that the handler may populate with API methods.
     * @throws IllegalStateException if no handler in any tier supports the current request.
     */
    public void dispatch(BotRequest botRequest, BotResponse botResponse) throws InvocationTargetException, IllegalAccessException {
        resolveHandler(botRequest).handle(botRequest, botResponse);
    }

    private BotHandler resolveHandler(BotRequest botRequest) {
        return findInTier(botHandlerRegistry.getStateHandlers(), botRequest, "state")
                .orElseGet(() -> findInTier(botHandlerRegistry.getBotHandlers(), botRequest, "specific")
                        .orElseGet(() -> findInTier(botHandlerRegistry.getDefaultHandlers(), botRequest, "default")
                                .orElseThrow(() -> {
                                    Update update = botRequest.getUpdate();
                                    log.warn("No handler matched for updateId={} update={}",
                                            Objects.nonNull(update) ? update.getUpdateId() : null, update);
                                    return new IllegalStateException("No handler found for update: " + update);
                                })));
    }

    private Optional<BotHandler> findInTier(
            List<BotHandler> tier, BotRequest botRequest, String tierName) {
        if (log.isTraceEnabled()) {
            Update u = botRequest.getUpdate();
            Integer updateId = u != null ? u.getUpdateId() : null;
            tier.forEach(h -> {
                if (!h.supports(botRequest)) {
                    log.trace("Tier '{}' handler '{}' rejected updateId={}", tierName, h.info(), updateId);
                }
            });
        }
        List<BotHandler> matches = tier.stream()
                .filter(h -> h.supports(botRequest))
                .toList();
        if (matches.isEmpty()) {
            if (log.isDebugEnabled()) {
                Update u = botRequest.getUpdate();
                log.debug("Tier '{}' had no matching handler for updateId={} (tier size={})",
                        tierName, u != null ? u.getUpdateId() : null, tier.size());
            }
            return Optional.empty();
        }
        if (matches.size() > 1) {
            log.warn("Ambiguous handler dispatch in '{}' tier — {} handlers matched the same request. " +
                     "Using first. Matched: {}",
                    tierName, matches.size(),
                    matches.stream().map(BotHandler::info).toList());
        }
        BotHandler chosen = matches.get(0);
        log.debug("Matched {} handler: {}", tierName, chosen.info());
        return Optional.of(chosen);
    }
}
