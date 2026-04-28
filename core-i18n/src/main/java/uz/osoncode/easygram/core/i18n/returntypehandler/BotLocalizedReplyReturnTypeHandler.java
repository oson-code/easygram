package uz.osoncode.easygram.core.i18n.returntypehandler;

import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.i18n.LocalizedReply;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReplyActionChain;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * {@link BotReturnTypeHandler} that processes handler methods returning a {@link LocalizedReply}.
 * Resolves the message key using {@link BotMessageSource}, formats it with provided arguments,
 * then delegates all dispatch decisions to the {@link BotReplyActionChain}.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotLocalizedReplyReturnTypeHandler implements BotReturnTypeHandler {

    private final BotMessageSource botMessageSource;
    private final BotReplyActionChain chain;

    public BotLocalizedReplyReturnTypeHandler(BotMessageSource botMessageSource, BotReplyActionChain chain) {
        this.botMessageSource = botMessageSource;
        this.chain = chain;
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
        chain.execute(botRequest, botResponse, resolved, reply.getOptions());
    }

    @Override
    public boolean supportsElement(Object element) {
        return element instanceof LocalizedReply;
    }
}
