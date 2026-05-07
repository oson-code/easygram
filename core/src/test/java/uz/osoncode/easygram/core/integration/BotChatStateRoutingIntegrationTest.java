package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotForwardChatState;
import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.chatstate.InMemoryBotChatStateService;
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

/**
 * Integration tests for chat-state-driven routing with {@code @BotChatState} and
 * {@code @BotForwardChatState}.
 */
class BotChatStateRoutingIntegrationTest {

    private static final AtomicBoolean START_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean WAITING_NAME_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean GENERIC_TEXT_CALLED = new AtomicBoolean(false);

    private static final long CHAT_ID = 999L;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(ChatStateController.class)
            .withBean(BotChatStateService.class, () -> new InMemoryBotChatStateService());

    @Test
    void startCommand_noState_triggersStartHandler() throws Exception {
        START_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, BotCommandRoutingIntegrationTest.buildMessageUpdate("/start", CHAT_ID));
            assertThat(START_CALLED.get()).isTrue();
        });
    }

    @Test
    void afterStart_stateIsWaitingName() throws Exception {
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);

            // Dispatch /start to advance state
            dispatch(context, BotCommandRoutingIntegrationTest.buildMessageUpdate("/start", CHAT_ID));

            assertThat(stateService.getState(CHAT_ID)).isEqualTo("WAITING_NAME");
        });
    }

    @Test
    void inWaitingNameState_textRoutesToStateHandler() throws Exception {
        WAITING_NAME_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);

            // Put chat into WAITING_NAME state
            stateService.setState(CHAT_ID, "WAITING_NAME");

            // Dispatch any text message
            dispatch(context, BotCommandRoutingIntegrationTest.buildMessageUpdate("Alice", CHAT_ID));
            assertThat(WAITING_NAME_CALLED.get()).isTrue();
        });
    }

    @Test
    void noState_genericTextHandlerCalled() throws Exception {
        GENERIC_TEXT_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            BotChatStateService stateService = context.getBean(BotChatStateService.class);

            // Ensure no state
            stateService.clearState(CHAT_ID);

            // Dispatch text that would match generic handler
            dispatch(context, BotCommandRoutingIntegrationTest.buildMessageUpdate("hello", CHAT_ID));
            assertThat(GENERIC_TEXT_CALLED.get()).isTrue();
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void dispatch(
            org.springframework.context.ApplicationContext ctx, Update update) throws Exception {
        List<BotFilter> filters = new ArrayList<>(ctx.getBeansOfType(BotFilter.class).values());
        BotDispatcher dispatcher = ctx.getBean(BotDispatcher.class);
        BotExceptionHandlerRegistry exReg = ctx.getBean(BotExceptionHandlerRegistry.class);
        DefaultBotFilterChain chain = new DefaultBotFilterChain(filters, dispatcher, exReg);
        BotRequest request = new BotRequest();
        request.setUpdate(update);
        chain.doFilter(request, new BotResponse());
    }

    // ── test controller ───────────────────────────────────────────────────────

    @BotController
    public static class ChatStateController {

        @BotCommand("/start")
        @BotForwardChatState("WAITING_NAME")
        public void onStart() {
            START_CALLED.set(true);
        }

        @BotTextDefault
        @BotChatState("WAITING_NAME")
        public void onWaitingName() {
            WAITING_NAME_CALLED.set(true);
        }

        @BotText("hello")
        public void onHello() {
            GENERIC_TEXT_CALLED.set(true);
        }
    }
}
