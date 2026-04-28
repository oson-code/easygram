package uz.example.producer.longpolling;

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
 * Integration tests for {@link EchoBotController} in the longpolling-as-producer sample.
 * Handler routing is tested using CoreAutoConfiguration only — no message broker needed.
 */
class EchoBotControllerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(EchoBotController.class);

    @Test
    void startCommand_returnsGreetingWithBrokerInfo() throws Exception {
        BotResponse response = dispatch("/start");
        assertThat(firstText(response)).contains("Hello", "Eve").contains("broker");
    }

    @Test
    void textMessage_echoesForwardedText() throws Exception {
        BotResponse response = dispatch("forward me");
        assertThat(firstText(response)).contains("Forwarded to broker").contains("forward me");
    }

    @Test
    void unknownUpdate_defaultHandlerFiresWithChatId() throws Exception {
        BotResponse response = dispatchUnknown(11L);
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getChatId()).isEqualTo("11");
        assertThat(msg.getText()).contains("forwarded to broker");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private BotResponse dispatch(String text) throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = buildMessageUpdate(text, 11L, "Eve");
            BotRequest request = new BotRequest();
            request.setUpdate(update);
            List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
            new DefaultBotFilterChain(filters, context.getBean(BotDispatcher.class),
                    context.getBean(BotExceptionHandlerRegistry.class)).doFilter(request, response);
        });
        return response;
    }

    private BotResponse dispatchUnknown(long chatId) throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = mock(Update.class);
            when(update.hasMessage()).thenReturn(false);
            BotRequest request = new BotRequest();
            request.setUpdate(update);
            User user = mock(User.class);
            when(user.getId()).thenReturn(chatId);
            Chat chat = mock(Chat.class);
            when(chat.getId()).thenReturn(chatId);
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
