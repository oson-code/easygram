package uz.example.linkedlist;

import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotCommandValue;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;
import uz.osoncode.easygram.core.bind.annotation.BotTextValue;

/**
 * Bot controller for the LinkedList sample.
 *
 * <p>These handlers are invoked by the {@link uz.example.linkedlist.consumer.LinkedListBotConsumer}
 * after it dequeues an update from the in-memory {@link java.util.concurrent.LinkedBlockingDeque}.
 * The full round-trip is:</p>
 * <ol>
 *   <li>Telegram → long-polling transport</li>
 *   <li>{@code BotUpdatePublishingFilter} →
 *       {@link uz.example.linkedlist.publisher.LinkedListBotUpdatePublisher} → deque</li>
 *   <li>Consumer thread ← deque → {@code BotDispatcher} → these handlers</li>
 *   <li>Handler returns reply → {@code TelegramClient} → Telegram</li>
 * </ol>
 *
 * <p><strong>⚠ WARNING: for demonstration purposes only.</strong>
 * The {@link java.util.concurrent.LinkedBlockingDeque} used as the "broker" is entirely in-memory.
 * All queued updates are lost on application restart. Do not use this pattern in production — use
 * the {@code easygram-messaging-kafka} or {@code easygram-messaging-rabbit} modules instead.</p>
 *
 * @since 0.0.6
 */
@BotController
public class EchoBotController {

    @BotCommand("/start")
    public String onStart(User user) {
        return "Hello, " + user.getFirstName() + "! I use an in-memory LinkedList as my message broker.";
    }

    @BotCommand("/help")
    public String onHelp(@BotCommandValue String command) {
        return """
                Available commands:
                /start — welcome message
                /help  — this help text
                
                Send any text and I will echo it back via the in-memory LinkedList queue.
                """;
    }

    @BotTextDefault
    public String onText(@BotTextValue String text, User user) {
        return "[linkedlist] " + user.getFirstName() + " said: " + text;
    }

    @BotDefaultHandler
    public String onUnknown() {
        return "I don't know how to handle that. Try /help.";
    }
}
