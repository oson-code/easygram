package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotCallbackQuery;
import uz.osoncode.easygram.core.bind.annotation.BotCallbackQueryData;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultCallbackQuery;
import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;
import uz.osoncode.easygram.core.bind.annotation.BotTextValue;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for argument injection into handler methods.
 */
class BotArgumentResolverIntegrationTest {

    private static final AtomicReference<String> CAPTURED_TEXT = new AtomicReference<>();
    private static final AtomicReference<BotRequest> CAPTURED_REQUEST = new AtomicReference<>();
    private static final AtomicReference<String> CAPTURED_CALLBACK_DATA = new AtomicReference<>();
    private static final AtomicReference<String> CAPTURED_COMMAND = new AtomicReference<>();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(ArgumentController.class);

    @Test
    void botTextValue_injectsMessageText() throws Exception {
        CAPTURED_TEXT.set(null);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, BotCommandRoutingIntegrationTest.buildMessageUpdate("hello world", 1L));
            assertThat(CAPTURED_TEXT.get()).isEqualTo("hello world");
        });
    }

    @Test
    void botRequest_isInjected() throws Exception {
        CAPTURED_REQUEST.set(null);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, BotCommandRoutingIntegrationTest.buildMessageUpdate("inject-request", 1L));
            assertThat(CAPTURED_REQUEST.get()).isNotNull();
        });
    }

    @Test
    void botCallbackQueryData_injectsCallbackData() throws Exception {
        CAPTURED_CALLBACK_DATA.set(null);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, BotCallbackQueryRoutingIntegrationTest.buildCallbackUpdate("my-data", 1L));
            assertThat(CAPTURED_CALLBACK_DATA.get()).isEqualTo("my-data");
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
    public static class ArgumentController {

        @BotTextDefault
        public void onText(@BotTextValue String text) {
            CAPTURED_TEXT.set(text);
        }

        @BotText("inject-request")
        public void onRequest(BotRequest request) {
            CAPTURED_REQUEST.set(request);
        }

        @BotDefaultCallbackQuery
        public void onCallback(@BotCallbackQueryData String data) {
            CAPTURED_CALLBACK_DATA.set(data);
        }
    }
}
