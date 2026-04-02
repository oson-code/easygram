package uz.osoncode.easygram.core.dynamiccallback;

/**
 * SPI for managing dynamic callback query payloads.
 *
 * <p>Implementations store a {@link BotDynamicCallbackData} payload under a string key
 * (typically a UUID placed in the Telegram inline button's {@code callbackData} field)
 * and retrieve it when the corresponding callback query arrives.</p>
 *
 * <p>This pattern allows bots to attach rich structured data to inline keyboard buttons
 * without being constrained by Telegram's 64-byte callback data limit.</p>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * // When building the keyboard button:
 * String key = UUID.randomUUID().toString();
 * dynamicService.store(key, BotDynamicCallbackData.builder()
 *     .type("product_buy")
 *     .put("id", productId)
 *     .build());
 * InlineKeyboardButton button = InlineKeyboardButton.builder()
 *     .text("Buy")
 *     .callbackData(key)
 *     .build();
 *
 * // Alternatively, use BotKeyboardFactory which generates the key automatically:
 * keyboardFactory.dynamicInlineButton("btn.buy",
 *     BotDynamicCallbackData.builder().type("product_buy").put("id", productId).build(),
 *     request);
 *
 * // When the callback fires, @BotDynamicCallbackQuery("product_buy") routes to the handler:
 * @BotDynamicCallbackQuery("product_buy")
 * public String onBuy(BotDynamicCallbackData data) { ... }
 * }</pre>
 *
 * <p>The default in-memory implementation is provided by the framework and is replaced by
 * declaring a custom {@code @Bean} of this type (e.g., a Redis- or database-backed store).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.4
 * @see BotDynamicCallbackData
 */
public interface BotDynamicCallbackQueryService {

    /**
     * Looks up the payload stored under the given callback data key.
     *
     * @param callbackData the raw Telegram callback data string (the lookup key); must not be {@code null}
     * @return the associated {@link BotDynamicCallbackData}, or {@code null} if not found
     */
    BotDynamicCallbackData resolve(String callbackData);

    /**
     * Stores a payload under the given callback data key, replacing any previous mapping.
     *
     * @param callbackData the key to store the payload under; must not be {@code null}
     * @param payload      the payload to persist; must not be {@code null}
     */
    void store(String callbackData, BotDynamicCallbackData payload);

    /**
     * Removes the payload associated with the given callback data key, if present.
     * This method is a no-op when the key is not found.
     *
     * @param callbackData the key to remove; must not be {@code null}
     */
    void remove(String callbackData);
}
