package uz.example.chatstate;

/**
 * Enum representing the stages of the user registration wizard.
 *
 * <p>Using an enum (instead of raw strings) provides compile-time safety and IDE
 * autocompletion. {@link uz.osoncode.easygram.core.chatstate.BotChatStateService}
 * has built-in {@code setState(Long, Enum)} and {@code getStateAs(Long, Class)} overloads
 * that work directly with enum constants.</p>
 */
public enum RegistrationState {

    /** Bot has asked for the user's full name; waiting for a text reply. */
    AWAITING_NAME,

    /** Bot has asked for the user's age; waiting for a numeric text reply. */
    AWAITING_AGE,

    /** Bot has asked for the user's city; waiting for a text reply. */
    AWAITING_CITY
}
