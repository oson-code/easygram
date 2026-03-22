package uz.osoncode.easygram.longpolling.provider;

import org.telegram.telegrambots.longpolling.interfaces.BackOff;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;

/**
 * Provider for the {@link BackOff} strategy applied when a long-poll request fails.
 *
 * <p>Register a Spring bean of this type to replace the default {@link ExponentialBackOff} with
 * a fixed-delay, custom, or no-op back-off strategy:</p>
 *
 * <pre>{@code
 * @Bean
 * public BotBackOffProvider botBackOffProvider() {
 *     // Fixed 5-second delay between retries
 *     return () -> new FixedBackOff(5_000L, Long.MAX_VALUE);
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface BotBackOffProvider {

    /**
     * Returns the {@link BackOff} strategy to use when the long-poll request fails.
     *
     * @return a non-null {@link BackOff}
     */
    BackOff provide();
}
