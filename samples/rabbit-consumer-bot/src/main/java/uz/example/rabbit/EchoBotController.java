package uz.example.rabbit;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.bind.annotation.BotCommandValue;
import uz.osoncode.easygram.core.bind.annotation.BotTextValue;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Bot controller for handling user commands and messages in the RabbitMQ consumer sample.
 * Echoes back text messages received through RabbitMQ transport and handles bot commands.
 *
 * @since 0.0.1
 */
@BotController
public class EchoBotController {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "! I receive updates from RabbitMQ and echo them back.";
    }

    @BotCommand("/help")
    public String onHelp(@BotCommandValue String command) {
        return """
                Available commands:
                /start — welcome message
                /help  — this help text
                
                Send any text and I will echo it back (via RabbitMQ consumer transport).
                """;
    }

    @BotTextDefault
    public String onText(@BotTextValue String text, User user) {
        return "[rabbit] " + user.getFirstName() + " said: " + text;
    }

    @BotDefaultHandler
    public SendMessage onUnknown(BotRequest request) {
        return SendMessage.builder()
                .chatId(request.getChat().getId())
                .text("I don't know how to handle that. Try /help.")
                .build();
    }
}
