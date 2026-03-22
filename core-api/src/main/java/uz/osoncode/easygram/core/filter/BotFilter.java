package uz.osoncode.easygram.core.filter;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

/**
 * Interceptor interface for the bot update processing pipeline.
 * Filters wrap around the handler dispatch, enabling cross-cutting concerns such as
 * authentication, logging, and exception handling to be applied before and after
 * the core handler chain runs.
 * Filters are invoked in ascending {@link #getOrder()} order and may delegate to the
 * next element in the chain via the provided {@link BotFilterChain}.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotFilter extends Comparable<BotFilter> {

    /**
     * Returns the priority order of this filter.
     * Lower values indicate higher priority. Defaults to {@link Integer#MAX_VALUE}.
     *
     * @return the order value for this filter
     */
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * Performs filter logic and delegates to the next filter or handler in the chain.
     * Implementations should call {@code filterChain.doFilter(botRequest, botResponse)}
     * to continue the pipeline.
     *
     * @param botRequest  the current request context; must not be {@code null}
     * @param botResponse the mutable response object; must not be {@code null}
     * @param filterChain the remaining filter/handler chain to delegate to; must not be {@code null}
     */
    void doFilter(BotRequest botRequest, BotResponse botResponse, BotFilterChain filterChain);

    /**
     * Compares this filter to another for ordering purposes.
     *
     * @param o the other {@link BotFilter} to compare to; must not be {@code null}
     * @return a negative integer, zero, or a positive integer as this filter's order
     *         is less than, equal to, or greater than the other filter's order
     */
    @Override
    default int compareTo(BotFilter o) {
        return Integer.compare(getOrder(), o.getOrder());
    }

    /**
     * Determines whether this filter should be applied for the current request.
     * Return {@code false} to skip this filter entirely for the given request/response pair.
     *
     * @param botRequest  the current request context; must not be {@code null}
     * @param botResponse the mutable response object; must not be {@code null}
     * @return {@code true} if the filter should execute, {@code false} to skip it
     */
    default boolean shouldFilter(BotRequest botRequest, BotResponse botResponse) {
        return Boolean.TRUE;
    }
}
