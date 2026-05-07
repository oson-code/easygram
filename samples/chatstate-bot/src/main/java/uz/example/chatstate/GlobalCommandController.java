package uz.example.chatstate;

import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.bind.annotation.BotClearChatState;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;

/**
 * Global commands available at any point in the conversation.
 *
 * <p>No class-level {@code @BotChatState} means every handler here accepts
 * updates regardless of the current chat state.</p>
 */
@BotController
public class GlobalCommandController {

    private final BotChatStateService chatStateService;

    public GlobalCommandController(BotChatStateService chatStateService) {
        this.chatStateService = chatStateService;
    }

    @BotCommand("/start")
    public String onStart(User user) {
        return """
                👋 Hello, %s! I'm a registration bot.

                Commands:
                  /register — start the registration wizard
                  /status   — show your current registration state
                  /cancel   — cancel the wizard at any step
                """.formatted(user.getFirstName());
    }

    /**
     * Shows the user's current chat state so they can see where they are in the flow.
     * Demonstrates {@link BotChatStateService#getStateAs(Long, Class)}.
     */
    @BotCommand("/status")
    public String onStatus(User user) {
        RegistrationState state = chatStateService.getStateAs(user.getId(), RegistrationState.class);
        if (state == null) {
            return "ℹ️ You have no active registration wizard. Use /register to start one.";
        }
        return switch (state) {
            case AWAITING_NAME -> "📝 Wizard in progress — waiting for your *name* (step 1/3).";
            case AWAITING_AGE  -> "📝 Wizard in progress — waiting for your *age* (step 2/3).";
            case AWAITING_CITY -> "📝 Wizard in progress — waiting for your *city* (step 3/3).";
        };
    }

    /**
     * Cancels the wizard from any step by clearing the state.
     * {@code @BotClearChatState} handles the setState call automatically after return.
     * The service is still used to check the current state for the right reply message.
     */
    @BotCommand("/cancel")
    @BotClearChatState
    public String onCancel(User user) {
        String current = chatStateService.getState(user.getId());
        if (current == null) {
            return "ℹ️ There is no active wizard to cancel.";
        }
        return "❌ Registration wizard cancelled. Use /register to start again.";
    }

    /**
     * Fallback for any update that was not matched by a more specific handler.
     * Guides the user to relevant commands.
     */
    @BotDefaultHandler
    public String onDefault() {
        return "I didn't understand that. Use /register to start, /status to check progress, or /cancel to stop.";
    }
}
