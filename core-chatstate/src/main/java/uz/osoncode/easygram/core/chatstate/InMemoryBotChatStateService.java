package uz.osoncode.easygram.core.chatstate;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation of {@link BotChatStateService} backed by a
 * {@link ConcurrentHashMap}.
 *
 * <p>State entries are stored in heap memory and are therefore not persisted across
 * application restarts. This implementation is suitable for single-instance bot deployments
 * where durability is not required. For clustered or persistent scenarios a custom
 * {@link BotChatStateService} bean (e.g. Redis-backed) should be provided instead.</p>
 *
 * <p>All operations are thread-safe because the underlying map is a
 * {@link ConcurrentHashMap}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class InMemoryBotChatStateService implements BotChatStateService {

    /** Thread-safe map from chat ID to the current state string. */
    private final ConcurrentHashMap<Long, String> stateMap = new ConcurrentHashMap<>();

    /**
     * Returns the current state associated with the given chat ID.
     *
     * <p>Returns {@code null} if no state has been set for the chat.</p>
     *
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     * @return the state string previously stored for {@code chatId}, or {@code null} if absent
     */
    @Override
    public String getState(Long chatId) {
        return stateMap.get(chatId);
    }

    /**
     * Sets or removes the state for the given chat ID.
     *
     * <p>If {@code state} is {@code null} the entry for {@code chatId} is removed from the
     * map, effectively clearing any previously stored state. Otherwise the new state value
     * is stored, replacing any previous value.</p>
     *
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     * @param state  the new state string to store, or {@code null} to remove the existing state
     */
    @Override
    public void setState(Long chatId, String state) {
        if (state == null) {
            stateMap.remove(chatId);
        } else {
            stateMap.put(chatId, state);
        }
    }
}
