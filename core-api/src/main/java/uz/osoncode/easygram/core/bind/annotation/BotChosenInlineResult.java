package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to chosen inline results.
 *
 * <p>Annotate a method with {@code @BotChosenInlineResult} to handle any update whose
 * {@code chosen_inline_result} field is populated (i.e., when a user selects a result
 * from an inline query answer).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotChosenInlineResult {
}
