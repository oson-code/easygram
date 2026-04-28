package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.PlainReply;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * {@link BotReturnTypeHandler} that handles handler methods returning a {@link PlainReply}.
 *
 * <p>When the reply carries positional {@link PlainReply#getArgs() args}, the text is formatted
 * with {@link MessageFormat#format(String, Object[])} before sending, replacing {@code {0}},
 * {@code {1}}, … placeholders with the corresponding argument values.</p>
 *
 * <p>Dispatch to the actual Telegram Bot API call(s) is delegated to the
 * {@link BotReplyActionChain}, which fires each registered {@link BotReplyAction} whose
 * {@code supports()} returns {@code true} for the current request.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPlainReplyReturnTypeHandler implements BotReturnTypeHandler {

    private final BotReplyActionChain replyActionChain;

    public BotPlainReplyReturnTypeHandler(BotReplyActionChain replyActionChain) {
        this.replyActionChain = replyActionChain;
    }

    @Override
    public boolean supportsReturnType(Method method) {
        return PlainReply.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {
        if (Objects.isNull(returnValue)) {
            return;
        }
        PlainReply reply = (PlainReply) returnValue;
        String text = resolveText(reply);
        replyActionChain.execute(botRequest, botResponse, text, reply.getOptions());
    }

    /**
     * Returns {@code true} when the element is a {@link PlainReply} instance.
     * Used by {@code BotMixedCollectionReturnTypeHandler} for per-element dispatch.
     */
    @Override
    public boolean supportsElement(Object element) {
        return element instanceof PlainReply;
    }

    private static String resolveText(PlainReply reply) {
        Object[] args = reply.getArgs();
        if (args != null && args.length > 0) {
            return MessageFormat.format(reply.getText(), args);
        }
        return reply.getText();
    }
}

