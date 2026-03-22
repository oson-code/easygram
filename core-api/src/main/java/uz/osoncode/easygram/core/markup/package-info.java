/**
 * Markup registry contracts for managing declarative reply keyboards in the Easygram framework.
 *
 * <p>The markup system allows handler methods to attach pre-built
 * {@link org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard} instances
 * to outgoing messages by referencing them by ID — keeping keyboard construction logic
 * decoupled from handler routing logic.</p>
 *
 * <h2>Key types</h2>
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.markup.BotMarkupRegistry} — stores and retrieves markup factories keyed by string ID</li>
 *   <li>{@link uz.osoncode.easygram.core.markup.MarkupAware} — interface implemented by reply types ({@code PlainReply},
 *       {@code LocalizedReply}) that carry an optional markup ID or a remove-markup flag</li>
 * </ul>
 *
 * <h2>How markups are registered</h2>
 * <p>Annotate a {@link uz.osoncode.easygram.core.annotation.BotConfiguration} class with
 * {@link uz.osoncode.easygram.core.annotation.BotMarkup}-annotated factory methods:</p>
 * <pre>{@code
 * @BotConfiguration
 * public class MyMarkups {
 *
 *     @BotMarkup("main_menu")
 *     public ReplyKeyboard mainMenu() {
 *         return ReplyKeyboardMarkup.builder()
 *                 .keyboardRow(new KeyboardRow("Settings", "Help"))
 *                 .build();
 *     }
 *
 *     // Locale-aware variant
 *     @BotMarkup("main_menu_i18n")
 *     public ReplyKeyboard mainMenuLocalized(BotRequest request) {
 *         // resolve labels using BotMessageSource, etc.
 *     }
 * }
 * }</pre>
 *
 * <h2>How markups are consumed</h2>
 * <p>Reference a markup by ID on a handler method:</p>
 * <pre>{@code
 * @BotCommand("/start")
 * @BotReplyMarkup("main_menu")
 * public String onStart() { return "Welcome!"; }
 * }</pre>
 *
 * <p>Or programmatically:</p>
 * <pre>{@code
 * return PlainReply.of("Welcome!").withMarkup("main_menu");
 * }</pre>
 */
package uz.osoncode.easygram.core.markup;
