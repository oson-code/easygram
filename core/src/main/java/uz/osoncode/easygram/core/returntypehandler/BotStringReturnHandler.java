package uz.osoncode.easygram.core.returntypehandler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import uz.osoncode.easygram.core.bind.annotation.BotClearMarkup;
import uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * {@link BotReturnTypeHandler} implementation that handles handler methods returning a {@link String}.
 *
 * <p>The returned string is automatically wrapped in a {@link SendMessage} addressed to the
 * originating chat (derived from {@link BotRequest#getChat()}) and added to {@link BotResponse}.
 * This allows handler methods to reply to the user simply by returning the message text.</p>
 *
 * <p>Markup annotations ({@link BotReplyMarkup}, {@link BotClearMarkup}) are applied
 * upstream by {@code MarkupApplicationFilter} before dispatch reaches this handler.
 * When markup is present on a {@code String}-returning method, the filter converts the
 * return value into a {@link uz.osoncode.easygram.core.reply.PlainReply} so that it is
 * dispatched via {@code BotPlainReplyReturnTypeHandler} instead. This handler therefore
 * only receives plain strings with no markup to apply.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotStringReturnHandler implements BotReturnTypeHandler {

    private final Optional<BotMarkupRegistry> markupRegistry;

    public BotStringReturnHandler(Optional<BotMarkupRegistry> markupRegistry) {
        this.markupRegistry = markupRegistry;
    }

    /**
     * Determines whether this handler supports the return type of the given method.
     *
     * <p>Returns {@code true} when the method's return type is {@link String}.</p>
     *
     * @param method the handler method whose return type is checked; must not be {@code null}
     * @return {@code true} if the return type is {@link String}
     */
    @Override
    public boolean supportsReturnType(Method method) {
        return String.class.isAssignableFrom(method.getReturnType());
    }

    /**
     * Wraps the returned string in a {@link SendMessage} and adds it to the {@link BotResponse}.
     *
     * @param botRequest  the current bot request used to determine the target chat; must not be {@code null}
     * @param botResponse the mutable response object to which the {@link SendMessage} is added; must not be {@code null}
     * @param returnValue the {@link String} value returned by the handler method; may be {@code null}
     */
    @Override
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {
        if (returnValue == null) return;
        botResponse.addBotApiMethod(
                SendMessage.builder()
                        .chatId(botRequest.getChat().getId())
                        .text((String) returnValue)
                        .build()
        );
    }

    /**
     * Returns {@code true} when the element is a {@link String} instance.
     * Used by {@code BotMixedCollectionReturnTypeHandler} for per-element dispatch.
     */
    @Override
    public boolean supportsElement(Object element) {
        return element instanceof String;
    }
}
