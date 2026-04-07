package uz.osoncode.easygram.core.handler;

import lombok.extern.slf4j.Slf4j;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.Objects;
import java.util.Set;

/**
 * {@link BotHandlerCondition} that guards a handler method behind a required chat state.
 *
 * <p>When a handler method (or its controller class) is annotated with
 * {@link BotChatState} carrying a non-empty state set, this condition is created and
 * added to the handler's condition list. At runtime it:</p>
 * <ol>
 *   <li>Returns {@code true} immediately if no {@link BotChatStateService} is available
 *       in the application context (state checking is disabled)</li>
 *   <li>Returns {@code true} immediately if the effective {@link BotChatState} has an
 *       empty value array (i.e. matches any state)</li>
 *   <li>Returns {@code false} if the request has no associated chat</li>
 *   <li>Returns {@code false} if the current chat state is {@code null} (no state set)</li>
 *   <li>Returns {@code true} only if the current state is contained in the required set</li>
 * </ol>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerCondition
 * @see BotChatState
 */
@Slf4j
public class BotChatStateCondition implements BotHandlerCondition {

    private final BotChatStateService botChatStateService;
    private final BotChatState effectiveChatState;

    /**
     * Creates a new chat-state condition.
     *
     * @param botChatStateService the chat-state service, or {@code null} if unavailable
     * @param effectiveChatState  the effective {@link BotChatState} (method or class level),
     *                            or {@code null} if neither is present
     */
    public BotChatStateCondition(BotChatStateService botChatStateService, BotChatState effectiveChatState) {
        this.botChatStateService = botChatStateService;
        this.effectiveChatState = effectiveChatState;
    }

    /**
     * Returns {@code true} if the current request satisfies the chat-state requirement.
     *
     * @param botRequest the current bot request; must not be {@code null}
     * @return {@code true} if the handler should be considered for this update
     */
    @Override
    public boolean matches(BotRequest botRequest) {
        if (Objects.isNull(effectiveChatState) || Objects.isNull(botChatStateService)) {
            return true;
        }
        Set<String> states = Set.of(effectiveChatState.value());
        if (states.isEmpty()) {
            return true;
        }
        if (Objects.isNull(botRequest.getChat())) {
            log.trace("State check skipped: no chat in request");
            return false;
        }
        String currentState = botChatStateService.getState(botRequest.getChat().getId());
        if (Objects.isNull(currentState)) {
            log.trace("State check failed: no current state for chatId={}", botRequest.getChat().getId());
            return false;
        }
        boolean matches = states.contains(currentState);
        if (!matches) {
            log.trace("State check failed: chatId={} currentState={} requiredStates={}",
                    botRequest.getChat().getId(), currentState, states);
        }
        return matches;
    }
}
