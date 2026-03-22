package uz.example.chatstate;

import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.bind.annotation.BotClearMarkup;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.annotation.BotOrder;
import uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup;
import uz.osoncode.easygram.core.bind.annotation.BotTextValue;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.bind.annotation.BotClearChatState;
import uz.osoncode.easygram.core.bind.annotation.BotForwardChatState;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotReplyButton;
import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;

/**
 * Multi-step registration wizard.
 *
 * <p>The class-level {@code @BotChatState} annotation restricts <em>all</em> handler methods
 * in this controller to run only when the chat is in one of the three registration states.
 * This ensures that free-text replies during registration are captured here rather than
 * falling through to the global default handler.</p>
 *
 * <p>Flow:</p>
 * <pre>
 *   /register  →  ask name  (state: AWAITING_NAME)
 *       ↓
 *   user sends name  →  ask age  (state: AWAITING_AGE)
 *       ↓
 *   user sends age  →  ask city  (state: AWAITING_CITY)
 *       ↓
 *   user sends city  →  show summary, clear state
 * </pre>
 *
 * <p>State transitions use {@link BotForwardChatState} and {@link BotClearChatState}
 * annotations wherever the transition is unconditional. The age step retains a manual
 * {@link BotChatStateService} call because the transition only occurs when input is valid.</p>
 */
@BotController
@BotChatState({"AWAITING_NAME", "AWAITING_AGE", "AWAITING_CITY"})
public class RegistrationFlowController {

    /**
     * Only needed for the conditional state advance in {@link #collectAge}.
     */
    private final BotChatStateService chatStateService;

    public RegistrationFlowController(BotChatStateService chatStateService) {
        this.chatStateService = chatStateService;
    }

    // ── Step 0: entry point ──────────────────────────────────────────────────

    /**
     * Starts the registration wizard from any chat state (including no state).
     * The empty {@code @BotChatState} array overrides the class-level restriction,
     * making this handler reachable at any point in the conversation.
     * {@code @BotForwardChatState} advances to AWAITING_NAME automatically after return.
     */
    @BotCommand("/register")
    @BotChatState   // empty → override class-level guard, accept any state
    @BotForwardChatState("AWAITING_NAME")
    @BotReplyMarkup("kb_cancel")
    public String startRegistration(User user) {
        return "📝 Let's get you registered!\n\nStep 1/3 — What is your full name?";
    }

    // ── Step 1: collect name ─────────────────────────────────────────────────

    /**
     * Receives the user's name when the chat is in the {@code AWAITING_NAME} state.
     * {@code @BotForwardChatState} advances to AWAITING_AGE automatically after return.
     */
    @BotTextDefault
    @BotChatState("AWAITING_NAME")
    @BotForwardChatState("AWAITING_AGE")
    @BotReplyMarkup("kb_cancel")
    public String collectName(@BotTextValue String name) {
        // In a real bot you would persist 'name' to a database or session store here.
        return "✅ Name saved: *" + name + "*\n\nStep 2/3 — How old are you? (enter a number)";
    }

    // ── Step 2: collect age ──────────────────────────────────────────────────

    /**
     * Receives the user's age when the chat is in the {@code AWAITING_AGE} state.
     * The state advance is conditional on valid input, so it is done manually —
     * {@code @BotForwardChatState} cannot be used here because it always fires on return.
     */
    @BotTextDefault
    @BotChatState("AWAITING_AGE")
    @BotReplyMarkup("kb_cancel")
    public String collectAge(@BotTextValue String ageText, User user) {
        try {
            int age = Integer.parseInt(ageText.trim());
            if (age <= 0 || age > 120) {
                return "⚠️ Please enter a valid age (1–120).";
            }
            chatStateService.setState(user.getId(), RegistrationState.AWAITING_CITY);
            return "✅ Age saved: *" + age + "*\n\nStep 3/3 — Which city do you live in?";
        } catch (NumberFormatException e) {
            return "⚠️ That doesn't look like a number. Please enter your age as digits only.";
        }
    }

    // ── Step 3: collect city ─────────────────────────────────────────────────

    /**
     * Receives the user's city when the chat is in the {@code AWAITING_CITY} state.
     * {@code @BotClearChatState} resets the state automatically after return.
     */
    @BotTextDefault
    @BotChatState("AWAITING_CITY")
    @BotClearMarkup
    @BotClearChatState
    public String collectCity(@BotTextValue String city) {
        return """
                🎉 Registration complete!
                
                Here's what we have on file:
                • Name: (saved in step 1)
                • Age:  (saved in step 2)
                • City: %s
                
                Use /status to see your registration state or /register to start over.
                """.formatted(city);
    }

    // ── Global handler for this flow ─────────────────────────────────────────

    /**
     * Handles "❌ Cancel" command during any registration step.
     * Since this class is restricted to registration states, this handler
     * only works when the user is in the middle of registration.
     */
    @BotReplyButton("❌ Cancel")
    @BotClearChatState
    @BotClearMarkup
    public String onCancel() {
        return "🛑 Registration cancelled.";
    }
}

