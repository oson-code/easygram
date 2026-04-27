package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.reply.PlainTextTemplate;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;
import java.util.Objects;
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
 * <p>When {@link PlainTextTemplate#isEditMessage()} is {@code true} and the request
 * originates from a callback query, the originating message is edited in-place via
 * {@code EditMessageText} instead of sending a new message.</p>
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
        if (Objects.isNull(returnValue)) {
            return;
        }
        PlainTextTemplate reply = (PlainTextTemplate) returnValue;
        String text = resolveArgs(reply.getTemplate(), reply.getArgs());
        if (!reply.isCallbackAlert()) {
            BotReplyMessageHelper.addReply(
                    botResponse,
                    botRequest,
                    text,
                    reply.isEditMessage(),
                    reply.getKeyboard(),
                    reply.isRemoveMarkup(),
                    botMarkupRegistry,
                    reply.getMarkupId(),
                    reply.getMarkupParams(),
                    reply.getParseMode());
        }
        if (reply.isAnswerCallbackQuery()) {
            BotReplyMessageHelper.addCallbackAnswer(botResponse, botRequest, text,
                    reply.isCallbackAlert(), reply.getCallbackUrl(), reply.getCallbackCacheTime());
        }
    }

    @Override
    public boolean supportsElement(Object element) {
        return element instanceof PlainTextTemplate;
    }

    private String resolveArgs(String template, Object[] args) {
        if (Objects.isNull(template)) return null;
        Matcher matcher = ARG_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String value = (Objects.nonNull(args) && index < args.length)
                    ? String.valueOf(args[index])
                    : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
