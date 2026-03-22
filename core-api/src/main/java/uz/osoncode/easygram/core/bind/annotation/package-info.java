/**
 * Handler method routing and parameter-injection annotations for Easygram bot controllers.
 *
 * <h2>Routing annotations</h2>
 * Place these on methods inside a {@link uz.osoncode.easygram.core.stereotype.BotController} class
 * to declare which incoming Telegram updates each method handles:
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotCommand} — matches bot command messages (e.g. {@code /start})</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotDefaultCommand} — fallback for any unmatched command</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotText} — matches exact plain-text messages</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotTextDefault} — fallback for any unmatched text</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotReplyButton} — matches reply-keyboard button labels</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotCallbackQuery} — matches callback queries by {@code data} value</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotDefaultCallbackQuery} — fallback for any unmatched callback query</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotContact} — matches contact-sharing messages</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotLocation} — matches location-sharing messages</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler} — global fallback for any unmatched update</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotExceptionHandler} — handles a specific exception type</li>
 * </ul>
 *
 * <h2>Chat-state annotations</h2>
 * Restrict handler invocation to a specific conversational state and drive state transitions:
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.chatstate.BotChatState} — guard a handler to run only in a given state</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotForwardChatState} — advance to a new state after the handler returns</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotClearChatState} — clear the chat state after the handler returns</li>
 * </ul>
 *
 * <h2>Markup annotations</h2>
 * Attach or remove reply keyboards declaratively on handler methods:
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup} — attach a pre-registered keyboard to the outgoing message</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotClearMarkup} — remove the current reply keyboard</li>
 * </ul>
 *
 * <h2>Parameter-injection annotations</h2>
 * Extract values from the current update for injection into handler parameters:
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotCommandValue} — the matched command string (e.g. {@code "/start"})</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotCommandQueryParam} — a typed argument parsed from the command text</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotTextValue} — the full incoming message text</li>
 *   <li>{@link uz.osoncode.easygram.core.bind.annotation.BotCallbackQueryData} — the {@code data} field of a callback query</li>
 * </ul>
 */
package uz.osoncode.easygram.core.bind.annotation;
