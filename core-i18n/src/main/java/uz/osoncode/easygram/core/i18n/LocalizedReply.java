package uz.osoncode.easygram.core.i18n;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.markup.MarkupAware;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object representing a localised reply where the content is a simple
 * message bundle key (without {@code ${}} syntax) and arguments.
 *
 * <p>Unlike {@link LocalizedTemplate}, this class does not support mixed content or
 * template tokens. The {@code key} is resolved directly against the message bundle,
 * and {@code args} are used for formatting that specific message.</p>
 *
 * <p>Keyboard markup can be attached in three ways, in order of precedence:</p>
 * <ol>
 *   <li>{@link #withKeyboard(ReplyKeyboard)} — attach a directly-built keyboard inline.</li>
 *   <li>{@link #withMarkup(String, Map)} — resolve a registered keyboard by ID and pass
 *       runtime parameters to the {@code @BotMarkup} factory method.</li>
 *   <li>{@link #withMarkup(String)} — resolve a registered keyboard by ID (no params).</li>
 *   <li>{@link #removeMarkup()} — send {@code ReplyKeyboardRemove} to clear the keyboard.</li>
 * </ol>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public final class LocalizedReply implements MarkupAware {

    private final String key;
    private final Object[] args;
    private final String markupId;
    private final Map<String, Object> markupParams;
    private final ReplyKeyboard keyboard;
    private final boolean removeMarkup;
    private final boolean editMessage;

    private LocalizedReply(String key, Object[] args, String markupId,
                            Map<String, Object> markupParams, ReplyKeyboard keyboard,
                            boolean removeMarkup, boolean editMessage) {
        this.key = key;
        this.args = args;
        this.markupId = markupId;
        this.markupParams = markupParams;
        this.keyboard = keyboard;
        this.removeMarkup = removeMarkup;
        this.editMessage = editMessage;
    }

    /**
     * Creates a new {@link Builder} for {@code LocalizedReply}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LocalizedReply}.
     *
     * <pre>{@code
     * LocalizedReply reply = LocalizedReply.builder()
     *         .key("welcome.message")
     *         .args(user.getFirstName())
     *         .markupId("main_menu")
     *         .build();
     * }</pre>
     */
    public static final class Builder {

        private String key;
        private Object[] args;
        private String markupId;
        private Map<String, Object> markupParams;
        private ReplyKeyboard keyboard;
        private boolean removeMarkup;
        private boolean editMessage;

        private Builder() {}

        /**
         * Sets the message bundle key.
         *
         * @param key the i18n message key; must not be {@code null}
         * @return this builder
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the optional arguments for the message format.
         *
         * @param args the format arguments
         * @return this builder
         */
        public Builder args(Object... args) {
            this.args = args;
            return this;
        }

        /**
         * Sets the pre-registered markup ID.
         *
         * @param markupId the ID of a registered markup
         * @return this builder
         */
        public Builder markupId(String markupId) {
            this.markupId = markupId;
            return this;
        }

        /**
         * Sets the markup factory parameters forwarded to the {@code @BotMarkup} factory.
         *
         * @param markupParams the parameters map
         * @return this builder
         */
        public Builder markupParams(Map<String, Object> markupParams) {
            this.markupParams = markupParams;
            return this;
        }

        /**
         * Sets a directly-built keyboard (takes precedence over {@link #markupId}).
         *
         * @param keyboard the keyboard to attach
         * @return this builder
         */
        public Builder keyboard(ReplyKeyboard keyboard) {
            this.keyboard = keyboard;
            return this;
        }

        /**
         * Instructs the framework to send a {@code ReplyKeyboardRemove}.
         *
         * @return this builder
         */
        public Builder removeMarkup() {
            this.removeMarkup = true;
            return this;
        }

        /**
         * Instructs the framework to edit the original message (via {@code EditMessageText})
         * when this reply is returned from a callback query handler.
         *
         * @param editMessage {@code true} to edit; {@code false} to send a new message (default)
         * @return this builder
         * @since 0.0.2
         */
        public Builder editMessage(boolean editMessage) {
            this.editMessage = editMessage;
            return this;
        }

        /**
         * Builds and returns the immutable {@link LocalizedReply}.
         *
         * @return a new {@code LocalizedReply} instance
         */
        public LocalizedReply build() {
            Objects.requireNonNull(key, "key must not be null");
            return new LocalizedReply(key, args, markupId, markupParams, keyboard, removeMarkup, editMessage);
        }
    }

    /**
     * Creates a {@code LocalizedReply} with the given message key and arguments.
     *
     * @param key  the message bundle key (e.g. "welcome.message"); must not be {@code null}
     * @param args optional arguments for the message format
     * @return a new {@code LocalizedReply} instance
     */
    public static LocalizedReply of(String key, Object... args) {
        return new LocalizedReply(key, args, null, null, null, false, false);
    }

    @Override
    public LocalizedReply withMarkup(String markupId) {
        return new LocalizedReply(this.key, this.args, markupId, null, null, false, this.editMessage);
    }

    /**
     * Returns a new {@code LocalizedReply} with the given markup ID and factory parameters.
     *
     * @param markupId the ID of a pre-registered markup; must not be {@code null}
     * @param params   the parameters to pass to the factory; must not be {@code null}
     * @return a new {@code LocalizedReply} with the markup ID and params set
     */
    @Override
    public LocalizedReply withMarkup(String markupId, Map<String, Object> params) {
        return new LocalizedReply(this.key, this.args, markupId, params, null, false, this.editMessage);
    }

    /**
     * Returns a new {@code LocalizedReply} with the given {@link ReplyKeyboard} attached directly.
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new {@code LocalizedReply} with the keyboard set
     */
    @Override
    public LocalizedReply withKeyboard(ReplyKeyboard keyboard) {
        return new LocalizedReply(this.key, this.args, null, null, keyboard, false, this.editMessage);
    }

    @Override
    public LocalizedReply removeMarkup() {
        return new LocalizedReply(this.key, this.args, null, null, null, true, this.editMessage);
    }

    /**
     * Returns a new {@code LocalizedReply} with the {@code editMessage} flag set to {@code true}.
     *
     * <p>When this flag is set and the request originates from a callback query, the framework
     * will edit the originating message instead of sending a new one.</p>
     *
     * @return a new {@code LocalizedReply} with {@code editMessage = true}
     * @since 0.0.2
     */
    public LocalizedReply withEditMessage() {
        return new LocalizedReply(this.key, this.args, this.markupId, this.markupParams, this.keyboard, this.removeMarkup, true);
    }

    public String getKey() {
        return key;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public String getMarkupId() {
        return markupId;
    }

    @Override
    public Map<String, Object> getMarkupParams() {
        return markupParams;
    }

    @Override
    public ReplyKeyboard getKeyboard() {
        return keyboard;
    }

    @Override
    public boolean isRemoveMarkup() {
        return removeMarkup;
    }

    /**
     * Returns {@code true} if the framework should edit the original callback-query message
     * instead of sending a new message.
     *
     * @return {@code true} to edit the originating message
     * @since 0.0.2
     */
    @Override
    public boolean isEditMessage() {
        return editMessage;
    }
}

