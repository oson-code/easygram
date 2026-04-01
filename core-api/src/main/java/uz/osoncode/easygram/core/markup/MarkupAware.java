package uz.osoncode.easygram.core.markup;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.Map;

/**
 * Marker interface for return value objects that support carrying a markup ID,
 * a directly-built {@link ReplyKeyboard}, and optional parameters for the
 * {@link BotMarkupRegistry} factory method.
 *
 * <p>Implemented by {@link uz.osoncode.easygram.core.reply.PlainReply},
 * {@link uz.osoncode.easygram.core.reply.PlainTextTemplate}, and
 * {@code LocalizedReply} (from {@code core-i18n}). This interface enables
 * {@code BotMethodHandler} to apply markup to any supported return type without
 * introducing module-level circular dependencies.</p>
 *
 * <h2>Markup resolution precedence</h2>
 * <ol>
 *   <li>{@code isRemoveMarkup()} — sends {@code ReplyKeyboardRemove}; overrides everything
 *       when {@code @BotClearMarkup} is on the handler method.</li>
 *   <li>{@code getKeyboard()} non-null — keyboard is attached directly, no registry lookup.</li>
 *   <li>{@code getMarkupId()} non-null — registry lookup, optionally with
 *       {@link #getMarkupParams()} forwarded via {@link BotMarkupContext}.</li>
 *   <li>{@code @BotReplyMarkup} annotation — fallback; only applied when neither
 *       a keyboard nor a markup ID is already set on the return value.</li>
 * </ol>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface MarkupAware {

    /**
     * Returns the markup ID set on this reply, or {@code null} if none was set.
     *
     * @return the markup ID; may be {@code null}
     */
    String getMarkupId();

    /**
     * Returns the parameters to pass to the {@link BotMarkupRegistry} factory when
     * resolving {@link #getMarkupId()}, or {@code null} if no params were provided.
     *
     * <p>When non-null, the framework wraps these in a {@link BotMarkupContext} and
     * stores it on the {@link uz.osoncode.easygram.core.model.BotRequest} under
     * {@link BotMarkupContext#REQUEST_ATTRIBUTE_KEY} before the factory is invoked.</p>
     *
     * @return the markup params map; may be {@code null}
     */
    default Map<String, Object> getMarkupParams() {
        return null;
    }

    /**
     * Returns a directly-set {@link ReplyKeyboard} that should be attached to the outgoing
     * message without a registry lookup, or {@code null} if none was set.
     *
     * <p>When non-null this takes precedence over {@link #getMarkupId()}.</p>
     *
     * @return the inline keyboard instance; may be {@code null}
     */
    default ReplyKeyboard getKeyboard() {
        return null;
    }

    /**
     * Returns a copy of this reply with the given markup ID set.
     *
     * @param markupId the markup ID to set; must not be {@code null}
     * @return a new instance of the same type with the markup ID set
     */
    MarkupAware withMarkup(String markupId);

    /**
     * Returns a copy of this reply with the given markup ID and factory parameters set.
     *
     * <p>The {@code params} map will be forwarded to the {@code @BotMarkup} factory
     * method via {@link BotMarkupContext} so the factory can build a dynamic keyboard.</p>
     *
     * @param markupId the markup ID to set; must not be {@code null}
     * @param params   the parameters to forward to the factory; must not be {@code null}
     * @return a new instance with the markup ID and params set
     */
    MarkupAware withMarkup(String markupId, Map<String, Object> params);

    /**
     * Returns a copy of this reply with the given {@link ReplyKeyboard} attached directly.
     *
     * <p>The keyboard is used as-is; no registry lookup is performed.</p>
     *
     * @param keyboard the keyboard to attach; must not be {@code null}
     * @return a new instance with the keyboard set
     */
    MarkupAware withKeyboard(ReplyKeyboard keyboard);

    /**
     * Returns a copy of this reply with the instruction to remove the current markup.
     * This is equivalent to sending a {@link org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove}.
     *
     * @return a new instance with the remove-markup flag set
     */
    MarkupAware removeMarkup();

    /**
     * Returns {@code true} if the handler should edit the original message (via
     * {@code EditMessageText}) rather than send a new message, when the current request
     * originates from a callback query.
     *
     * <p>Defaults to {@code false}. Implementations override this when an
     * {@code editMessage} flag is set on the return value.</p>
     *
     * @return {@code true} to edit the originating callback-query message
     * @since 0.0.2
     */
    default boolean isEditMessage() {
        return false;
    }

    /**
     * Returns {@code true} if this reply instructs to remove the current markup.
     *
     * @return {@code true} if markup should be removed
     */
    boolean isRemoveMarkup();
}

