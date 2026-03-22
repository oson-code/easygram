package uz.osoncode.easygram.core.bind.annotation;
import uz.osoncode.easygram.core.annotation.BotMarkup;
import uz.osoncode.easygram.core.annotation.BotConfiguration;

import java.lang.annotation.*;

/**
 * Decorates a handler method to automatically attach a pre-registered
 * {@link org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard}
 * to the outgoing message.
 *
 * <p>The markup is looked up by ID from the
 * {@link uz.osoncode.easygram.core.markup.BotMarkupRegistry}; the factory that produced
 * it was registered via {@link BotMarkup} in a {@link BotConfiguration} class.</p>
 *
 * <p>The annotation is applied **before** the return-type handler runs. The framework
 * transforms the handler's return value:</p>
 * <ul>
 *   <li>{@code String} → {@code PlainReply.of(text).withMarkup(id)}</li>
 *   <li>{@code PlainReply} (no markup set) → new {@code PlainReply} with markup ID</li>
 *   <li>{@code LocalizedReply} (no markup set) → {@code reply.withMarkup(id)}</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @BotCommand("/start")
 * @BotReplyMarkup("main_menu")
 * public String onStart(User user) {
 *     return "Welcome, " + user.getFirstName() + "!";
 * }
 * }</pre>
 *
 * <p>Use the programmatic {@code .withMarkup("id")} API on {@code PlainReply} or
 * {@code LocalizedReply} when the markup ID is determined at runtime.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotReplyMarkup {

    /**
     * The ID of the pre-registered markup to attach to the response.
     *
     * @return the markup ID; must match a registration made via {@link BotMarkup}
     */
    String value();
}
