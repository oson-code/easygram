package uz.osoncode.easygram.core.annotation;
import uz.osoncode.easygram.core.stereotype.BotController;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Marks a class as a Telegram bot markup configuration that declares
 * {@link BotMarkup}-annotated factory methods.
 *
 * <p>Acts as a Spring {@link Component}, making annotated classes eligible for
 * auto-detection and registration in the application context.
 * Use it to centralise all markup (keyboard) factory definitions outside of
 * {@link BotController} handlers:</p>
 *
 * <pre>{@code
 * @BotConfiguration
 * @RequiredArgsConstructor
 * public class MyMarkups {
 *
 *     private final BotKeyboardFactory keyboards;
 *
 *     @BotMarkup("main_menu")
 *     public InlineKeyboardMarkup mainMenu(BotRequest request) {
 *         return keyboards.inline(request)
 *             .row("btn.profile", "cb_profile", "btn.settings", "cb_settings")
 *             .build();
 *     }
 *
 *     @BotMarkup("static_kb")   // no BotRequest = locale-independent
 *     public ReplyKeyboardMarkup staticKb() {
 *         return ReplyKeyboardMarkup.builder()...build();
 *     }
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface BotConfiguration {

    /**
     * The optional Spring bean name for the configuration component.
     *
     * @return the bean name, or an empty string to use the default name
     */
    String value() default "";
}
