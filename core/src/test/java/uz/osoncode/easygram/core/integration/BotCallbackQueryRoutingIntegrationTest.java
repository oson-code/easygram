package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotCallbackQuery;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultCallbackQuery;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@code @BotCallbackQuery} routing.
 */
class BotCallbackQueryRoutingIntegrationTest {

    private static final AtomicBoolean YES_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean NO_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean DEFAULT_CALLED = new AtomicBoolean(false);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(CallbackController.class);

    @Test
    void yesCallback_routesToYesHandler() throws Exception {
        YES_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "yes");
            assertThat(YES_CALLED.get()).isTrue();
        });
    }

    @Test
    void noCallback_routesToNoHandler() throws Exception {
        NO_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "no");
            assertThat(NO_CALLED.get()).isTrue();
        });
    }

    @Test
    void unknownCallback_routesToDefaultHandler() throws Exception {
        DEFAULT_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "other");
            assertThat(DEFAULT_CALLED.get()).isTrue();
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void dispatch(
            org.springframework.context.ApplicationContext ctx, String callbackData) throws Exception {
        List<BotFilter> filters = new ArrayList<>(ctx.getBeansOfType(BotFilter.class).values());
        BotDispatcher dispatcher = ctx.getBean(BotDispatcher.class);
        BotExceptionHandlerRegistry exReg = ctx.getBean(BotExceptionHandlerRegistry.class);
        DefaultBotFilterChain chain = new DefaultBotFilterChain(filters, dispatcher, exReg);

        Update update = buildCallbackUpdate(callbackData, 1L);
        BotRequest request = new BotRequest();
        request.setUpdate(update);
        chain.doFilter(request, new BotResponse());
    }

    static Update buildCallbackUpdate(String data, long chatId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(chatId);
        when(user.getLanguageCode()).thenReturn("en");

        Chat chat = mock(Chat.class);
        when(chat.getId()).thenReturn(chatId);
        when(chat.getType()).thenReturn("private");

        Message message = mock(Message.class);
        when(message.getChat()).thenReturn(chat);

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.getData()).thenReturn(data);
        when(callbackQuery.getFrom()).thenReturn(user);
        when(callbackQuery.getMessage()).thenReturn(message);

        Update update = mock(Update.class);
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        return update;
    }

    // ── test controller ───────────────────────────────────────────────────────

    @BotController
    public static class CallbackController {

        @BotCallbackQuery("yes")
        public void onYes() {
            YES_CALLED.set(true);
        }

        @BotCallbackQuery("no")
        public void onNo() {
            NO_CALLED.set(true);
        }

        @BotDefaultCallbackQuery
        public void onDefault() {
            DEFAULT_CALLED.set(true);
        }
    }
}
