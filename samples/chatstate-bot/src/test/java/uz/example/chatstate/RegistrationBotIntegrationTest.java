package uz.example.chatstate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.example.chatstate.markup.RegistrationMarkups;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.chatstate.autoconfigure.ChatStateAutoConfiguration;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.markup.BotMarkupLoader;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the chatstate-bot registration wizard.
 * Verifies multi-step state transitions without starting a real Telegram transport.
 */
class RegistrationBotIntegrationTest {

    private static final long CHAT_ID = 100L;
    private static final String FIRST_NAME = "Alice";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class, ChatStateAutoConfiguration.class))
            .withUserConfiguration(GlobalCommandController.class, RegistrationFlowController.class,
                    RegistrationMarkups.class);

    @Test
    void startCommand_greetsUser() throws Exception {
        BotResponse response = dispatch("/start");
        assertThat(firstText(response)).contains("Hello", FIRST_NAME);
    }

    @Test
    void registerCommand_advancesToAwaitingName() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);

            dispatchInContext(context, "/register");

            assertThat(stateService.getState(CHAT_ID)).isEqualTo("AWAITING_NAME");
        });
    }

    @Test
    void registerCommand_replyContainsStep1() throws Exception {
        BotResponse response = dispatch("/register");
        assertThat(firstText(response)).contains("Step 1/3");
    }

    @Test
    void textInAwaitingName_advancesToAwaitingAge() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_NAME");

            dispatchInContext(context, "Alice Smith");

            assertThat(stateService.getState(CHAT_ID)).isEqualTo("AWAITING_AGE");
        });
    }

    @Test
    void textInAwaitingName_replyContainsStep2() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_NAME");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "Alice Smith", response);

            assertThat(firstText(response)).contains("Step 2/3");
        });
    }

    @Test
    void invalidAgeInAwaitingAge_stateUnchanged() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_AGE");

            dispatchInContext(context, "not-a-number");

            assertThat(stateService.getState(CHAT_ID)).isEqualTo("AWAITING_AGE");
        });
    }

    @Test
    void invalidAgeInAwaitingAge_replyContainsWarning() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_AGE");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "not-a-number", response);

            assertThat(firstText(response)).contains("doesn't look like a number");
        });
    }

    @Test
    void validAgeInAwaitingAge_advancesToAwaitingCity() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_AGE");

            dispatchInContext(context, "25");

            assertThat(stateService.getState(CHAT_ID)).isEqualTo("AWAITING_CITY");
        });
    }

    @Test
    void validAgeInAwaitingAge_replyContainsStep3() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_AGE");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "25", response);

            assertThat(firstText(response)).contains("Step 3/3");
        });
    }

    @Test
    void textInAwaitingCity_clearsStateAndShowsSummary() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_CITY");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "Tashkent", response);

            assertThat(stateService.getState(CHAT_ID)).isNull();
            assertThat(firstText(response)).contains("Registration complete");
        });
    }

    @Test
    void cancelButton_duringRegistration_clearsState() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_NAME");

            dispatchInContext(context, "❌ Cancel");

            assertThat(stateService.getState(CHAT_ID)).isNull();
        });
    }

    @Test
    void cancelButton_duringRegistration_repliesWithCancelled() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_NAME");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "❌ Cancel", response);

            assertThat(firstText(response)).contains("cancelled");
        });
    }

    @Test
    void statusCommand_withNoState_reportsNoWizard() throws Exception {
        BotResponse response = dispatch("/status");
        assertThat(firstText(response)).contains("no active registration wizard");
    }

    @Test
    void statusCommand_inAwaitingName_reportsStep1() throws Exception {
        runner.run(context -> {
            loadAll(context);
            context.getBean(BotChatStateService.class).setState(CHAT_ID, "AWAITING_NAME");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "/status", response);

            assertThat(firstText(response)).contains("step 1/3");
        });
    }

    @Test
    void cancelCommand_withNoState_reportsNothingToCancel() throws Exception {
        BotResponse response = dispatch("/cancel");
        assertThat(firstText(response)).contains("no active wizard");
    }

    @Test
    void cancelCommand_withActiveState_clearsState() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_AGE");

            dispatchInContext(context, "/cancel");

            assertThat(stateService.getState(CHAT_ID)).isNull();
        });
    }

    @Test
    void unknownUpdate_defaultHandlerFires() throws Exception {
        BotResponse response = dispatchUnknown();
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getText()).contains("I didn't understand");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private BotResponse dispatch(String text) throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            loadAll(context);
            dispatchWithResponse(context, text, response);
        });
        return response;
    }

    private BotResponse dispatchUnknown() throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            loadAll(context);
            Update update = mock(Update.class);
            when(update.hasMessage()).thenReturn(false);
            BotRequest request = new BotRequest();
            request.setUpdate(update);
            User user = mock(User.class);
            when(user.getId()).thenReturn(CHAT_ID);
            Chat chat = mock(Chat.class);
            when(chat.getId()).thenReturn(CHAT_ID);
            request.setUser(user);
            request.setChat(chat);
            filterChain(context).doFilter(request, response);
        });
        return response;
    }

    private void dispatchInContext(org.springframework.context.ApplicationContext context,
                                   String text) throws Exception {
        dispatchWithResponse(context, text, new BotResponse());
    }

    private void dispatchWithResponse(org.springframework.context.ApplicationContext context,
                                      String text, BotResponse response) throws Exception {
        BotRequest request = new BotRequest();
        request.setUpdate(buildMessageUpdate(text, CHAT_ID, FIRST_NAME));
        filterChain(context).doFilter(request, response);
    }

    private DefaultBotFilterChain filterChain(org.springframework.context.ApplicationContext context) {
        List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
        return new DefaultBotFilterChain(filters, context.getBean(BotDispatcher.class),
                context.getBean(BotExceptionHandlerRegistry.class));
    }

    private static void loadAll(org.springframework.context.ApplicationContext context) {
        context.getBean(BotHandlerLoader.class).run(null);
        context.getBean(BotMarkupLoader.class).run(null);
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
