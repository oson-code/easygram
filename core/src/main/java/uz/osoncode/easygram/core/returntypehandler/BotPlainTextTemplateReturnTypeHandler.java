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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link BotReturnTypeHandler} that processes handler methods returning a {@link PlainTextTemplate}.
 *
 * <p>Resolves {@code #{index}} tokens in the template by substituting positional arguments
 * (0-based). Does NOT perform any message-bundle lookups — use {@code LocalizedTemplate}
 * (from {@code core-i18n}) when i18n is required.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotPlainTextTemplateReturnTypeHandler implements BotReturnTypeHandler {

    private static final Pattern ARG_PATTERN = Pattern.compile("#\\{(\\d+)}");

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
        String text = resolveArgs(reply.getTemplate(), reply.getArgs());

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

    private String resolveArgs(String template, Object[] args) {
        if (template == null) return null;
        Matcher matcher = ARG_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String value = (args != null && index < args.length)
                    ? String.valueOf(args[index])
                    : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
