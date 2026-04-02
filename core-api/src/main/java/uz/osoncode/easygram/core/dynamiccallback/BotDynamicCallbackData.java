package uz.osoncode.easygram.core.dynamiccallback;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable container for dynamic callback query payload data.
 *
 * <p>Stores a <em>type</em> discriminator and an arbitrary key-value data map.
 * Instances are stored server-side via {@link BotDynamicCallbackQueryService} and
 * looked up by the raw Telegram callback data string (typically a UUID key).</p>
 *
 * <h2>Building an instance</h2>
 * <pre>{@code
 * BotDynamicCallbackData payload = BotDynamicCallbackData.builder()
 *     .type("product_buy")
 *     .put("id", 42L)
 *     .put("currency", "USD")
 *     .build();
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @see BotDynamicCallbackQueryService
 * @since 0.0.4
 */
public record BotDynamicCallbackData(
        /**
         * Discriminator field used for routing via {@link uz.osoncode.easygram.core.bind.annotation.BotDynamicCallbackQuery}.
         */
        String type,
        /**
         * Structured payload data associated with this callback entry.
         */
        Map<String, Object> data
) {

    /**
     * Key used to store the resolved {@code BotDynamicCallbackData} in
     * {@link uz.osoncode.easygram.core.model.BotRequest#setAttribute} so that the argument
     * resolver can retrieve it without a second service lookup.
     */
    public static final String ATTRIBUTE_KEY = "DYNAMIC_CALLBACK_DATA";


    private BotDynamicCallbackData(Builder builder) {
        this(builder.type, builder.data);
    }

    public BotDynamicCallbackData(String type, Map<String, Object> data) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.data = Collections.unmodifiableMap(new HashMap<>(data));
    }

    /**
     * Returns the type discriminator for this payload.
     *
     * @return the type string; never {@code null}
     */
    public String getType() {
        return type;
    }

    /**
     * Returns an unmodifiable view of the structured data map.
     *
     * @return the data map; never {@code null}, may be empty
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Creates a new {@link Builder} instance.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BotDynamicCallbackData}.
     *
     * @since 0.0.4
     */
    public static final class Builder {

        private String type;
        private final Map<String, Object> data = new HashMap<>();

        private Builder() {
        }

        /**
         * Sets the type discriminator.
         *
         * @param type the type string; must not be {@code null}
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Replaces the entire data map with the given map.
         *
         * @param data the data map; must not be {@code null}
         * @return this builder
         */
        public Builder data(Map<String, Object> data) {
            Objects.requireNonNull(data, "data must not be null");
            this.data.clear();
            this.data.putAll(data);
            return this;
        }

        /**
         * Adds a single key-value pair to the data map.
         *
         * @param key   the entry key; must not be {@code null}
         * @param value the entry value
         * @return this builder
         */
        public Builder put(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        /**
         * Builds the immutable {@link BotDynamicCallbackData} instance.
         *
         * @return the constructed instance
         * @throws NullPointerException if {@code type} has not been set
         */
        public BotDynamicCallbackData build() {
            return new BotDynamicCallbackData(this);
        }
    }
}
