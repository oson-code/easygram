package uz.example.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link EchoBotController} in the webhook-bot sample.
 * Handler routing is transport-agnostic — these tests use CoreAutoConfiguration only.
 */
class EchoBotControllerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(EchoBotController.class);

    @Test
    void startCommand_returnsGreetingWithUserName() throws Exception {
        BotResponse response = dispatch("/start");
        assertThat(firstText(response)).contains("Hello", "Bob").contains("webhook");
    }

    @Test
    void helpCommand_returnsHelpText() throws Exception {
        BotResponse response = dispatch("/help");
        assertThat(firstText(response)).contains("/start").contains("/help");
    }

    @Test
    void textMessage_echoesBackWithUserName() throws Exception {
        BotResponse response = dispatch("ping");
        assertThat(firstText(response)).contains("Bob").contains("ping");
    }

    @Test
    void unknownUpdate_defaultHandlerFiresWithChatId() throws Exception {
        BotResponse response = dispatchUnknown();
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getChatId()).isEqualTo("7");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private BotResponse dispatch(String text) throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = buildMessageUpdate(text, 7L, "Bob");
            BotRequest request = new BotRequest();
            request.setUpdate(update);
            List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
            new DefaultBotFilterChain(filters, context.getBean(BotDispatcher.class),
                    context.getBean(BotExceptionHandlerRegistry.class)).doFilter(request, response);
        });
        return response;
    }

    private BotResponse dispatchUnknown() throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = mock(Update.class);
            when(update.hasMessage()).thenReturn(false);
            BotRequest request = new BotRequest();
            request.setUpdate(update);
            User user = mock(User.class);
            when(user.getId()).thenReturn(7L);
            Chat chat = mock(Chat.class);
            when(chat.getId()).thenReturn(7L);
            request.setUser(user);
            request.setChat(chat);
            List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
            new DefaultBotFilterChain(filters, context.getBean(BotDispatcher.class),
                    context.getBean(BotExceptionHandlerRegistry.class)).doFilter(request, response);
        });
        return response;
    }

    private static String firstText(BotResponse response) {
        assertThat(response.getBotApiMethods()).isNotEmpty();
        return ((SendMessage) response.getBotApiMethods().iterator().next()).getText();
    }

    private static Update buildMessageUpdate(String text, long chatId, String firstName) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(chatId);
        when(user.getFirstName()).thenReturn(firstName);
        when(user.getLanguageCode()).thenReturn("en");

        Chat chat = mock(Chat.class);
        when(chat.getId()).thenReturn(chatId);
        when(chat.getType()).thenReturn("private");

        Message message = mock(Message.class);
        when(message.getText()).thenReturn(text);
        when(message.getFrom()).thenReturn(user);
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);

        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        return update;
    }
}
