package uz.example.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.example.i18n.markup.RegistrationMarkups;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.chatstate.autoconfigure.ChatStateAutoConfiguration;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.exceptionhandler.BotMethodExceptionHandlerLoader;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.i18n.autoconfigure.BotI18nAutoConfiguration;
import uz.osoncode.easygram.core.markup.BotMarkupLoader;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the i18n-registration-bot sample.
 * Verifies LocalizedReply/LocalizedTemplate rendering and multi-step state routing
 * using the real English message bundle from src/main/resources.
 */
class I18nRegistrationBotIntegrationTest {

    private static final long CHAT_ID = 200L;
    private static final String FIRST_NAME = "Maria";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(
                    "easygram.token=test-token",
                    "easygram.i18n.enabled=true",
                    "spring.messages.basename=messages/bot",
                    "spring.messages.encoding=UTF-8"
            )
            .withConfiguration(AutoConfigurations.of(
                    MessageSourceAutoConfiguration.class,
                    BotI18nAutoConfiguration.class,
                    ChatStateAutoConfiguration.class,
                    CoreAutoConfiguration.class
            ))
            .withUserConfiguration(GlobalController.class, RegistrationController.class,
                    RegistrationMarkups.class);

    // ── /start ────────────────────────────────────────────────────────────────

    @Test
    void startCommand_repliesWithWelcomeContainingFirstName() throws Exception {
        BotResponse response = dispatch("/start");
        assertThat(firstText(response)).contains(FIRST_NAME);
    }

    // ── /register flow ────────────────────────────────────────────────────────

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
    void registerCommand_replyContainsStep1Prompt() throws Exception {
        BotResponse response = dispatch("/register");
        assertThat(firstText(response)).contains("Step 1/3");
    }

    @Test
    void textInAwaitingName_advancesToAwaitingPhone() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_NAME");

            dispatchInContext(context, "Maria Garcia");

            assertThat(stateService.getState(CHAT_ID)).isEqualTo("AWAITING_PHONE");
        });
    }

    @Test
    void textInAwaitingName_replyContainsNameSaved() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_NAME");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "Maria Garcia", response);

            assertThat(firstText(response)).contains("Step 2/3");
        });
    }

    @Test
    void validPhoneInAwaitingPhone_advancesToAwaitingCity() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_PHONE");

            dispatchInContext(context, "+998901234567");

            assertThat(stateService.getState(CHAT_ID)).isEqualTo("AWAITING_CITY");
        });
    }

    @Test
    void validPhoneInAwaitingPhone_replyContainsPhoneSaved() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_PHONE");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "+998901234567", response);

            assertThat(firstText(response)).contains("Step 3/3");
        });
    }

    @Test
    void invalidPhoneInAwaitingPhone_stateUnchanged() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_PHONE");

            dispatchInContext(context, "12345");

            assertThat(stateService.getState(CHAT_ID)).isEqualTo("AWAITING_PHONE");
        });
    }

    @Test
    void invalidPhoneInAwaitingPhone_replyContainsInvalidMessage() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_PHONE");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "12345", response);

            assertThat(firstText(response)).contains("Invalid phone number");
        });
    }

    @Test
    void textInAwaitingCity_clearsStateAndShowsComplete() throws Exception {
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
    void cancelButton_duringRegistration_clearsStateAndRepliesWithCancelled() throws Exception {
        runner.run(context -> {
            loadAll(context);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);
            stateService.setState(CHAT_ID, "AWAITING_NAME");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "❌ Cancel", response);

            assertThat(stateService.getState(CHAT_ID)).isNull();
            assertThat(firstText(response)).contains("cancelled");
        });
    }

    // ── /status ───────────────────────────────────────────────────────────────

    @Test
    void statusCommand_withNoState_reportsNoWizard() throws Exception {
        BotResponse response = dispatch("/status");
        assertThat(firstText(response)).contains("No active registration wizard");
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
    void statusCommand_inAwaitingPhone_reportsStep2() throws Exception {
        runner.run(context -> {
            loadAll(context);
            context.getBean(BotChatStateService.class).setState(CHAT_ID, "AWAITING_PHONE");

            BotResponse response = new BotResponse();
            dispatchWithResponse(context, "/status", response);

            assertThat(firstText(response)).contains("step 2/3");
        });
    }

    // ── /cancel ───────────────────────────────────────────────────────────────

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
            stateService.setState(CHAT_ID, "AWAITING_PHONE");

            dispatchInContext(context, "/cancel");

            assertThat(stateService.getState(CHAT_ID)).isNull();
        });
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
        context.getBean(BotMethodExceptionHandlerLoader.class).run(null);
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
