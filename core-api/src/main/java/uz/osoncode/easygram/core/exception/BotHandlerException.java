package uz.osoncode.easygram.core.exception;

/**
 * Unchecked exception thrown when a bot handler method cannot be invoked via reflection.
 * Wraps the underlying cause so that the original failure details are preserved while
 * allowing the exception to propagate through the handler dispatch chain without checked
 * exception declarations.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotHandlerException extends RuntimeException {

    /**
     * Constructs a new {@code BotHandlerException} with a descriptive message and the root cause.
     *
     * @param message a human-readable description of the invocation failure
     * @param cause   the underlying exception that prevented handler method execution
     */
    public BotHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
