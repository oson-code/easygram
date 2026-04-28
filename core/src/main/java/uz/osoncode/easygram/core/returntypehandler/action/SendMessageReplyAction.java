package uz.osoncode.easygram.core.returntypehandler.action;

import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.ReplyOptions;
import uz.osoncode.easygram.core.returntypehandler.BotReplyAction;
import uz.osoncode.easygram.core.returntypehandler.BotReplyMessageHelper;

import java.util.Optional;

/**
 * Default {@link BotReplyAction} that sends a new message via {@code sendMessage}.
 *
 * <p>Fires when the reply is not a callback-alert and either the {@code editMessage} flag
 * is {@code false}, or the current update is not a callback query (so there is nothing to
 * edit).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
public class SendMessageReplyAction implements BotReplyAction {

    private final Optional<BotMarkupRegistry> markupRegistry;

    /**
     * Creates a new {@code SendMessageReplyAction}.
     *
     * @param markupRegistry the markup registry used to resolve pre-registered keyboards by ID;
     *                       may be {@link java.util.Optional#empty()} when no registry is configured
     */
    public SendMessageReplyAction(Optional<BotMarkupRegistry> markupRegistry) {
        this.markupRegistry = markupRegistry;
    }

    @Override
    public boolean supports(ReplyOptions options, BotRequest request) {
        return !options.callbackAlert()
                && (!options.editMessage() || !request.getUpdate().hasCallbackQuery());
    }

    @Override
    public void execute(BotRequest request, BotResponse response, String resolvedText, ReplyOptions options) {
        BotReplyMessageHelper.addReply(
                response, request, resolvedText,
                false,
                options.keyboard(), options.removeMarkup(),
                markupRegistry, options.markupId(), options.markupParams(),
                options.toSendReplyOptions());
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
