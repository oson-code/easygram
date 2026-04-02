package uz.osoncode.easygram.core.chatstate;

import java.util.Objects;

/**
 * Service interface for managing per-chat conversational state.
 * Implementations persist and retrieve a string-based state token for each chat,
 * enabling multi-step conversation flows that are conditionally activated via
 * {@link BotChatState}.
 *
 * <p>Enum-friendly overloads are provided so callers can pass enum constants directly
 * and receive IDE autocompletion, instead of writing raw strings:</p>
 * <pre>{@code
 * // Instead of: chatStateService.setState(chatId, "WAITING_FOR_NAME")
 * chatStateService.setState(chatId, MyState.WAITING_FOR_NAME);
 *
 * // Instead of: MyState.valueOf(chatStateService.getState(chatId))
 * MyState state = chatStateService.getStateAs(chatId, MyState.class);
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotChatStateService {

    /**
     * Retrieves the current state for the specified chat.
     *
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     * @return the current state string for the chat, or {@code null} if no state has been set
     */
    String getState(Long chatId);

    /**
     * Stores or updates the state for the specified chat.
     *
     * <p>To remove the current state use {@link #clearState(Long)} — passing {@code null}
     * here is not permitted and will throw an {@link IllegalArgumentException}.</p>
     *
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     * @param state  the new state value to persist; must not be {@code null}
     * @throws IllegalArgumentException if {@code state} is {@code null}
     */
    void setState(Long chatId, String state);

    /**
     * Stores the enum constant's {@link Enum#name()} as the state for the specified chat.
     * This overload enables IDE autocompletion when callers work with enum-defined states.
     *
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     * @param state  the enum constant whose name is persisted; must not be {@code null}
     */
    default void setState(Long chatId, Enum<?> state) {
        java.util.Objects.requireNonNull(state, "state must not be null");
        setState(chatId, state.name());
    }

    /**
     * Clears (removes) the current state for the specified chat.
     *
     * <p>This is the only supported way to reset a chat's state. Calling
     * {@code setState(chatId, null)} is not permitted and will throw an
     * {@link IllegalArgumentException}.</p>
     *
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     */
    void clearState(Long chatId);

    /**
     * Retrieves the current state for the specified chat and parses it as an enum constant
     * of the given type. Returns {@code null} if no state is set.
     *
     * <pre>{@code
     * MyState current = chatStateService.getStateAs(chatId, MyState.class);
     * }</pre>
     *
     * @param <T>    the enum type
     * @param chatId the unique Telegram chat identifier; must not be {@code null}
     * @param type   the enum class to parse the stored string into; must not be {@code null}
     * @return the matching enum constant, or {@code null} if no state has been set
     * @throws IllegalArgumentException if the stored string does not match any constant of {@code type}
     */
    default <T extends Enum<T>> T getStateAs(Long chatId, Class<T> type) {
        String raw = getState(chatId);
        return Objects.nonNull(raw) ? Enum.valueOf(type, raw) : null;
    }
}
