package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.ReplyOptions;

import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates all registered {@link BotReplyAction} beans, executing each one whose
 * {@link BotReplyAction#supports(ReplyOptions, BotRequest)} returns {@code true}.
 *
 * <p>Actions are sorted by {@link BotReplyAction#getOrder()} in ascending order (lower
 * value = higher priority) and invoked in that order. Multiple actions may fire for
 * a single reply (for example, {@code sendMessage} at order 10 and
 * {@code answerCallbackQuery} at order 20 both fire when {@code answerCallbackQuery = true}
 * but {@code callbackAlert = false}).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
public class BotReplyActionChain {

    private final List<BotReplyAction> actions;

    /**
     * Creates a new chain from the given list of actions.
     * The actions are sorted by {@link BotReplyAction#getOrder()} once at construction time
     * (lower value = higher priority) and stored in that stable order.
     *
     * @param actions the actions to include in this chain; must not be {@code null}
     */
    public BotReplyActionChain(List<BotReplyAction> actions) {
        this.actions = actions.stream()
                .sorted(Comparator.comparingInt(BotReplyAction::getOrder))
                .toList();
    }

    /**
     * Executes all actions that support the current reply options and request.
     *
     * @param request      the current bot request
     * @param response     the response accumulator
     * @param resolvedText the fully-resolved message text
     * @param options      the reply options
     */
    public void execute(BotRequest request, BotResponse response, String resolvedText, ReplyOptions options) {
        for (BotReplyAction action : actions) {
            if (action.supports(options, request)) {
                action.execute(request, response, resolvedText, options);
            }
        }
    }
}
