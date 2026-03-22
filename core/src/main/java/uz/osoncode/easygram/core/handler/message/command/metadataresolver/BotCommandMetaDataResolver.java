package uz.osoncode.easygram.core.handler.message.command.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotCommand} annotation.
 *
 * <p>Determines whether an incoming {@link BotRequest} contains a bot command message
 * whose command name matches one of the values declared in the {@link BotCommand} annotation.
 * The leading {@code "/"} character is stripped from both the message text and the annotation
 * values before comparison, so both {@code "/start"} and {@code "start"} are treated equally.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotCommandMetaDataResolver implements BotMetaDataSpecResolver<BotCommand> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotCommand} annotation class.
     */
    @Override
    public Class<BotCommand> getAnnotationType() {
        return BotCommand.class;
    }

    /**
     * Returns {@code true} if the update contains a message that starts with {@code "/"}
     * and whose command name (the first token, without the leading slash) equals one of
     * the values specified in the {@link BotCommand} annotation.
     *
     * <p>Annotation values may include or omit the leading {@code "/"} — both forms are
     * normalized before comparison. Only the command portion (before any space) is compared,
     * so {@code "/start arg"} matches a handler declared for {@code "start"}.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotCommand} annotation declared on the handler method.
     * @return {@code true} if the message command matches any of the annotation's values;
     *         {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotCommand annotation) {
        if (botRequest.getUpdate().hasMessage()
                && botRequest.getUpdate().getMessage().hasText()
                && botRequest.getUpdate().getMessage().getText().startsWith("/")) {
            String messageText = botRequest.getUpdate().getMessage().getText().split(" ")[0].substring(1);
            for (String text : annotation.value()) {
                text = text.startsWith("/") ? text.substring(1) : text;
                if (messageText.equals(text)) return true;
            }
        }
        return false;
    }
}
