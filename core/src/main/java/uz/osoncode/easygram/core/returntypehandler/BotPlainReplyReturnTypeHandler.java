package uz.osoncode.easygram.core.returntypehandler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.osoncode.easygram.core.markup.BotMarkupContext;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.PlainReply;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link BotReturnTypeHandler} that handles handler methods returning a {@link PlainReply}.
 *
 * <p>The reply text is sent as-is (no template resolution). If a markup ID is set on the
 * {@link PlainReply} (via {@link PlainReply#withMarkup(String)}) and a
 * {@link BotMarkupRegistry} is present, the registered markup factory is invoked and the
 * resulting {@code ReplyKeyboard} is attached to the {@link SendMessage}.</p>
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
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(botRequest.getChat().getId())
                .text(reply.getText());

        if (reply.isRemoveMarkup()) {
            builder.replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
        } else if (Objects.nonNull(reply.getKeyboard())) {
            builder.replyMarkup(reply.getKeyboard());
        } else {
            String markupId = reply.getMarkupId();
            if (Objects.nonNull(markupId)) {
                markupRegistry.ifPresent(registry -> {
                    Map<String, Object> params = reply.getMarkupParams();
                    if (Objects.nonNull(params)) {
                        botRequest.setAttribute(BotMarkupContext.REQUEST_ATTRIBUTE_KEY, BotMarkupContext.of(params));
                    }
                    try {
                        ReplyKeyboard markup = registry.resolve(markupId, botRequest);
                        if (Objects.nonNull(markup)) {
                            builder.replyMarkup(markup);
                        }
                    } finally {
                        if (Objects.nonNull(params)) {
                            botRequest.setAttribute(BotMarkupContext.REQUEST_ATTRIBUTE_KEY, null);
                        }
                    }
                });
            }
        }

        botResponse.addBotApiMethod(builder.build());
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
