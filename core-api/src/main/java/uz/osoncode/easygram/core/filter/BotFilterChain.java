package uz.osoncode.easygram.core.filter;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

/**
 * Contract for advancing the bot filter/handler chain.
 *
 * <p>Implementations pass control to the next element in the processing pipeline
 * (either the next {@link BotFilter} or the final dispatcher).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface BotFilterChain {

    /**
     * Invokes the next filter or the dispatcher in the chain.
     *
     * @param botRequest  the current request context; must not be {@code null}
     * @param botResponse the mutable response object; must not be {@code null}
     */
    void doFilter(BotRequest botRequest, BotResponse botResponse);
}
