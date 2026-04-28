package uz.osoncode.easygram.core.returntypehandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.reply.PlainReply;
import uz.osoncode.easygram.core.returntypehandler.action.AnswerCallbackQueryReplyAction;
import uz.osoncode.easygram.core.returntypehandler.action.EditMessageReplyAction;
import uz.osoncode.easygram.core.returntypehandler.action.SendMessageReplyAction;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BotPlainReplyReturnTypeHandlerTest {

    private BotPlainReplyReturnTypeHandler handler;
    private BotRequest request;
    private BotResponse response;

    @BeforeEach
    void setUp() {
        BotReplyActionChain chain = new BotReplyActionChain(List.of(
                new SendMessageReplyAction(Optional.empty()),
                new EditMessageReplyAction(Optional.empty()),
                new AnswerCallbackQueryReplyAction()
        ));
        handler = new BotPlainReplyReturnTypeHandler(chain);
        request = new BotRequest();
        request.setUpdate(new Update());
        response = new BotResponse();
    }

    @Test
    void supportsReturnType_plainReplyMethod_returnsTrue() throws Exception {
        Method m = SampleController.class.getDeclaredMethod("plainReplyReturn");
        assertThat(handler.supportsReturnType(m)).isTrue();
    }

    @Test
    void supportsReturnType_stringMethod_returnsFalse() throws Exception {
        Method m = SampleController.class.getDeclaredMethod("stringReturn");
        assertThat(handler.supportsReturnType(m)).isFalse();
    }

    @Test
    void supportsElement_plainReply_returnsTrue() {
        assertThat(handler.supportsElement(PlainReply.of("hello"))).isTrue();
    }

    @Test
    void supportsElement_string_returnsFalse() {
        assertThat(handler.supportsElement("hello")).isFalse();
    }

    @Test
    void handleReturnType_plainReplyWithChat_addsSendMessage() {
        Chat chat = Chat.builder().id(42L).type("private").build();
        request.setChat(chat);
        handler.handleReturnType(request, response, PlainReply.of("Hello!"));
        assertThat(response.getBotApiMethods()).isNotEmpty();
        SendMessage msg = response.getBotApiMethods().stream()
                .filter(m -> m instanceof SendMessage)
                .map(m -> (SendMessage) m)
                .findFirst()
                .orElseThrow();
        assertThat(msg.getText()).isEqualTo("Hello!");
        assertThat(msg.getChatId()).isEqualTo("42");
    }

    @Test
    void handleReturnType_nullReturn_doesNothing() {
        Chat chat = Chat.builder().id(42L).type("private").build();
        request.setChat(chat);
        handler.handleReturnType(request, response, null);
        assertThat(response.getBotApiMethods()).isEmpty();
    }

    // ── sample controller for reflection ─────────────────────────────────────

    static class SampleController {
        public PlainReply plainReplyReturn() { return PlainReply.of("hi"); }
        public String stringReturn() { return "hi"; }
    }
}

