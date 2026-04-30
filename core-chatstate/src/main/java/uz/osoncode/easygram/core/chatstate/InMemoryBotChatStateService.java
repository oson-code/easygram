package uz.osoncode.easygram.core.chatstate;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.Objects;
import java.util.Optional;
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
 * <p><strong>⚠ Not suitable for production or multi-instance deployments.</strong>
 * Each pod/instance maintains independent state — conversations that span multiple
 * restarts or instances will behave incorrectly. The map also grows without bound;
 * replace this bean with a Redis-backed implementation for any non-trivial workload.</p>
 *
 * <p>All operations are thread-safe because the underlying map is a
 * {@link ConcurrentHashMap}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class InMemoryBotChatStateService implements BotChatStateService, SmartInitializingSingleton {

    /** Thread-safe map from chat ID to the current state string. */
    private final ConcurrentHashMap<Long, String> stateMap = new ConcurrentHashMap<>();

    private final Counter getHitCounter;
    private final Counter getMissCounter;
    private final Counter setCounter;
    private final Counter clearCounter;

    /**
     * Creates an instance with optional Micrometer instrumentation.
     *
     * @param meterRegistry an {@link Optional} {@link MeterRegistry}; when present, records
     *                      {@code easygram.chatstate.get}, {@code easygram.chatstate.set},
     *                      and {@code easygram.chatstate.clear} counters
     * @since 0.0.7
     */
    public InMemoryBotChatStateService(Optional<MeterRegistry> meterRegistry) {
        MeterRegistry registry = meterRegistry.orElse(null);
        if (registry != null) {
            getHitCounter  = Counter.builder("easygram.chatstate.get").tag("result", "hit").register(registry);
            getMissCounter = Counter.builder("easygram.chatstate.get").tag("result", "miss").register(registry);
            setCounter     = Counter.builder("easygram.chatstate.set").register(registry);
            clearCounter   = Counter.builder("easygram.chatstate.clear").register(registry);
        } else {
            getHitCounter = getMissCounter = setCounter = clearCounter = null;
        }
    }

    /**
     * Logs a startup warning reminding operators that this implementation is not
     * production-ready.
     */
    @Override
    public void afterSingletonsInstantiated() {
        log.warn("Easygram is using InMemoryBotChatStateService. " +
                 "This implementation is NOT suitable for production or multi-instance deployments — " +
                 "state is lost on restart and not shared across pods. " +
                 "Provide a Redis-backed (or persistent) BotChatStateService @Bean to suppress this warning.");
    }

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
        if (state != null) {
            if (getHitCounter != null) getHitCounter.increment();
        } else {
            if (getMissCounter != null) getMissCounter.increment();
        }
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
        if (setCounter != null) setCounter.increment();
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
        if (clearCounter != null) clearCounter.increment();
        log.debug("Chat state cleared: chatId={} removedState={}", chatId, removed);
    }
}
