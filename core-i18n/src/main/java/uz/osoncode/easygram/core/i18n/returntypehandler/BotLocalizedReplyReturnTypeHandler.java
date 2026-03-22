package uz.osoncode.easygram.core.i18n.returntypehandler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.LocalizedReply;
import uz.osoncode.easygram.core.markup.BotMarkupContext;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * {@link BotReturnTypeHandler} that processes handler methods returning a {@link LocalizedReply}.
 * Resolves the message key directly using the {@link BotMessageSource} and formats it with provided arguments.
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
        if (returnValue == null) {
            return;
        }
        LocalizedReply reply = (LocalizedReply) returnValue;
        String resolved = botMessageSource.getMessage(reply.getKey(), botRequest, reply.getArgs());
        
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(botRequest.getChat().getId())
                .text(resolved);
                
        if (reply.isRemoveMarkup()) {
            builder.replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
        } else if (reply.getKeyboard() != null) {
            builder.replyMarkup(reply.getKeyboard());
        } else {
            String markupId = reply.getMarkupId();
            if (markupId != null) {
                botMarkupRegistry.ifPresent(registry -> {
                    Map<String, Object> params = reply.getMarkupParams();
                    if (params != null) {
                        botRequest.setAttribute(BotMarkupContext.REQUEST_ATTRIBUTE_KEY, BotMarkupContext.of(params));
                    }
                    try {
                        ReplyKeyboard markup = registry.resolve(markupId, botRequest);
                        if (markup != null) {
                            builder.replyMarkup(markup);
                        }
                    } finally {
                        if (params != null) {
                            botRequest.setAttribute(BotMarkupContext.REQUEST_ATTRIBUTE_KEY, null);
                        }
                    }
                });
            }
        }
        
        botResponse.addBotApiMethod(builder.build());
    }
    
    @Override
    public boolean supportsElement(Object element) {
        return element instanceof LocalizedReply;
    }
}
