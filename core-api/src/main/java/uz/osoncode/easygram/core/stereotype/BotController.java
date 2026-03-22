package uz.osoncode.easygram.core.stereotype;

import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/**
 * Marks a class as a Telegram bot controller that contains handler methods for incoming updates.
 * Acts as a Spring {@link Component}, making annotated classes eligible for auto-detection
 * and registration in the application context.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface BotController {

    /**
     * The optional Spring bean name for the controller component.
     *
     * @return the bean name, or an empty string to use the default name
     */
    String value() default "";
}
