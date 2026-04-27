package uz.osoncode.easygram.core.i18n.returntypehandler;

import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.LocalizedTemplate;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReplyMessageHelper;

import java.lang.reflect.Method;
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
 *       {@link BotMessageSource#getMessage(String, BotRequest, Object...)} — locale is derived
 *       automatically from the user's language code in the request.</li>
 *   <li>Every {@code #{index}} token is replaced with {@code String.valueOf(args[index])}.
 *       If {@code index} is out of bounds the token is left unchanged.</li>
 * </ol>
 *
 * <p>When {@link LocalizedTemplate#isEditMessage()} is {@code true} and the request originates
 * from a callback query, the originating message is edited in-place via {@code EditMessageText}
 * instead of sending a new message. In edit context only {@code InlineKeyboardMarkup} is
 * supported; other keyboard types are silently ignored.</p>
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
        if (!reply.isCallbackAlert()) {
            BotReplyMessageHelper.addReply(
                    botResponse,
                    botRequest,
                    resolved,
                    reply.isEditMessage(),
                    reply.getKeyboard(),
                    reply.isRemoveMarkup(),
                    botMarkupRegistry,
                    reply.getMarkupId(),
                    reply.getMarkupParams(),
                    reply.getParseMode());
        }
        if (reply.isAnswerCallbackQuery()) {
            BotReplyMessageHelper.addCallbackAnswer(botResponse, botRequest, resolved,
                    reply.isCallbackAlert(), reply.getCallbackUrl(), reply.getCallbackCacheTime());
        }
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
        String resolvedTemplate = resolveKeys(template, request);
        return resolveArgs(resolvedTemplate, args);
    }

    private String resolveKeys(String template, BotRequest request) {
        Matcher keyMatcher = KEY_PATTERN.matcher(template);
        StringBuilder keyResult = new StringBuilder();
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
        StringBuilder argResult = new StringBuilder();
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
