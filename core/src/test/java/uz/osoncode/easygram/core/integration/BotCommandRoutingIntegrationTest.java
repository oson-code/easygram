package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultCommand;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotContextSetterFilter;
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
 * Integration tests for {@code @BotCommand} routing.
 */
class BotCommandRoutingIntegrationTest {

    private static final AtomicBoolean START_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean HELP_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean DEFAULT_CALLED = new AtomicBoolean(false);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(CommandController.class);

    @Test
    void startCommand_routesToStartHandler() throws Exception {
        START_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatchCommand(context, "/start");
            assertThat(START_CALLED.get()).isTrue();
        });
    }

    @Test
    void helpCommand_routesToHelpHandler() throws Exception {
        HELP_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatchCommand(context, "/help");
            assertThat(HELP_CALLED.get()).isTrue();
        });
    }

    @Test
    void unknownCommand_routesToDefaultHandler() throws Exception {
        DEFAULT_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatchCommand(context, "/unknown_xyz");
            assertThat(DEFAULT_CALLED.get()).isTrue();
        });
    }

    @Test
    void startCommand_doesNotTriggerHelpHandler() throws Exception {
        HELP_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatchCommand(context, "/start");
            assertThat(HELP_CALLED.get()).isFalse();
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void dispatchCommand(
            org.springframework.context.ApplicationContext ctx, String command) throws Exception {
        List<BotFilter> filters = new ArrayList<>(ctx.getBeansOfType(BotFilter.class).values());
        BotDispatcher dispatcher = ctx.getBean(BotDispatcher.class);
        BotExceptionHandlerRegistry exReg = ctx.getBean(BotExceptionHandlerRegistry.class);
        DefaultBotFilterChain chain = new DefaultBotFilterChain(filters, dispatcher, exReg);

        Update update = buildMessageUpdate(command, 42L);
        BotRequest request = new BotRequest();
        request.setUpdate(update);
        chain.doFilter(request, new BotResponse());
    }

    static Update buildMessageUpdate(String text, long chatId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(chatId);
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

    // ── test controller ───────────────────────────────────────────────────────

    @BotController
    public static class CommandController {

        @BotCommand("/start")
        public void onStart() {
            START_CALLED.set(true);
        }

        @BotCommand("/help")
        public void onHelp() {
            HELP_CALLED.set(true);
        }

        @BotDefaultCommand
        public void onDefault() {
            DEFAULT_CALLED.set(true);
        }
    }
}
