package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotExceptionHandler;
import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.exceptionhandler.BotMethodExceptionHandlerLoader;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@code @BotExceptionHandler} routing.
 */
class BotExceptionHandlerIntegrationTest {

    private static final AtomicBoolean HANDLER_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean EXCEPTION_HANDLER_CALLED = new AtomicBoolean(false);
    private static final AtomicReference<String> CAUGHT_MESSAGE = new AtomicReference<>();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(ExceptionController.class);

    @Test
    void handlerThrows_exceptionHandlerIsCalled() throws Exception {
        EXCEPTION_HANDLER_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            context.getBean(BotMethodExceptionHandlerLoader.class).run(null);
            dispatch(context, "trigger-error");
            assertThat(EXCEPTION_HANDLER_CALLED.get()).isTrue();
        });
    }

    @Test
    void exceptionHandler_receivesCorrectMessage() throws Exception {
        CAUGHT_MESSAGE.set(null);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            context.getBean(BotMethodExceptionHandlerLoader.class).run(null);
            dispatch(context, "trigger-error");
            assertThat(CAUGHT_MESSAGE.get()).isEqualTo("integration test error");
        });
    }

    @Test
    void handlerSucceeds_exceptionHandlerNotCalled() throws Exception {
        EXCEPTION_HANDLER_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            context.getBean(BotMethodExceptionHandlerLoader.class).run(null);
            dispatch(context, "normal");
            assertThat(EXCEPTION_HANDLER_CALLED.get()).isFalse();
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void dispatch(
            org.springframework.context.ApplicationContext ctx, String text) throws Exception {
        List<BotFilter> filters = new ArrayList<>(ctx.getBeansOfType(BotFilter.class).values());
        BotDispatcher dispatcher = ctx.getBean(BotDispatcher.class);
        BotExceptionHandlerRegistry exReg = ctx.getBean(BotExceptionHandlerRegistry.class);
        DefaultBotFilterChain chain = new DefaultBotFilterChain(filters, dispatcher, exReg);

        Update update = BotCommandRoutingIntegrationTest.buildMessageUpdate(text, 1L);
        BotRequest request = new BotRequest();
        request.setUpdate(update);
        chain.doFilter(request, new BotResponse());
    }

    // ── test controller ───────────────────────────────────────────────────────

    @BotController
    public static class ExceptionController {

        @BotText("trigger-error")
        public void onError() {
            HANDLER_CALLED.set(true);
            throw new IllegalStateException("integration test error");
        }

        @BotText("normal")
        public void onNormal() {
            HANDLER_CALLED.set(true);
        }

        @BotExceptionHandler(IllegalStateException.class)
        public void onIllegalState(IllegalStateException ex) {
            EXCEPTION_HANDLER_CALLED.set(true);
            CAUGHT_MESSAGE.set(ex.getMessage());
        }
    }
}
