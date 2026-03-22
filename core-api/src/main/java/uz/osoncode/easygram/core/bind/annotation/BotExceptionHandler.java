package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Designates a method as a bot exception handler within a {@link uz.osoncode.easygram.core.stereotype.BotController}.
 * The annotated method is invoked when an exception of one of the specified types is thrown
 * during the processing of a Telegram update.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotExceptionHandler {

    /**
     * The exception types handled by the annotated method.
     *
     * @return an array of {@link Throwable} subclasses that trigger this handler
     */
    Class<? extends Throwable>[] value();
}
