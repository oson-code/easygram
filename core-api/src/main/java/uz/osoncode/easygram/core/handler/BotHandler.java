package uz.osoncode.easygram.core.handler;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

/**
 * Core strategy interface for processing incoming Telegram updates.
 * Each implementation declares which {@link BotRequest} types it can process via
 * {@link #supports(BotRequest)} and performs the actual processing in
 * {@link #handle(BotRequest, BotResponse)}.
 * Handlers are ordered by {@link #getOrder()} so that a sorted list of handlers is
 * evaluated in priority sequence.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotHandler extends Comparable<BotHandler> {

    /**
     * Compares this handler to another for ordering purposes.
     * Handlers with lower order values are evaluated first.
     *
     * @param o the other {@link BotHandler} to compare to; must not be {@code null}
     * @return a negative integer, zero, or a positive integer as this handler's order
     *         is less than, equal to, or greater than the other handler's order
     */
    default int compareTo(BotHandler o) {
        return Integer.compare(this.getOrder(), o.getOrder());
    }

    /**
     * Returns the priority order of this handler.
     * Lower values indicate higher priority. Defaults to {@link Integer#MAX_VALUE}.
     *
     * @return the order value for this handler
     */
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * Determines whether this handler is capable of processing the given request.
     *
     * @param botRequest the current request context containing the incoming update; must not be {@code null}
     * @return {@code true} if this handler can process the request, {@code false} otherwise
     */
    boolean supports(BotRequest botRequest);

    /**
     * Processes the incoming Telegram update encapsulated in the request and populates the response.
     *
     * @param botRequest  the current request context containing the incoming update; must not be {@code null}
     * @param botResponse the mutable response object to which API methods may be added; must not be {@code null}
     */
    void handle(BotRequest botRequest, BotResponse botResponse);
}
