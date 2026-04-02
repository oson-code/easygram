package uz.osoncode.easygram.core.dynamiccallback;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation of {@link BotDynamicCallbackQueryService}.
 *
 * <p>Stores payloads in a {@link ConcurrentHashMap}, making it safe for concurrent
 * multi-threaded use. The map is unbounded — callers should invoke {@link #remove} after
 * processing a callback to avoid memory growth in long-running bots.</p>
 *
 * <p>This bean is registered with {@code @ConditionalOnMissingBean} so it can be replaced
 * by any custom {@code @Bean} implementation (e.g., a Redis- or database-backed store).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.4
 * @see BotDynamicCallbackQueryService
 */
public class InMemoryBotDynamicCallbackQueryService implements BotDynamicCallbackQueryService {

    private final ConcurrentHashMap<String, BotDynamicCallbackData> store = new ConcurrentHashMap<>();

    /**
     * Returns the payload stored under {@code callbackData}, or {@code null} if absent.
     *
     * @param callbackData the raw Telegram callback data string; must not be {@code null}
     * @return the stored {@link BotDynamicCallbackData}, or {@code null}
     */
    @Override
    public BotDynamicCallbackData resolve(String callbackData) {
        return store.get(callbackData);
    }

    /**
     * Stores {@code payload} under {@code callbackData}, replacing any previous mapping.
     *
     * @param callbackData the lookup key; must not be {@code null}
     * @param payload      the payload to store; must not be {@code null}
     */
    @Override
    public void store(String callbackData, BotDynamicCallbackData payload) {
        store.put(callbackData, payload);
    }

    /**
     * Removes the entry for {@code callbackData} if present; otherwise a no-op.
     *
     * @param callbackData the key to remove; must not be {@code null}
     */
    @Override
    public void remove(String callbackData) {
        store.remove(callbackData);
    }
}
