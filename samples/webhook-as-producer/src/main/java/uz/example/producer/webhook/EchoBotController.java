package uz.example.producer.webhook;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.bind.annotation.BotTextValue;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Optional local handler — only active when {@code telegram.bot.messaging.forward-only=false}.
 * When {@code forward-only=true} (the default for a pure producer), this controller is still
 * registered but the {@code BotUpdatePublishingFilter} stops the chain before reaching it.
 */
@BotController
public class EchoBotController {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "! Updates are being forwarded to the message broker via webhook.";
    }

    @BotTextDefault
    public String onText(@BotTextValue String text) {
        return "Forwarded to broker: " + text;
    }

    @BotDefaultHandler
    public SendMessage onUnknown(BotRequest request) {
        return SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("Update forwarded to broker.")
                .build();
    }
}
