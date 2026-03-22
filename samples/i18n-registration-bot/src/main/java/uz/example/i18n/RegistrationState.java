package uz.example.i18n;

/**
 * Enum representing the stages of the user registration wizard.
 *
 * <p>Using an enum (instead of raw strings) provides compile-time safety and IDE
 * autocompletion. {@link uz.osoncode.easygram.core.chatstate.BotChatStateService}
 * has built-in {@code setState(Long, Enum)} and {@code getStateAs(Long, Class)} overloads
 * that work directly with enum constants.</p>
 *
 * <p>Flow:</p>
 * <pre>
 *   /register
 *       ↓
 *   AWAITING_NAME   → user sends full name
 *       ↓
 *   AWAITING_PHONE  → user sends phone number (validated by {@code @BotTextPattern})
 *       ↓
 *   AWAITING_CITY   → user sends city name
 *       ↓
 *   (state cleared, summary shown)
 * </pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public enum RegistrationState {

    /** Bot has asked for the user's full name; waiting for a text reply. */
    AWAITING_NAME,

    /** Bot has asked for the user's phone number; waiting for a valid phone reply. */
    AWAITING_PHONE,

    /** Bot has asked for the user's city; waiting for a text reply. */
    AWAITING_CITY
}
