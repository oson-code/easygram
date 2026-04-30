package uz.osoncode.easygram.webhook.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.handler.BotHandler;
import uz.osoncode.easygram.core.handler.BotHandlerRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.provider.EasygramExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.EasygramObjectMapperProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.webhook.EasygramWebhookProperties;
import uz.osoncode.easygram.webhook.WebhookBot;
import uz.osoncode.easygram.webhook.WebhookController;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link WebhookController} — tests HTTP routing and body deserialization
 * without starting a full Spring context.
 */
class WebhookControllerIntegrationTest {

    private static final AtomicBoolean UPDATE_HANDLED = new AtomicBoolean(false);

    private WebhookController controller;
    private EasygramWebhookProperties webhookProperties;
    private EasygramObjectMapperProvider mapperProvider;
    private WebhookBot webhookBot;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        var telegramClient = mock(org.telegram.telegrambots.meta.generics.TelegramClient.class);
        var botUser = org.telegram.telegrambots.meta.api.objects.User.builder()
                .id(1L)
                .firstName("Bot")
                .userName("test_bot")
                .isBot(true)
                .build();
        doReturn(botUser).when(telegramClient).execute(any(GetMe.class));

        EasygramProperties props = new EasygramProperties("test-token");
        webhookProperties = new EasygramWebhookProperties(
                "https://example.com/webhook", "/webhook", null, 40, false, false);

        BotHandlerRegistry registry = new BotHandlerRegistry();
        // Register a no-op default handler so the dispatcher doesn't throw for valid updates
        registry.registerDefault(new BotHandler() {
            @Override public boolean supports(BotRequest req) { return true; }
            @Override public void handle(BotRequest req, BotResponse resp) {}
            @Override public String info() { return "test-noop"; }
        });
        BotDispatcher dispatcher = new BotDispatcher(registry);

        webhookBot = new WebhookBot(
                props,
                webhookProperties,
                List.of(),
                List.of(),
                dispatcher,
                new BotExceptionHandlerRegistry(),
                token -> telegramClient,
                () -> java.util.concurrent.Executors.newSingleThreadExecutor()
        );

        mapperProvider = ObjectMapper::new;
        controller = new WebhookController(webhookBot, webhookProperties, mapperProvider);
    }

    @Test
    void validUpdate_returns200() throws Exception {
        String updateJson = """
                {"update_id":1,"message":{"message_id":1,"text":"hello",
                "chat":{"id":1,"type":"private"},"from":{"id":1,"is_bot":false,"first_name":"Test"},
                "date":1700000000}}
                """;

        ResponseEntity<Void> response = controller.receiveUpdate(updateJson, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void invalidJson_returns500() {
        ResponseEntity<Void> response = controller.receiveUpdate("{ invalid json }", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handlerException_returns500() throws Exception {
        // Wire a bot whose dispatcher always throws so Telegram gets a 500 and retries
        BotHandlerRegistry throwingRegistry = new BotHandlerRegistry();
        throwingRegistry.registerDefault(new BotHandler() {
            @Override public boolean supports(BotRequest req) { return true; }
            @Override public void handle(BotRequest req, BotResponse resp) {
                throw new RuntimeException("simulated handler failure");
            }
            @Override public String info() { return "test-throwing"; }
        });
        BotDispatcher throwingDispatcher = new BotDispatcher(throwingRegistry);

        var throwingBot = new WebhookBot(
                new EasygramProperties("test-token"),
                webhookProperties,
                List.of(),
                List.of(),
                throwingDispatcher,
                new BotExceptionHandlerRegistry(),
                token -> mock(org.telegram.telegrambots.meta.generics.TelegramClient.class),
                () -> java.util.concurrent.Executors.newSingleThreadExecutor()
        );
        WebhookController throwingController =
                new WebhookController(throwingBot, webhookProperties, mapperProvider);

        String updateJson = """
                {"update_id":99,"message":{"message_id":99,"text":"fail",
                "chat":{"id":1,"type":"private"},"from":{"id":1,"is_bot":false,"first_name":"Test"},
                "date":1700000000}}
                """;

        ResponseEntity<Void> response = throwingController.receiveUpdate(updateJson, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void withSecretToken_correctToken_returns200() {
        webhookProperties = new EasygramWebhookProperties(
                "https://example.com/webhook", "/webhook", "my-secret", 40, false, false);
        controller = new WebhookController(webhookBot, webhookProperties, mapperProvider);

        String updateJson = """
                {"update_id":2,"message":{"message_id":2,"text":"hi",
                "chat":{"id":1,"type":"private"},"from":{"id":1,"is_bot":false,"first_name":"Test"},
                "date":1700000000}}
                """;

        ResponseEntity<Void> response = controller.receiveUpdate(updateJson, "my-secret");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void withSecretToken_wrongToken_returns401() {
        webhookProperties = new EasygramWebhookProperties(
                "https://example.com/webhook", "/webhook", "my-secret", 40, false, false);
        controller = new WebhookController(webhookBot, webhookProperties, mapperProvider);

        String updateJson = """
                {"update_id":3,"message":{"message_id":3,"text":"hey",
                "chat":{"id":1,"type":"private"},"from":{"id":1,"is_bot":false,"first_name":"Test"},
                "date":1700000000}}
                """;

        ResponseEntity<Void> response = controller.receiveUpdate(updateJson, "wrong-token");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void withSecretToken_missingHeader_returns401() {
        webhookProperties = new EasygramWebhookProperties(
                "https://example.com/webhook", "/webhook", "my-secret", 40, false, false);
        controller = new WebhookController(webhookBot, webhookProperties, mapperProvider);

        String updateJson = """
                {"update_id":4,"message":{"message_id":4,"text":"test",
                "chat":{"id":1,"type":"private"},"from":{"id":1,"is_bot":false,"first_name":"Test"},
                "date":1700000000}}
                """;

        ResponseEntity<Void> response = controller.receiveUpdate(updateJson, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
