package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.PlainReply;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link BotReturnTypeHandler} that handles handler methods returning a {@link PlainReply}.
 *
 * <p>The reply text is sent as-is (no template resolution). When {@link PlainReply#isEditMessage()}
 * is {@code true} and the request originates from a callback query, the originating message is
 * edited in-place via {@code EditMessageText} instead of sending a new message.</p>
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
        if (!reply.isCallbackAlert()) {
            BotReplyMessageHelper.addReply(
                    botResponse,
                    botRequest,
                    reply.getText(),
                    reply.isEditMessage(),
                    reply.getKeyboard(),
                    reply.isRemoveMarkup(),
                    markupRegistry,
                    reply.getMarkupId(),
                    reply.getMarkupParams());
        }
        if (reply.isAnswerCallbackQuery()) {
            BotReplyMessageHelper.addCallbackAnswer(
                    botResponse,
                    botRequest,
                    reply.getText(),
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
}
