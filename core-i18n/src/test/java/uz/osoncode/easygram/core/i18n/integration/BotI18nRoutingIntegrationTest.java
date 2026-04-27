package uz.osoncode.easygram.core.i18n.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.i18n.LocalizedReply;
import uz.osoncode.easygram.core.i18n.autoconfigure.BotI18nAutoConfiguration;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for i18n routing — {@link LocalizedReply} return-type handling.
 */
class BotI18nRoutingIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(
                    "easygram.token=test-token",
                    "easygram.i18n.enabled=true",
                    "easygram.i18n.default-locale=en"
            )
            .withUserConfiguration(FakeMessageSourceConfig.class, I18nController.class)
            .withConfiguration(AutoConfigurations.of(
                    BotI18nAutoConfiguration.class,
                    CoreAutoConfiguration.class
            ));

    @Test
    void localizedReplyReturn_resolvesKeyToMessage() throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = buildUpdate("greet", "en", 42L);
            BotRequest request = new BotRequest();
            request.setUpdate(update);
            List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
            BotDispatcher dispatcher = context.getBean(BotDispatcher.class);
            BotExceptionHandlerRegistry exReg = context.getBean(BotExceptionHandlerRegistry.class);
            new DefaultBotFilterChain(filters, dispatcher, exReg).doFilter(request, response);
        });

        assertThat(response.getBotApiMethods()).isNotEmpty();
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getText()).isEqualTo("Hello!");
    }

    @Test
    void localizedReplyReturn_usesUserLanguage() throws Exception {
        BotResponse response = new BotResponse();
        runner.run(context -> {
            context.getBean(BotHandlerLoader.class).run(null);
            Update update = buildUpdate("greet", "uz", 42L);
            BotRequest request = new BotRequest();
            request.setUpdate(update);
            List<BotFilter> filters = new ArrayList<>(context.getBeansOfType(BotFilter.class).values());
            BotDispatcher dispatcher = context.getBean(BotDispatcher.class);
            BotExceptionHandlerRegistry exReg = context.getBean(BotExceptionHandlerRegistry.class);
            new DefaultBotFilterChain(filters, dispatcher, exReg).doFilter(request, response);
        });

        assertThat(response.getBotApiMethods()).isNotEmpty();
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getText()).isEqualTo("Salom!");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Update buildUpdate(String text, String langCode, long chatId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(chatId);
        when(user.getLanguageCode()).thenReturn(langCode);

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

    // ── test config ───────────────────────────────────────────────────────────

    @Configuration
    public static class FakeMessageSourceConfig {

        @Bean
        StaticMessageSource messageSource() {
            StaticMessageSource source = new StaticMessageSource();
            source.addMessage("greeting", Locale.ENGLISH, "Hello!");
            source.addMessage("greeting", new Locale("uz"), "Salom!");
            return source;
        }
    }

    @BotController
    public static class I18nController {

        @BotText("greet")
        public LocalizedReply onGreet() {
            return LocalizedReply.of("greeting");
        }
    }
}
