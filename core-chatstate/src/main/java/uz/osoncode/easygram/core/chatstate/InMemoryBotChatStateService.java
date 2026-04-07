package uz.osoncode.easygram.core.chatstate;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
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
@Slf4j
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
        String state = stateMap.get(chatId);
        log.trace("Chat state get: chatId={} state={}", chatId, state);
        return state;
    }

    /**
     * Sets the state for the given chat ID.
     *
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     * @param state  the new state string to store; must not be {@code null} — use
     *               {@link #clearState(Long)} to remove the current state
     * @throws IllegalArgumentException if {@code state} is {@code null}
     */
    @Override
    public void setState(Long chatId, String state) {
        if (Objects.isNull(state)) {
            throw new IllegalArgumentException("state must not be null — use clearState(chatId) to remove the current state");
        }
        String previous = stateMap.put(chatId, state);
        log.debug("Chat state set: chatId={} previousState={} newState={}", chatId, previous, state);
    }

    /**
     * Removes the state entry for the given chat ID directly, without going through
     * the {@code null}-branch of {@link #setState}.
     *
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     */
    @Override
    public void clearState(Long chatId) {
        String removed = stateMap.remove(chatId);
        log.debug("Chat state cleared: chatId={} removedState={}", chatId, removed);
    }
}
