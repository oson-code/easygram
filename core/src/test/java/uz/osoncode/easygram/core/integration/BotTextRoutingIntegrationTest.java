package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.bind.annotation.BotTextDefault;
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
 * Integration tests for {@code @BotText} routing.
 */
class BotTextRoutingIntegrationTest {

    private static final AtomicBoolean HELLO_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean BYE_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean DEFAULT_CALLED = new AtomicBoolean(false);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(TextController.class);

    @Test
    void helloText_routesToHelloHandler() throws Exception {
        HELLO_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "hello");
            assertThat(HELLO_CALLED.get()).isTrue();
        });
    }

    @Test
    void byeText_routesToByeHandler() throws Exception {
        BYE_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "bye");
            assertThat(BYE_CALLED.get()).isTrue();
        });
    }

    @Test
    void unknownText_routesToDefaultHandler() throws Exception {
        DEFAULT_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "something else");
            assertThat(DEFAULT_CALLED.get()).isTrue();
        });
    }

    @Test
    void helloText_doesNotTriggerByeHandler() throws Exception {
        BYE_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "hello");
            assertThat(BYE_CALLED.get()).isFalse();
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
    public static class TextController {

        @BotText("hello")
        public void onHello() {
            HELLO_CALLED.set(true);
        }

        @BotText("bye")
        public void onBye() {
            BYE_CALLED.set(true);
        }

        @BotTextDefault
        public void onDefault() {
            DEFAULT_CALLED.set(true);
        }
    }
}
