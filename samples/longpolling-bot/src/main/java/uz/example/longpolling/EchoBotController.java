package uz.example.longpolling;

import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.bind.annotation.BotCommandValue;
import uz.osoncode.easygram.core.bind.annotation.BotTextValue;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;

/**
 * Bot controller for handling user commands and messages in the longpolling sample.
 * A simple echo bot that repeats messages back to users with longpolling transport.
 *
 * @since 0.0.1
 */
@BotController
public class EchoBotController {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "! I am an echo bot. Send me any message and I will repeat it.\n\nUse /help to see available commands.";
    }

    @BotCommand("/help")
    public String onHelp(@BotCommandValue String command) {
        return """
                Available commands:
                /start — welcome message
                /help  — this help text
                
                Send any text message and I will echo it back.
                """;
    }

    @BotTextDefault
    public String onText(@BotTextValue String text, User user) {
        return user.getFirstName() + " said: " + text;
    }

    @BotDefaultHandler
    public String onUnknown() {
        return "I don't know how to handle that. Try /help.";
    }
}
