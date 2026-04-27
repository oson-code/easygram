package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotReplyButton;
import uz.osoncode.easygram.core.bind.annotation.BotText;
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
 * Integration tests for reply button routing via {@code @BotReplyButton}.
 */
class BotReplyButtonRoutingIntegrationTest {

    private static final AtomicBoolean YES_CALLED = new AtomicBoolean(false);
    private static final AtomicBoolean NO_CALLED = new AtomicBoolean(false);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(ReplyButtonController.class);

    @Test
    void yesButton_routesToYesHandler() throws Exception {
        YES_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "Yes");
            assertThat(YES_CALLED.get()).isTrue();
        });
    }

    @Test
    void noButton_routesToNoHandler() throws Exception {
        NO_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "No");
            assertThat(NO_CALLED.get()).isTrue();
        });
    }

    @Test
    void yesButton_doesNotTriggerNoHandler() throws Exception {
        NO_CALLED.set(false);
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            dispatch(context, "Yes");
            assertThat(NO_CALLED.get()).isFalse();
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
    public static class ReplyButtonController {

        @BotReplyButton("Yes")
        public void onYes() {
            YES_CALLED.set(true);
        }

        @BotReplyButton("No")
        public void onNo() {
            NO_CALLED.set(true);
        }
    }
}
