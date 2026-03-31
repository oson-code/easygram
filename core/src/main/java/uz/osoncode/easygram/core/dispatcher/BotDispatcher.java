package uz.osoncode.easygram.core.dispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uz.osoncode.easygram.core.handler.BotHandler;
import uz.osoncode.easygram.core.handler.BotHandlerRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.InvocationTargetException;

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
        return botHandlerRegistry.getStateHandlers().stream()
                .filter(h -> h.supports(botRequest))
                .peek(h -> log.debug("Matched state handler: {}", h.info()))
                .findAny()
                .orElseGet(() -> botHandlerRegistry.getBotHandlers().stream()
                        .filter(h -> h.supports(botRequest))
                        .peek(h -> log.debug("Matched specific handler: {}", h.info()))
                        .findAny()
                        .orElseGet(() -> botHandlerRegistry.getDefaultHandlers().stream()
                                .filter(h -> h.supports(botRequest))
                                .peek(h -> log.debug("Matched default handler: {}", h.info()))
                                .findAny()
                                .orElseThrow(() -> new IllegalStateException(
                                        "No handler found for update: " + botRequest.getUpdate()))));
    }
}
