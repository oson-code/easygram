package uz.osoncode.easygram.core.model;

/**
 * Named constants for framework-managed {@link BotRequest} attribute keys.
 *
 * <p>Use these constants with {@link BotRequest#getAttribute(String, Class)} to avoid
 * stringly-typed key lookups and to get IDE autocompletion for well-known attributes.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * Class<?> controller = request.getAttribute(BotRequestAttributes.CONTROLLER_CLASS, Class.class);
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.7
 */
public final class BotRequestAttributes {

    /**
     * The {@link Class} of the {@code @BotController} bean that handled the current request.
     * Set by the framework before invoking the controller method; available to filters and
     * exception handlers for scoping decisions.
     *
     * <p>Type: {@code Class<?>}</p>
     */
    public static final String CONTROLLER_CLASS = "easygram.controllerClass";

    /**
     * The {@link uz.osoncode.easygram.core.markup.BotMarkupContext} resolved from
     * {@code @BotReplyMarkup} or {@code @BotClearMarkup} annotations.
     * Set by the framework before invoking markup factory beans.
     *
     * <p>Type: {@link uz.osoncode.easygram.core.markup.BotMarkupContext}</p>
     */
    public static final String MARKUP_CONTEXT =
            uz.osoncode.easygram.core.markup.BotMarkupContext.REQUEST_ATTRIBUTE_KEY;

    /**
     * The parsed payload string extracted from a dynamic callback query pattern.
     * Set by the framework when a {@code @BotDynamicCallbackQuery} handler matches.
     *
     * <p>Type: {@code String}</p>
     */
    public static final String DYNAMIC_CALLBACK_DATA =
            uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData.ATTRIBUTE_KEY;

    private BotRequestAttributes() {}
}
