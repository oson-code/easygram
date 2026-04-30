package uz.osoncode.easygram.core.markup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Typed wrapper for parameters passed to a {@link BotMarkupRegistry} factory method
 * at request time.
 *
 * <p>When a handler returns a {@link MarkupAware} value that carries markup parameters
 * (via {@link MarkupAware#withMarkup(String, Map)}), the framework stores a
 * {@code BotMarkupContext} as a
 * {@link uz.osoncode.easygram.core.model.BotRequest} attribute under
 * {@link #REQUEST_ATTRIBUTE_KEY} just before invoking
 * {@link BotMarkupRegistry#resolve(String, uz.osoncode.easygram.core.model.BotRequest)}.
 * The registered {@code @BotMarkup} factory method can then declare a
 * {@code BotMarkupContext} parameter and receive those parameters through the
 * framework's argument resolver.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Handler passes page number to the keyboard factory
 * return PlainReply.of("Items:")
 *     .withMarkup("item_list", Map.of("page", currentPage, "size", 5));
 *
 * // @BotMarkup factory declares BotMarkupContext
 * @BotMarkup("item_list")
 * public InlineKeyboardMarkup itemList(BotRequest request, BotMarkupContext ctx) {
 *     int page = ctx.get("page", Integer.class);
 *     int size = ctx.getOrDefault("size", 10);
 *     return buildInlineKeyboard(page, size);
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see MarkupAware#withMarkup(String, Map)
 * @see MarkupAware#withKeyboard(org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard)
 */
public final class BotMarkupContext {

    /**
     * The key under which this context is stored in
     * {@link uz.osoncode.easygram.core.model.BotRequest} attributes.
     */
    public static final String REQUEST_ATTRIBUTE_KEY = "__botMarkupContext__";

    private static final BotMarkupContext EMPTY = new BotMarkupContext(Collections.emptyMap());

    private final Map<String, Object> params;

    private BotMarkupContext(Map<String, Object> params) {
        this.params = Collections.unmodifiableMap(params);
    }

    /**
     * Creates a {@code BotMarkupContext} from the given parameter map.
     *
     * <p>The provided map is defensively copied so that subsequent mutations of the
     * caller's map do not affect this context. The resulting instance is fully immutable.</p>
     *
     * @param params the parameters; may be {@code null} or empty (returns {@link #EMPTY})
     * @return a new immutable {@code BotMarkupContext}
     */
    public static BotMarkupContext of(Map<String, Object> params) {
        if (Objects.isNull(params) || params.isEmpty()) {
            return EMPTY;
        }
        return new BotMarkupContext(new HashMap<>(params));
    }

    /**
     * Returns an empty {@code BotMarkupContext} with no parameters.
     *
     * @return the shared empty instance
     */
    public static BotMarkupContext empty() {
        return EMPTY;
    }

    /**
     * Returns the parameter value for the given key, or {@code null} if absent.
     *
     * @param key the parameter name; must not be {@code null}
     * @param <T> the expected value type
     * @return the value, or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) params.get(key);
    }

    /**
     * Returns the parameter value for the given key, cast to the provided type.
     *
     * @param key  the parameter name; must not be {@code null}
     * @param type the expected type class; must not be {@code null}
     * @param <T>  the expected value type
     * @return the value cast to {@code type}, or {@code null} if not present
     */
    public <T> T get(String key, Class<T> type) {
        Object value = params.get(key);
        return Objects.isNull(value) ? null : type.cast(value);
    }

    /**
     * Returns the parameter value for the given key, or {@code defaultValue} if absent.
     *
     * @param key          the parameter name; must not be {@code null}
     * @param defaultValue the value to return when the key is absent
     * @param <T>          the expected value type
     * @return the stored value or {@code defaultValue}
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        Object value = params.get(key);
        return Objects.isNull(value) ? defaultValue : (T) value;
    }

    /**
     * Returns {@code true} if a parameter with the given key is present.
     *
     * @param key the parameter name; must not be {@code null}
     * @return {@code true} if the key exists in this context
     */
    public boolean has(String key) {
        return params.containsKey(key);
    }

    /**
     * Returns an unmodifiable view of all parameters in this context.
     *
     * @return the parameter map; never {@code null}
     */
    public Map<String, Object> asMap() {
        return params;
    }
}
