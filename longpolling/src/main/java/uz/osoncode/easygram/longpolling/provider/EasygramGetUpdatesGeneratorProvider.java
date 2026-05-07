package uz.osoncode.easygram.longpolling.provider;

import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;

import java.util.function.Function;

/**
 * Provider for the factory function that builds a {@link GetUpdates} request for a given update
 * offset.
 *
 * <p>Register a Spring bean of this type to customise the batch size, timeout, or allowed
 * update types without re-creating the entire bot configuration:</p>
 *
 * <pre>{@code
 * @Bean
 * public EasygramGetUpdatesGeneratorProvider botGetUpdatesGeneratorProvider() {
 *     return () -> offset -> GetUpdates.builder()
 *             .offset(offset + 1)
 *             .limit(50)
 *             .timeout(30)
 *             .allowedUpdates(List.of("message", "callback_query"))
 *             .build();
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface EasygramGetUpdatesGeneratorProvider {

    /**
     * Returns the function used to produce {@link GetUpdates} requests.
     *
     * @return a non-null {@link Function} mapping update offset to {@link GetUpdates}
     */
    Function<Integer, GetUpdates> provide();
}
