package uz.osoncode.easygram.core.i18n.returntypehandler;

import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.LocalizedReply;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReplyMessageHelper;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link BotReturnTypeHandler} that processes handler methods returning a {@link LocalizedReply}.
 * Resolves the message key directly using the {@link BotMessageSource} and formats it with
 * provided arguments.
 *
 * <p>When {@link LocalizedReply#isEditMessage()} is {@code true} and the request originates
 * from a callback query, the originating message is edited in-place via {@code EditMessageText}
 * instead of sending a new message. In edit context only {@code InlineKeyboardMarkup} is
 * supported; other keyboard types are silently ignored.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotLocalizedReplyReturnTypeHandler implements BotReturnTypeHandler {

    private final BotMessageSource botMessageSource;
    private final Optional<BotMarkupRegistry> botMarkupRegistry;

    public BotLocalizedReplyReturnTypeHandler(BotMessageSource botMessageSource, Optional<BotMarkupRegistry> botMarkupRegistry) {
        this.botMessageSource = botMessageSource;
        this.botMarkupRegistry = botMarkupRegistry;
    }

    @Override
    public boolean supportsReturnType(Method method) {
        return LocalizedReply.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {
        if (Objects.isNull(returnValue)) {
            return;
        }
        LocalizedReply reply = (LocalizedReply) returnValue;
        String resolved = botMessageSource.getMessage(reply.getKey(), botRequest, reply.getArgs());
        BotReplyMessageHelper.addReply(
                botResponse,
                botRequest,
                resolved,
                reply.isEditMessage(),
                reply.getKeyboard(),
                reply.isRemoveMarkup(),
                botMarkupRegistry,
                reply.getMarkupId(),
                reply.getMarkupParams());
        if (reply.isAnswerCallbackQuery()) {
            BotReplyMessageHelper.addCallbackAnswer(
                    botResponse,
                    botRequest,
                    resolved,
                    reply.isCallbackAlert(),
                    reply.getCallbackUrl(),
                    reply.getCallbackCacheTime());
        }
    }

    @Override
    public boolean supportsElement(Object element) {
        return element instanceof LocalizedReply;
    }
}
