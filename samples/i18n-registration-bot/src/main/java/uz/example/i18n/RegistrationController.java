package uz.example.i18n;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.annotation.BotOrder;
import uz.osoncode.easygram.core.bind.annotation.*;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.i18n.LocalizedReply;
import uz.osoncode.easygram.core.i18n.LocalizedTemplate;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.List;

/**
 * Multi-step registration wizard controller.
 *
 * <p>The class-level {@link BotChatState} restricts <em>all</em> handler methods in this
 * controller to run only when the chat is in one of the three registration states. This
 * ensures that free-text replies during registration are captured here rather than falling
 * through to the global default handler in {@link GlobalController}.</p>
 *
 * <h3>Registration flow</h3>
 * <pre>
 *   /register
 *       │
 *       ▼  (state: AWAITING_NAME)
 *   collectName ── @BotTextDefault ──────────────────────────────►  AWAITING_PHONE
 *       │
 *       ▼  (state: AWAITING_PHONE)
 *   collectPhone ── @BotTextPattern("^\\+\\d{7,15}$") ──────────►  AWAITING_CITY
 *   invalidPhone ── @BotTextDefault (fallback) ──────────────────►  stays AWAITING_PHONE
 *       │
 *       ▼  (state: AWAITING_CITY)
 *   collectCity ── @BotTextDefault ─────────────────────────────►  (cleared)
 * </pre>
 *
 * <h3>i18n patterns demonstrated</h3>
 * <ul>
 *   <li>{@link LocalizedReply} — single message-bundle key (used for every step prompt)</li>
 *   <li>{@link LocalizedTemplate} — mixed template with a positional arg (registration summary)</li>
 *   <li>State-bound keyboards from {@link uz.example.i18n.markup.RegistrationMarkups} —
 *       attached automatically based on the effective next state; no
 *       {@code @BotReplyMarkup} annotation needed on the handler methods.</li>
 * </ul>
 *
 * <h3>{@code @BotTextPattern} for phone routing</h3>
 * <p>Instead of validating the phone number with if/else inside a single handler,
 * two separate handlers cover the two outcomes:</p>
 * <ol>
 *   <li>{@link #collectPhone} — matched only when the text is a valid E.164-ish number
 *       (e.g. {@code +998901234567}). Lower {@code @BotOrder} makes it win over the fallback.</li>
 *   <li>{@link #invalidPhone} — matched for <em>all</em> other text via {@code @BotTextDefault},
 *       and sends back a localised error asking the user to retry.</li>
 * </ol>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@BotController
@BotChatState({"AWAITING_NAME", "AWAITING_PHONE", "AWAITING_CITY"})
public class RegistrationController {

    // ── Step 0: entry point ──────────────────────────────────────────────────

    /**
     * Starts the registration wizard from any chat state.
     *
     * <p>The empty {@code @BotChatState} on this method overrides the class-level guard,
     * making it reachable regardless of the current state — even when no state is set.
     * {@code @BotForwardChatState} advances the state to {@code AWAITING_NAME} after return.</p>
     *
     * @param user the Telegram user who issued the command
     * @return a {@link LocalizedReply} asking for the user's full name
     */
    @BotCommand("/register")
    @BotChatState            // empty → override class guard, accept any state
    @BotForwardChatState("AWAITING_NAME")
    public LocalizedReply startRegistration(User user) {
        return LocalizedReply.of("register.start");
    }

    // ── Step 1: collect name ─────────────────────────────────────────────────

    /**
     * Receives the user's full name when the chat is in the {@code AWAITING_NAME} state.
     *
     * <p>{@code @BotForwardChatState} advances the state to {@code AWAITING_PHONE}
     * automatically after the method returns.</p>
     *
     * @param name the text the user sent (injected via {@code @BotTextValue})
     * @return a {@link LocalizedReply} confirming the name and asking for the phone number;
     *         the name is passed as a format argument ({@code {0}}) for the message bundle
     */
    @BotTextDefault
    @BotChatState("AWAITING_NAME")
    @BotForwardChatState("AWAITING_PHONE")
    public LocalizedReply collectName(@BotTextValue @NotBlank @Size(min = 2, max = 50, message = "name.limit") String name) {
        return LocalizedReply.of("register.name.saved", name);
    }

    // ── Step 2a: collect phone — VALID input ─────────────────────────────────

    /**
     * Receives a valid phone number when the chat is in the {@code AWAITING_PHONE} state.
     *
     * <p>The handler is triggered <em>only</em> when the incoming text matches the
     * {@code @BotTextPattern} regex {@code ^\+\d{7,15}$} (an international phone number).
     * {@code @BotOrder(1)} gives this handler higher priority than the
     * {@link #invalidPhone} fallback at the same state.</p>
     *
     * <p>{@code @BotForwardChatState} advances the state to {@code AWAITING_CITY}
     * automatically after the method returns.</p>
     *
     * @param contact contact object
     * @return a {@link LocalizedReply} confirming the phone and asking for the city
     */
    @BotContact
    @BotChatState("AWAITING_PHONE")
    @BotForwardChatState("AWAITING_CITY")
    public LocalizedReply collectPhone(Contact contact) {
        return LocalizedReply.of("register.phone.saved", contact.getPhoneNumber());
    }

    /**
     * Receives a valid phone number when the chat is in the {@code AWAITING_PHONE} state.
     *
     * <p>The handler is triggered <em>only</em> when the incoming text matches the
     * {@code @BotTextPattern} regex {@code ^\+\d{7,15}$} (an international phone number).
     * {@code @BotOrder(1)} gives this handler higher priority than the
     * {@link #invalidPhone} fallback at the same state.</p>
     *
     * <p>{@code @BotForwardChatState} advances the state to {@code AWAITING_CITY}
     * automatically after the method returns.</p>
     *
     * @param phone text param
     * @return a {@link LocalizedReply} confirming the phone and asking for the city
     */
    @BotTextPattern("^\\+\\d{7,15}$")
    @BotChatState("AWAITING_PHONE")
    @BotForwardChatState("AWAITING_CITY")
    public LocalizedReply collectPhone(@BotTextValue String phone) {
        return LocalizedReply.of("register.phone.saved", phone);
    }

    // ── Step 2b: collect phone — INVALID input ────────────────────────────────

    /**
     * Handles invalid phone-number input when the chat is in the {@code AWAITING_PHONE} state.
     *
     * <p>This handler catches any text that did <em>not</em> match the {@code @BotTextPattern}
     * in {@link #collectPhone}. It stays in the {@code AWAITING_PHONE} state (no
     * {@code @BotForwardChatState}) and returns a localised error message so the user can retry.</p>
     *
     * @return a {@link LocalizedReply} asking the user to re-enter a valid phone number
     */
    @BotTextDefault
    @BotChatState("AWAITING_PHONE")
    public LocalizedReply invalidPhone() {
        return LocalizedReply.of("register.phone.invalid");
    }

    // ── Step 3: collect city ─────────────────────────────────────────────────

    /**
     * Receives the user's city when the chat is in the {@code AWAITING_CITY} state.
     *
     * <p>{@code @BotClearChatState} resets the chat state automatically after the method
     * returns. {@code @BotClearMarkup} removes the cancel keyboard from the UI.</p>
     *
     * <p>The registration summary uses {@link LocalizedTemplate} to demonstrate mixed
     * {@code ${key}} bundle lookups and {@code #{n}} positional arguments in a single
     * template string. The city is passed as argument {@code #{0}}.</p>
     *
     * @param city the city the user sent
     * @return a {@link LocalizedTemplate} with the registration complete message
     */
    @BotTextDefault
    @BotChatState("AWAITING_CITY")
    @BotClearChatState
    @BotClearMarkup
    public LocalizedTemplate collectCity(@BotTextValue @NotBlank @Size(min = 2, max = 50) String city) {
        // LocalizedTemplate: ${key} → message bundle, #{0} → positional arg (city).
        // In a real application, name and phone would be loaded from a database or session.
        return LocalizedTemplate.of("${register.complete}", "(saved)", "(saved)", city);
    }

    // ── Validation error handler ─────────────────────────────────────────────

    /**
     * Catches {@link ConstraintViolationException} thrown by the framework when a handler
     * parameter fails Jakarta Bean Validation constraints (e.g. {@code @NotBlank},
     * {@code @Size}).
     *
     * <p>The framework's {@code MethodInvocationFilter} calls
     * {@code ExecutableValidator.validateParameters()} after argument resolution and throws
     * a {@link ConstraintViolationException} <em>before</em> the handler method runs.
     * This method is invoked automatically by the exception-handler dispatch mechanism.</p>
     *
     * @param ex the constraint violation exception containing all failing constraints
     * @return a localised error message listing each violation
     */
    @BotExceptionHandler(ConstraintViolationException.class)
    public List<LocalizedReply> onValidationError(ConstraintViolationException ex) {
        return ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .map(LocalizedReply::of)
                .toList();
    }

    // ── Cancel button ────────────────────────────────────────────────────────

    /**
     * Handles the localised "Cancel" reply button during any registration step.
     *
     * <p>With {@code core-i18n} on the classpath, {@code @BotReplyButton} values are treated
     * as message-bundle keys. The framework resolves {@code "btn.cancel"} in the user's locale
     * (e.g. "❌ Cancel" for English, "❌ Bekor qilish" for Uzbek, "❌ Отмена" for Russian)
     * and matches it against the incoming text. This means a single annotation covers all
     * supported languages automatically.</p>
     *
     * <p>{@code @BotClearChatState} and {@code @BotClearMarkup} clean up wizard state
     * and remove the keyboard automatically after return.</p>
     *
     * @return a {@link LocalizedReply} confirming the cancellation
     */
    @BotReplyButton("btn.cancel")
    @BotClearChatState
    @BotClearMarkup
    @BotOrder(0)
    public LocalizedReply onCancelButton() {
        return LocalizedReply.of("register.cancelled");
    }
}
