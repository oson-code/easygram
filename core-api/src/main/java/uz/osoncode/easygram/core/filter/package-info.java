/**
 * Filter pipeline contracts and ordering constants for the Easygram framework.
 *
 * <p>Every incoming Telegram update passes through a sorted list of {@link uz.osoncode.easygram.core.filter.BotFilter}
 * implementations before being handed to a {@link uz.osoncode.easygram.core.handler.BotHandler}.
 * Filters are ordered by {@link uz.osoncode.easygram.core.filter.BotFilter#getOrder()} (ascending —
 * lower values run first) and are chained via {@link uz.osoncode.easygram.core.filter.BotFilterChain}.</p>
 *
 * <h2>Key types</h2>
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.filter.BotFilter} — the core interceptor interface; implement this to add cross-cutting concerns</li>
 *   <li>{@link uz.osoncode.easygram.core.filter.BotFilterChain} — delegates to the next filter (or the handler) in the pipeline</li>
 *   <li>{@link uz.osoncode.easygram.core.filter.BotFilterOrder} — ordering constants for all built-in filters</li>
 * </ul>
 *
 * <h2>Built-in filter execution order</h2>
 * <ol>
 *   <li>{@code BotContextSetterFilter} (order {@link uz.osoncode.easygram.core.filter.BotFilterOrder#CONTEXT_SETTER}) — resolves {@code User} and {@code Chat} on the request</li>
 *   <li>{@code BotObservabilityFilter} (order {@link uz.osoncode.easygram.core.filter.BotFilterOrder#OBSERVATION}) — wraps the update in a Micrometer {@code Observation}</li>
 *   <li>{@code BotApiMethodsSenderFilter} (order {@link uz.osoncode.easygram.core.filter.BotFilterOrder#API_SENDER}) — sends all queued {@link org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod} calls after the handler chain</li>
 *   <li>{@code BotUpdatePublishingFilter} (order {@link uz.osoncode.easygram.core.filter.BotFilterOrder#PUBLISHING}) — forwards the update to a message broker</li>
 *   <li>Custom filters — default order {@link Integer#MAX_VALUE} (run last)</li>
 * </ol>
 *
 * <h2>Implementing a custom filter</h2>
 * <pre>{@code
 * @Component
 * public class RateLimitFilter implements BotFilter {
 *
 *     @Override
 *     public int getOrder() { return 100; }
 *
 *     @Override
 *     public void doFilter(BotRequest req, BotResponse res, BotFilterChain chain) {
 *         if (!rateLimiter.tryAcquire(req.getUser().getId())) {
 *             return; // drop the update — do not call chain.doFilter()
 *         }
 *         chain.doFilter(req, res);
 *     }
 * }
 * }</pre>
 */
package uz.osoncode.easygram.core.filter;
