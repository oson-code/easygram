package uz.example.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotCommandValue;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;
import uz.osoncode.easygram.core.bind.annotation.BotTextValue;
import uz.osoncode.easygram.core.stereotype.BotController;

/**
 * Echo bot controller for the tracing sample.
 *
 * <p>Each handler method logs its invocation. Because {@code micrometer-tracing-bridge-otel}
 * automatically populates the SLF4J MDC with {@code traceId} and {@code spanId}, every log line
 * printed here carries the same trace ID that appears in Grafana Tempo — making it trivial to
 * correlate log output with the distributed trace.
 *
 * @since 0.0.7
 */
@BotController
public class EchoBotController {

    private static final Logger log = LoggerFactory.getLogger(EchoBotController.class);

    @BotCommand("/start")
    public String onStart(User user) {
        log.info("Handling /start for user={}", user.getId());
        return """
                Hello, %s! I'm the tracing-bot 🔭
                
                Every update I handle creates an OpenTelemetry span.
                Open Grafana at http://localhost:3000 → Explore → Tempo
                to see the full trace (including the Kafka pub/sub hop).
                
                Send any text to generate a trace, or try /help.
                """.formatted(user.getFirstName());
    }

    @BotCommand("/help")
    public String onHelp(@BotCommandValue String command) {
        log.info("Handling /help, command='{}'", command);
        return """
                Available commands:
                /start — welcome + observability info
                /help  — this help text
                
                Send any text — I'll echo it and produce an easygram.update OTel span.
                Check Grafana Tempo at http://localhost:3000 for traces.
                """;
    }

    @BotTextDefault
    public String onText(@BotTextValue String text, User user) {
        log.info("Echoing text='{}' for user={}", text, user.getId());
        return "[tracing] " + user.getFirstName() + " said: " + text;
    }

    @BotDefaultHandler
    public String onUnknown() {
        log.warn("Unhandled update type received");
        return "I don't know how to handle that. Try /help.";
    }
}
