package uz.osoncode.easygram.core.bind.annotation;

import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackQueryService;

import java.lang.annotation.*;

/**
 * Maps a handler method to one or more dynamic callback query types.
 *
 * <p>Unlike {@link BotCallbackQuery}, which matches the raw Telegram callback data string,
 * this annotation routes by a <em>type</em> field stored server-side via
 * {@link BotDynamicCallbackQueryService}. The raw callback data is used as a lookup key;
 * the resolved {@link uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData}
 * payload carries the actual type and a structured data map.</p>
 *
 * <p>This pattern is ideal when callback payloads exceed Telegram's 64-byte limit or
 * when rich structured data needs to be passed through an inline button click.</p>
 *
 * <pre>{@code
 * @BotDynamicCallbackQuery("product_select")
 * public String onProductSelect(BotDynamicCallbackData data) {
 *     Long id = (Long) data.getData().get("id");
 *     return "You selected product #" + id;
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.4
 * @see BotDynamicCallbackQueryService
 * @see uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotDynamicCallbackQuery {

    /**
     * One or more type strings to match against the resolved
     * {@link uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData#getType()}.
     * The annotated method is invoked when the resolved payload's type equals any of these values.
     *
     * @return the payload type(s) that trigger this handler
     */
    String[] value() default {};
}
