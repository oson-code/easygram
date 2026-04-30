package uz.osoncode.easygram.core.markup;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.Optional;
import java.util.function.Function;

/**
 * Registry that maps markup IDs and chat-state names to their factory functions.
 *
 * <p>Markup factories are registered at startup by the framework when it scans
 * {@link uz.osoncode.easygram.core.annotation.BotConfiguration} beans for methods
 * annotated with {@link uz.osoncode.easygram.core.annotation.BotMarkup}.
 * At request time, a registered factory is invoked with the current
 * {@link BotRequest} to produce the locale-aware (or static) markup.</p>
 *
 * <h2>State-bound keyboards</h2>
 * <p>A {@code @BotMarkup} method that is also annotated with
 * {@link uz.osoncode.easygram.core.chatstate.BotChatState @BotChatState("STATE")}
 * is automatically registered as the default keyboard for that state via
 * {@link #registerForState}. When a handler returns without an explicit keyboard
 * or markup ID, the framework resolves the keyboard for the handler's effective
 * next state via {@link #resolveByState}.</p>
 *
 * <p>The default implementation is {@code InMemoryBotMarkupRegistry} (registered
 * automatically via {@code @ConditionalOnMissingBean}). Replace it with your own
 * {@code @Bean} to support distributed or database-backed registries.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public interface BotMarkupRegistry {

    /**
     * Registers a markup factory under the given ID.
     *
     * <p>The factory receives the current {@link BotRequest} (which may be
     * {@code null} for static/locale-independent markups) and returns the
     * resolved {@link ReplyKeyboard}.</p>
     *
     * @param id      the unique markup identifier; must not be {@code null} or empty
     * @param factory the factory function; must not be {@code null}
     */
    void register(String id, Function<BotRequest, ReplyKeyboard> factory);

    /**
     * Resolves the markup for the given ID as an {@link Optional}, avoiding null checks at call sites.
     *
     * <p>Returns an empty {@link Optional} when the ID is not registered, making it easy to
     * compose with {@code .ifPresent()}, {@code .orElseGet()}, or other {@code Optional} combinators:</p>
     * <pre>{@code
     * registry.resolveOptional("main_menu", request)
     *         .ifPresent(keyboard -> sendMessage.setReplyMarkup(keyboard));
     * }</pre>
     *
     * @param id      the markup ID to look up; must not be {@code null}
     * @param request the current bot request for locale resolution; may be {@code null}
     *                for static markups
     * @return an {@link Optional} containing the resolved keyboard, or empty if not registered
     * @since 0.0.7
     */
    default Optional<ReplyKeyboard> resolveOptional(String id, BotRequest request) {
        return Optional.ofNullable(resolve(id, request));
    }

    /**
     * Resolves the markup for the given ID using the provided request context.
     *
     * @param id      the markup ID to look up; must not be {@code null}
     * @param request the current bot request for locale resolution; may be {@code null}
     *                for static markups
     * @return the resolved {@link ReplyKeyboard}, or {@code null} if the ID is not registered
     */
    ReplyKeyboard resolve(String id, BotRequest request);

    /**
     * Returns {@code true} if a factory is registered under the given ID.
     *
     * @param id the markup ID to check; must not be {@code null}
     * @return {@code true} if the ID is registered
     */
    boolean contains(String id);

    /**
     * Registers a markup factory as the default keyboard for the given chat state.
     *
     * <p>Called by {@code BotMarkupLoader} when a {@code @BotMarkup} method is also
     * annotated with {@code @BotChatState}. Multiple states may share the same factory.</p>
     *
     * <p>The default implementation is a no-op. Override in custom registry implementations
     * to support state-bound keyboards.</p>
     *
     * @param state   the chat state name; must not be {@code null}
     * @param factory the factory function; must not be {@code null}
     */
    default void registerForState(String state, Function<BotRequest, ReplyKeyboard> factory) {
        // no-op by default; InMemoryBotMarkupRegistry overrides this
    }

    /**
     * Resolves the state-bound keyboard for the given chat state, or {@code null} if no
     * keyboard is registered for that state.
     *
     * <p>Called by {@code MarkupApplicationFilter} after all explicit markup options
     * ({@code @BotClearMarkup}, {@code @BotReplyMarkup}, {@code .withMarkup()},
     * {@code .withKeyboard()}) have been exhausted.</p>
     *
     * <p>The default implementation always returns {@code null}.</p>
     *
     * @param state   the chat state name to look up; must not be {@code null}
     * @param request the current bot request; may be {@code null}
     * @return the resolved {@link ReplyKeyboard}, or {@code null} if none is registered
     */
    default ReplyKeyboard resolveByState(String state, BotRequest request) {
        return null;
    }
}
