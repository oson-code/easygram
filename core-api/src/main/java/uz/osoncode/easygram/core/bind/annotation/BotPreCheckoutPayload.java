package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Injects the pre-checkout query invoice payload string into a handler method parameter.
 *
 * <p>Apply this annotation to a {@link String} parameter to receive the invoice payload
 * from the current pre-checkout query directly, without needing to unwrap the full
 * {@code PreCheckoutQuery} object.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotPreCheckoutPayload {
}
