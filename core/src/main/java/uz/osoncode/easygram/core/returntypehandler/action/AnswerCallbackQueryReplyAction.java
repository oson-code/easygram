package uz.osoncode.easygram.core.returntypehandler.action;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.ReplyOptions;
import uz.osoncode.easygram.core.returntypehandler.BotReplyAction;
import uz.osoncode.easygram.core.returntypehandler.BotReplyMessageHelper;

/**
 * Default {@link BotReplyAction} that answers the originating callback query via
 * {@code answerCallbackQuery}.
 *
 * <p>Fires whenever {@link ReplyOptions#answerCallbackQuery()} is {@code true}.
 * This action can fire alongside {@link SendMessageReplyAction} or
 * {@link EditMessageReplyAction} — for example, to both send a message and dismiss
 * the callback spinner in the same update.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.6
 */
public class AnswerCallbackQueryReplyAction implements BotReplyAction {

    @Override
    public boolean supports(ReplyOptions options, BotRequest request) {
        return options.answerCallbackQuery();
    }

    @Override
    public void execute(BotRequest request, BotResponse response, String resolvedText, ReplyOptions options) {
        BotReplyMessageHelper.addCallbackAnswer(
                response, request, resolvedText,
                options.callbackAlert(),
                options.callbackUrl(),
                options.callbackCacheTime());
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
