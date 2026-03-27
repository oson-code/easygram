package uz.osoncode.easygram.core.i18n.returntypehandler;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.LocalizedTemplate;
import uz.osoncode.easygram.core.markup.BotMarkupContext;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link BotReturnTypeHandler} that processes handler methods returning a {@link LocalizedTemplate}.
 *
 * <p>The template string inside {@link LocalizedTemplate} is resolved as follows:</p>
 * <ol>
 *   <li>Every {@code ${key}} token is replaced with the message obtained from
 *       {@link BotMessageSource#getMessage(String, BotRequest, Object...)} — locale is derived automatically
 *       from the user's language code in the request.</li>
 *   <li>Every {@code #{index}} token is replaced with {@code String.valueOf(args[index])}.
 *       If {@code index} is out of bounds the token is left unchanged.</li>
 * </ol>
 *
 * <p>The fully-resolved text is then wrapped in a {@link SendMessage} targeted at the
 * originating chat and added to {@link BotResponse}. If a markup ID is present, the
 * registered markup is resolved and attached.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotLocalizedTemplateReturnTypeHandler implements BotReturnTypeHandler {

    private static final Pattern KEY_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern ARG_PATTERN = Pattern.compile("#\\{(\\d+)}");

    private final BotMessageSource botMessageSource;
    private final Optional<BotMarkupRegistry> botMarkupRegistry;

    public BotLocalizedTemplateReturnTypeHandler(BotMessageSource botMessageSource, Optional<BotMarkupRegistry> botMarkupRegistry) {
        this.botMessageSource = botMessageSource;
        this.botMarkupRegistry = botMarkupRegistry;
    }

    @Override
    public boolean supportsReturnType(Method method) {
        return LocalizedTemplate.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue) {
        if (Objects.isNull(returnValue)) {
            return;
        }
        LocalizedTemplate reply = (LocalizedTemplate) returnValue;
        String resolved = resolveTemplate(reply.getTemplate(), botRequest, reply.getArgs());
        
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(botRequest.getChat().getId())
                .text(resolved);
                
        if (reply.isRemoveMarkup()) {
            builder.replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
        } else if (Objects.nonNull(reply.getKeyboard())) {
            builder.replyMarkup(reply.getKeyboard());
        } else {
            String markupId = reply.getMarkupId();
            if (Objects.nonNull(markupId)) {
                botMarkupRegistry.ifPresent(registry -> {
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
     * Returns {@code true} when the element is a {@link LocalizedTemplate} instance.
     * Used by {@code BotMixedCollectionReturnTypeHandler} for per-element dispatch.
     */
    @Override
    public boolean supportsElement(Object element) {
        return element instanceof LocalizedTemplate;
    }
    
    private String resolveTemplate(String template, BotRequest request, Object[] args) {
        // Replace ${key} tokens with message-bundle values
        String resolvedTemplate = resolveKeys(template, request);
        
        // Replace #{index} tokens with positional args
        return resolveArgs(resolvedTemplate, args);
    }
    
    private String resolveKeys(String template, BotRequest request) {
        Matcher keyMatcher = KEY_PATTERN.matcher(template);
        StringBuffer keyResult = new StringBuffer();
        while (keyMatcher.find()) {
            String key = keyMatcher.group(1);
            String value = botMessageSource.getMessage(key, request);
            keyMatcher.appendReplacement(keyResult, Matcher.quoteReplacement(value));
        }
        keyMatcher.appendTail(keyResult);
        return keyResult.toString();
    }
    
    private String resolveArgs(String text, Object[] args) {
        Matcher argMatcher = ARG_PATTERN.matcher(text);
        StringBuffer argResult = new StringBuffer();
        while (argMatcher.find()) {
            int index = Integer.parseInt(argMatcher.group(1));
            String value = (Objects.nonNull(args) && index < args.length)
                    ? String.valueOf(args[index])
                    : argMatcher.group(0);
            argMatcher.appendReplacement(argResult, Matcher.quoteReplacement(value));
        }
        argMatcher.appendTail(argResult);
        return argResult.toString();
    }
}
