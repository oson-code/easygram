package uz.osoncode.easygram.core.handler.message.command.metadataresolver;

import uz.osoncode.easygram.core.bind.annotation.BotDefaultCommand;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataDefaultResolver;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Metadata resolver for the {@link BotDefaultCommand} annotation.
 *
 * <p>Acts as a fallback resolver that matches any message containing a bot command
 * (i.e. text starting with {@code "/"}), regardless of the specific command name.
 * Handler methods annotated with {@link BotDefaultCommand} are invoked when no specific
 * {@link uz.osoncode.easygram.core.bind.annotation.BotCommand}
 * handler matched the incoming command.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotDefaultCommandMetaDataResolver implements BotMetaDataDefaultResolver<BotDefaultCommand> {

    /**
     * Returns the annotation type handled by this resolver.
     *
     * @return the {@link BotDefaultCommand} annotation class.
     */
    @Override
    public Class<BotDefaultCommand> getAnnotationType() {
        return BotDefaultCommand.class;
    }

    /**
     * Returns {@code true} if the update contains a message whose text starts with {@code "/"},
     * indicating that it is a bot command of any kind.
     *
     * @param botRequest the current bot request containing the Telegram {@code Update}.
     * @param annotation the {@link BotDefaultCommand} annotation declared on the handler method.
     * @return {@code true} if the update is a command message; {@code false} otherwise.
     */
    @Override
    public boolean support(BotRequest botRequest, BotDefaultCommand annotation) {
        return botRequest.getUpdate().hasMessage()
                && botRequest.getUpdate().getMessage().hasText()
                && botRequest.getUpdate().getMessage().getText().startsWith("/");
    }
}
