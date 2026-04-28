package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.PlainReply;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link BotReturnTypeHandler} that handles handler methods returning a {@link PlainReply}.
 *
 * <p>When the reply carries positional {@link PlainReply#getArgs() args}, the text is formatted
 * with {@link MessageFormat#format(String, Object[])} before sending, replacing {@code {0}},
 * {@code {1}}, … placeholders with the corresponding argument values.</p>
 *
 * <p>When {@link PlainReply#isEditMessage()} is {@code true} and the request originates from a
 * callback query, the originating message is edited in-place via {@code EditMessageText} instead
 * of sending a new message.</p>
 *
 * <p>If a markup ID is set on the {@link PlainReply} (via {@link PlainReply#withMarkup(String)})
 * and a {@link BotMarkupRegistry} is present, the registered markup factory is invoked and
 * the resulting {@code ReplyKeyboard} is attached to the response. In edit context only
 * {@code InlineKeyboardMarkup} is attached; other keyboard types are silently ignored.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPlainReplyReturnTypeHandler implements BotReturnTypeHandler {

    private final Optional<BotMarkupRegistry> markupRegistry;

    public BotPlainReplyReturnTypeHandler(Optional<BotMarkupRegistry> markupRegistry) {
        this.markupRegistry = markupRegistry;
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
        if (!reply.isCallbackAlert()) {
            BotReplyMessageHelper.addReply(
                    botResponse,
                    botRequest,
                    text,
                    reply.isEditMessage(),
                    reply.getKeyboard(),
                    reply.isRemoveMarkup(),
                    markupRegistry,
                    reply.getMarkupId(),
                    reply.getMarkupParams(),
                    reply.getParseMode());
        }
        if (reply.isAnswerCallbackQuery()) {
            BotReplyMessageHelper.addCallbackAnswer(
                    botResponse,
                    botRequest,
                    text,
                    reply.isCallbackAlert(),
                    reply.getCallbackUrl(),
                    reply.getCallbackCacheTime());
        }
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
