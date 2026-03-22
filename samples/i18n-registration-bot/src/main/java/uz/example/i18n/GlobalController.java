package uz.example.i18n;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.bind.annotation.BotClearChatState;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.i18n.LocalizedReply;
import uz.osoncode.easygram.core.i18n.LocalizedTemplate;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.Locale;

/**
 * Global commands available at any point in the conversation.
 *
 * <p>No class-level {@code @BotChatState} means every handler here accepts
 * updates regardless of the current chat state.</p>
 *
 * <p>This controller demonstrates several i18n patterns:</p>
 * <ul>
 *   <li>{@link #onStart} — {@link LocalizedTemplate} with a positional argument
 *       ({@code #{0}} substituted with the user's first name)</li>
 *   <li>{@link #onStatus} — {@link LocalizedReply} for simple key lookup</li>
 *   <li>{@link #onCancel} — explicit {@link Locale} injection to show locale info in the reply</li>
 *   <li>{@link #onUnknown} — raw {@link SendMessage} using {@link BotRequest} for chat ID</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@BotController
@RequiredArgsConstructor
public class GlobalController {

    private final BotChatStateService chatStateService;

    // ── /start ───────────────────────────────────────────────────────────────

    /**
     * Sends a localised welcome message using {@link LocalizedTemplate}.
     *
     * <p>The template {@code "${welcome.title} ${welcome.body}\n\n${welcome.commands}"}
     * is resolved against the user's locale. {@code #{0}} is replaced with the user's
     * first name at the template level.</p>
     *
     * @param user the Telegram {@link User} sending the command
     * @return a {@link LocalizedTemplate} with the user's first name as argument
     */
    @BotCommand("/start")
    public LocalizedTemplate onStart(User user) {
        // LocalizedTemplate supports mixed ${key} bundle lookups and #{n} positional args.
        return LocalizedTemplate.of("${welcome.title}\n\n${welcome.body}\n\n${welcome.commands}",
                user.getFirstName());
    }

    // ── /status ───────────────────────────────────────────────────────────────

    /**
     * Shows the user's current wizard progress using a {@link LocalizedReply}.
     *
     * <p>The message key is chosen based on the current chat state. If no state is
     * active the user is informed that no wizard is running.</p>
     *
     * @param user the Telegram {@link User} sending the command
     * @return a {@link LocalizedReply} with the appropriate status message key
     */
    @BotCommand("/status")
    public LocalizedReply onStatus(User user) {
        RegistrationState state = chatStateService.getStateAs(user.getId(), RegistrationState.class);
        if (state == null) {
            return LocalizedReply.of("status.none");
        }
        return switch (state) {
            case AWAITING_NAME  -> LocalizedReply.of("status.awaiting_name");
            case AWAITING_PHONE -> LocalizedReply.of("status.awaiting_phone");
            case AWAITING_CITY  -> LocalizedReply.of("status.awaiting_city");
        };
    }

    // ── /cancel ───────────────────────────────────────────────────────────────

    /**
     * Cancels the active wizard (if any) from any step.
     *
     * <p>This handler also demonstrates injecting the resolved {@link Locale} directly
     * as a method parameter — useful when the locale is needed for conditional logic or
     * logging, not just for the return value.</p>
     *
     * <p>{@code @BotClearChatState} automatically resets the chat state after the method
     * returns, so no explicit {@code chatStateService.setState()} call is needed.</p>
     *
     * @param user   the Telegram user sending the command
     * @param locale the resolved locale (injected by {@code BotLocaleArgumentResolver})
     * @return a {@link LocalizedReply} confirming the cancellation
     */
    @BotCommand("/cancel")
    @BotClearChatState
    public LocalizedReply onCancel(User user, Locale locale) {
        String current = chatStateService.getState(user.getId());
        if (current == null) {
            return LocalizedReply.of("cancel.none");
        }
        // locale is available for logging or conditional logic
        return LocalizedReply.of("cancel.done");
    }

    // ── fallback ─────────────────────────────────────────────────────────────

    /**
     * Catch-all handler for any update that was not matched by a more specific handler.
     *
     * <p>Uses a raw {@link SendMessage} to demonstrate that non-i18n return types
     * can still be used alongside localised ones. The error text is kept hardcoded here
     * for brevity; in production, use {@link uz.osoncode.easygram.core.i18n.BotMessageSource}
     * or return a {@link LocalizedReply} instead.</p>
     *
     * @param request the current bot request
     * @return a {@link LocalizedReply} with the generic error message
     */
    @BotDefaultHandler
    public LocalizedReply onUnknown(BotRequest request) {
        return LocalizedReply.of("error.unknown");
    }
}
