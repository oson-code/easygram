package uz.osoncode.easygram.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contextual request object that is passed through the filter chain and into handler methods.
 * Aggregates all data relevant to processing a single incoming Telegram update, including
 * the raw {@link Update}, the {@link TelegramClient} for sending responses, the resolved
 * {@link User} and {@link Chat}, and any {@link Throwable} raised during processing.
 *
 * <p>The attributes map provides a request-scoped key-value store that allows filters,
 * handlers, and {@code @BotMarkup} factory methods to communicate without coupling.
 * Use {@link #setAttribute(String, Object)} and {@link #getAttribute(String)} to share
 * data within a single update processing lifecycle.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BotRequest {

    /** The incoming Telegram update containing all event data. */
    private Update update;

    /** The client used to send API requests back to the Telegram Bot API. */
    private TelegramClient telegramClient;

    /** The Telegram user who triggered this update, resolved from the update payload. */
    private User user;

    /** The Telegram chat in which this update occurred, resolved from the update payload. */
    private Chat chat;

    /** The exception thrown during update processing, populated when an error occurs in the handler chain. */
    private Throwable throwable;

    /** Metadata about the bot processing this request. */
    private BotMetadata botMetadata;

    /** Request-scoped attribute store for cross-component communication within a single update lifecycle. */
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Stores an attribute under the given key, replacing any previous value.
     *
     * @param key   the attribute key; must not be {@code null}
     * @param value the attribute value; may be {@code null} to remove the key
     */
    public void setAttribute(String key, Object value) {
        if (Objects.isNull(value)) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
    }

    /**
     * Returns the attribute stored under the given key, or {@code null} if absent.
     *
     * @param key the attribute key; must not be {@code null}
     * @param <T> the expected value type
     * @return the stored value cast to {@code T}, or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * Returns the attribute stored under the given key cast to the specified type,
     * or {@code null} if absent.
     *
     * <p>Prefer this overload over {@link #getAttribute(String)} when the expected type
     * is known — it produces a clear {@link ClassCastException} message instead of a
     * confusing downstream {@link ClassCastException} at the call site.</p>
     *
     * @param key  the attribute key; must not be {@code null}
     * @param type the class to cast the stored value to; must not be {@code null}
     * @param <T>  the expected value type
     * @return the stored value cast to {@code T}, or {@code null} if not present
     * @throws ClassCastException if the stored value is not an instance of {@code type}
     * @since 0.0.7
     */
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value == null) return null;
        return type.cast(value);
    }

    /**
     * Returns an unmodifiable view of all attributes in this request.
     *
     * @return the attribute map; never {@code null}
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

}
