package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.PlainReply;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for return-type handlers (String, PlainReply).
 *
 * <p>The tests verify that return values are added to {@link BotResponse} without making
 * real Telegram API calls — the {@code BotApiMethodsSenderFilter} is intentionally excluded
 * so the queue can be inspected directly.</p>
 */
class BotReturnTypeHandlerIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(ReturnTypeController.class);

    @Test
    void stringReturn_addsSendMessageToResponse() throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = BotCommandRoutingIntegrationTest.buildMessageUpdate("greet", 42L);
            BotRequest request = new BotRequest();
            request.setUpdate(update);

            List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
            BotDispatcher dispatcher = context.getBean(BotDispatcher.class);
            BotExceptionHandlerRegistry exReg = context.getBean(BotExceptionHandlerRegistry.class);
            new DefaultBotFilterChain(filters, dispatcher, exReg).doFilter(request, response);
        });

        assertThat(response.getBotApiMethods()).hasSize(1);
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getText()).isEqualTo("Hello!");
        assertThat(msg.getChatId()).isEqualTo("42");
    }

    @Test
    void plainReplyReturn_addsSendMessageToResponse() throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = BotCommandRoutingIntegrationTest.buildMessageUpdate("reply-obj", 42L);
            BotRequest request = new BotRequest();
            request.setUpdate(update);

            List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
            BotDispatcher dispatcher = context.getBean(BotDispatcher.class);
            BotExceptionHandlerRegistry exReg = context.getBean(BotExceptionHandlerRegistry.class);
            new DefaultBotFilterChain(filters, dispatcher, exReg).doFilter(request, response);
        });

        assertThat(response.getBotApiMethods()).hasSize(1);
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getText()).isEqualTo("PlainReply text");
    }

    @Test
    void voidReturn_addsNothingToResponse() throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = BotCommandRoutingIntegrationTest.buildMessageUpdate("void-handler", 42L);
            BotRequest request = new BotRequest();
            request.setUpdate(update);

            List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
            BotDispatcher dispatcher = context.getBean(BotDispatcher.class);
            BotExceptionHandlerRegistry exReg = context.getBean(BotExceptionHandlerRegistry.class);
            new DefaultBotFilterChain(filters, dispatcher, exReg).doFilter(request, response);
        });

        assertThat(response.getBotApiMethods()).isEmpty();
    }

    // ── test controller ───────────────────────────────────────────────────────

    @BotController
    public static class ReturnTypeController {

        @BotText("greet")
        public String onGreet() {
            return "Hello!";
        }

        @BotText("reply-obj")
        public PlainReply onReplyObj() {
            return PlainReply.of("PlainReply text");
        }

        @BotText("void-handler")
        public void onVoid() {
            // no return value
        }
    }
}
