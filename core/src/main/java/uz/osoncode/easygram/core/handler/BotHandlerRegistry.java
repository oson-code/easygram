package uz.osoncode.easygram.core.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry that stores and maintains sorted lists of {@link BotHandler} instances.
 *
 * <p>Handlers are divided into three groups evaluated in the following order:</p>
 * <ol>
 *   <li><em>State handlers</em> — registered via {@link #registerState} and evaluated first;
 *       each requires a specific chat state (non-empty {@code @BotChatState}).</li>
 *   <li><em>Specific handlers</em> — registered via {@link #register} and evaluated second;
 *       each targets a concrete update condition such as a command or callback pattern,
 *       without a chat-state restriction.</li>
 *   <li><em>Default handlers</em> — registered via {@link #registerDefault} and used as
 *       fallbacks when no state or specific handler matches the incoming update.</li>
 * </ol>
 *
 * <p>After every insertion all lists are re-sorted using the natural ordering defined by
 * {@link BotHandler#compareTo} so that higher-priority handlers are always evaluated first.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class BotHandlerRegistry {

    /** State-specific handlers sorted by priority, evaluated before all other handlers. */
    private final List<BotHandler> stateHandlers = new CopyOnWriteArrayList<>();

    /** Specific handlers sorted by priority, evaluated after state handlers. */
    private final List<BotHandler> botHandlers = new CopyOnWriteArrayList<>();

    /** Fallback handlers sorted by priority, evaluated when no state or specific handler matches. */
    private final List<BotHandler> defaultHandlers = new CopyOnWriteArrayList<>();

    /**
     * Returns an unmodifiable view of the state-specific handler list.
     *
     * @return an unmodifiable list of state handlers
     */
    public List<BotHandler> getStateHandlers() {
        return Collections.unmodifiableList(stateHandlers);
    }

    /**
     * Returns an unmodifiable view of the specific handler list.
     *
     * @return an unmodifiable list of specific handlers
     */
    public List<BotHandler> getBotHandlers() {
        return Collections.unmodifiableList(botHandlers);
    }

    /**
     * Returns an unmodifiable view of the default (fallback) handler list.
     *
     * @return an unmodifiable list of default handlers
     */
    public List<BotHandler> getDefaultHandlers() {
        return Collections.unmodifiableList(defaultHandlers);
    }

    /**
     * Registers a state-specific {@link BotHandler} and re-sorts the state-handler list
     * by natural priority order.
     *
     * @param botHandler the state-specific handler to register; must not be {@code null}
     */
    public void registerState(BotHandler botHandler) {
        log.debug("Registering state handler: {}", botHandler.info());
        register(botHandler, stateHandlers);
    }

    /**
     * Registers a specific {@link BotHandler} and re-sorts the specific-handler list
     * by natural priority order.
     *
     * @param botHandler the specific handler to register; must not be {@code null}
     */
    public void register(BotHandler botHandler) {
        log.debug("Registering specific handler: {}", botHandler.info());
        register(botHandler, botHandlers);
    }

    /**
     * Registers a default (fallback) {@link BotHandler} and re-sorts the default-handler
     * list by natural priority order.
     *
     * @param botHandler the fallback handler to register; must not be {@code null}
     */
    public void registerDefault(BotHandler botHandler) {
        log.debug("Registering default handler: {}", botHandler.info());
        register(botHandler, defaultHandlers);
    }

    /**
     * Adds the given handler to the target list and re-sorts the list using
     * {@link BotHandler#compareTo} to maintain priority ordering.
     *
     * @param botHandler  the handler to add; must not be {@code null}
     * @param handlerList the list into which the handler should be inserted
     */
    private void register(BotHandler botHandler, List<BotHandler> handlerList) {
        handlerList.add(botHandler);
        handlerList.sort(BotHandler::compareTo);
    }
}
