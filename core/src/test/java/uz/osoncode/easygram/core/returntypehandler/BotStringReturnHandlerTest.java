package uz.osoncode.easygram.core.returntypehandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import uz.osoncode.easygram.core.bind.annotation.BotParseMode;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BotStringReturnHandlerTest {

    private BotStringReturnHandler handler;
    private BotRequest request;
    private BotResponse response;

    @BeforeEach
    void setUp() {
        handler = new BotStringReturnHandler(Optional.empty());
        request = new BotRequest();
        response = new BotResponse();
    }

    @Test
    void supportsReturnType_stringMethod_returnsTrue() throws Exception {
        Method m = SampleController.class.getDeclaredMethod("stringReturn");
        assertThat(handler.supportsReturnType(m)).isTrue();
    }

    @Test
    void supportsReturnType_voidMethod_returnsFalse() throws Exception {
        Method m = SampleController.class.getDeclaredMethod("voidReturn");
        assertThat(handler.supportsReturnType(m)).isFalse();
    }

    @Test
    void supportsElement_string_returnsTrue() {
        assertThat(handler.supportsElement("hello")).isTrue();
    }

    @Test
    void supportsElement_nonString_returnsFalse() {
        assertThat(handler.supportsElement(42)).isFalse();
    }

    @Test
    void handleReturnType_withChat_addsSendMessage() {
        Chat chat = Chat.builder().id(100L).type("private").build();
        request.setChat(chat);
        handler.handleReturnType(request, response, "Hello!");
        assertThat(response.getBotApiMethods()).hasSize(1);
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getChatId()).isEqualTo("100");
        assertThat(msg.getText()).isEqualTo("Hello!");
    }

    @Test
    void handleReturnType_nullReturn_doesNothing() {
        Chat chat = Chat.builder().id(100L).type("private").build();
        request.setChat(chat);
        handler.handleReturnType(request, response, null);
        assertThat(response.getBotApiMethods()).isEmpty();
    }

    @Test
    void handleReturnType_noChat_doesNothing() {
        // chat is null — should log warning and skip
        handler.handleReturnType(request, response, "Hello!");
        assertThat(response.getBotApiMethods()).isEmpty();
    }

    @Test
    void handleReturnType_withBotParseMode_setsParseModeOnSendMessage() throws Exception {
        Chat chat = Chat.builder().id(100L).type("private").build();
        request.setChat(chat);
        Method m = SampleController.class.getDeclaredMethod("htmlReturn");
        handler.handleReturnType(request, response, "<b>bold</b>", m);
        assertThat(response.getBotApiMethods()).hasSize(1);
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getParseMode()).isEqualTo("HTML");
        assertThat(msg.getText()).isEqualTo("<b>bold</b>");
    }

    @Test
    void handleReturnType_withMarkdownV2ParseMode_setsParseModeOnSendMessage() throws Exception {
        Chat chat = Chat.builder().id(100L).type("private").build();
        request.setChat(chat);
        Method m = SampleController.class.getDeclaredMethod("markdownReturn");
        handler.handleReturnType(request, response, "*bold*", m);
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getParseMode()).isEqualTo("MarkdownV2");
    }

    @Test
    void handleReturnType_withoutBotParseMode_parseModeIsNull() throws Exception {
        Chat chat = Chat.builder().id(100L).type("private").build();
        request.setChat(chat);
        Method m = SampleController.class.getDeclaredMethod("stringReturn");
        handler.handleReturnType(request, response, "plain text", m);
        SendMessage msg = (SendMessage) response.getBotApiMethods().iterator().next();
        assertThat(msg.getParseMode()).isNull();
    }

    // ── sample controller for reflection ─────────────────────────────────────

    static class SampleController {
        public String stringReturn() { return "hi"; }
        public void voidReturn() {}

        @BotParseMode("HTML")
        public String htmlReturn() { return "<b>bold</b>"; }

        @BotParseMode("MarkdownV2")
        public String markdownReturn() { return "*bold*"; }
    }
}
