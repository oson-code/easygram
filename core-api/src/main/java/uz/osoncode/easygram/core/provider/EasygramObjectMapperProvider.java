package uz.osoncode.easygram.core.provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provider for the Jackson {@link ObjectMapper} used throughout the bot framework.
 *
 * <p>Register a Spring bean of this type to replace the default {@code ObjectMapper} with a
 * fully customised one (custom modules, features, naming strategy, etc.) without touching
 * any transport-specific configuration:</p>
 *
 * <pre>{@code
 * @Bean
 * public EasygramObjectMapperProvider botObjectMapperProvider() {
 *     ObjectMapper mapper = new ObjectMapper()
 *             .registerModule(new JavaTimeModule())
 *             .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 *     return () -> mapper;
 * }
 * }</pre>
 *
 * <p>The framework guarantees that {@link #provide()} is called at most once per bot instance
 * during startup, so the returned instance will be shared across all internal consumers.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface EasygramObjectMapperProvider {

    /**
     * Returns the {@link ObjectMapper} to use for Telegram API payload serialisation and
     * deserialisation.
     *
     * @return a non-null {@link ObjectMapper}
     */
    ObjectMapper provide();
}
