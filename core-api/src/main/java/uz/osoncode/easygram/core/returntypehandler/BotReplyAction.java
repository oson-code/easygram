package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.ReplyOptions;

/**
 * SPI for sending a reply using one specific Telegram Bot API method.
 *
 * <p>The framework collects all {@code BotReplyAction} beans, sorts them by {@link #getOrder()},
 * and invokes each one whose {@link #supports(ReplyOptions, BotRequest)} returns {@code true}
 * for the current request and reply options. Multiple actions may fire for the same reply
 * (for example, {@code sendMessage} and {@code answerCallbackQuery} can both fire).</p>
 *
 * <p>Implementations should be registered as Spring beans. The default implementations
 * ({@code SendMessageReplyAction}, {@code EditMessageReplyAction},
 * {@code AnswerCallbackQueryReplyAction}) are registered automatically and can be replaced
 * by providing a bean with the same type annotated with {@code @Primary}, or by providing
 * a completely custom ordering via {@link #getOrder()}.</p>
 *
 * <h2>Implementing a custom action</h2>
 * <pre>{@code
 * @Component
 * public class PinMessageReplyAction implements BotReplyAction {
 *
 *     @Override
 *     public boolean supports(ReplyOptions options, BotRequest request) {
 *         return Boolean.TRUE.equals(options.pinMessage());  // hypothetical custom option
 *     }
 *
 *     @Override
 *     public void execute(BotRequest request, BotResponse response,
 *                         String resolvedText, ReplyOptions options) {
 *         // add a PinChatMessage to botResponse ...
 *     }
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
public interface BotReplyAction {

    /**
     * Returns {@code true} if this action should fire for the given reply options and request.
     *
     * @param options the reply options for the current reply
     * @param request the current bot request
     * @return {@code true} to execute this action
     */
    boolean supports(ReplyOptions options, BotRequest request);

    /**
     * Executes the Telegram Bot API call represented by this action, adding the result to
     * {@code response}.
     *
     * @param request      the current bot request
     * @param response     the response accumulator; add {@code BotApiMethod} instances here
     * @param resolvedText the final, fully-resolved message text (after i18n and MessageFormat)
     * @param options      the reply options for the current reply
     */
    void execute(BotRequest request, BotResponse response, String resolvedText, ReplyOptions options);

    /**
     * Returns the execution order of this action. Lower values run first.
     *
     * <p>Default actions use order 10 (send/edit) and 20 (callback answer). Custom actions
     * at order 0 (default) run before the built-in ones.</p>
     *
     * @return the execution order; default is {@code 0}
     */
    default int getOrder() {
        return 0;
    }
}
