package uz.osoncode.easygram.core.model;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Mutable response object that accumulates {@link BotApiMethod} instances produced by handler methods.
 * Collected methods are later executed by the {@code BotApiMethodsSenderFilter} after the handler
 * chain completes.
 *
 * <p>The attributes map provides a response-scoped key-value store that allows filters and
 * return type handlers to attach metadata to the response (e.g. sent message IDs, pagination
 * state) for downstream consumption within the same update lifecycle.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Getter
public class BotResponse {

    /** The ordered collection of Telegram API methods queued for execution. */
    private Collection<BotApiMethod<?>> botApiMethods = new LinkedList<>();

    /** Response-scoped attribute store for passing metadata between processing components. */
    private Map<String, Object> attributes = new LinkedHashMap<>();

    /**
     * Appends a single {@link BotApiMethod} to the response queue.
     *
     * @param botApiMethod the API method to queue for execution; must not be {@code null}
     */
    public void addBotApiMethod(BotApiMethod<?> botApiMethod) {
        this.botApiMethods.add(botApiMethod);
    }

    /**
     * Appends all elements from the given collection of {@link BotApiMethod} instances to the response queue.
     *
     * @param botApiMethods the collection of API methods to queue; must not be {@code null}
     */
    public void addBotApiMethods(Collection<BotApiMethod<?>> botApiMethods) {
        this.botApiMethods.addAll(botApiMethods);
    }

    /**
     * Stores an attribute under the given key, replacing any previous value.
     *
     * @param key   the attribute key; must not be {@code null}
     * @param value the attribute value; may be {@code null} to remove the key
     */
    public void setAttribute(String key, Object value) {
        if (value == null) {
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
     * Returns an unmodifiable view of all response attributes.
     *
     * @return the attribute map; never {@code null}
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
