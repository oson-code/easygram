package uz.osoncode.easygram.core.returntypehandler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.osoncode.easygram.core.reply.PlainTextTemplate;
import uz.osoncode.easygram.core.markup.BotMarkupContext;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * {@link BotReturnTypeHandler} that processes handler methods returning a {@link PlainTextTemplate}.
 * Formats the text using {@link String#format} if arguments are present, otherwise uses it as-is.
 * Does NOT perform any message bundle lookups.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPlainTextTemplateReturnTypeHandler implements BotReturnTypeHandler {

    private final Optional<BotMarkupRegistry> botMarkupRegistry;

    public BotPlainTextTemplateReturnTypeHandler(Optional<BotMarkupRegistry> botMarkupRegistry) {
        this.botMarkupRegistry = botMarkupRegistry;
    }

    @Override
    public boolean supportsReturnType(Method method) {
        return PlainTextTemplate.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {
        if (returnValue == null) {
            return;
        }
        PlainTextTemplate reply = (PlainTextTemplate) returnValue;
        String text = reply.getTemplate();
        
        if (reply.getArgs() != null && reply.getArgs().length > 0) {
            text = String.format(text, reply.getArgs());
        }
        
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(botRequest.getChat().getId())
                .text(text);
                
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
        return element instanceof PlainTextTemplate;
    }
}
