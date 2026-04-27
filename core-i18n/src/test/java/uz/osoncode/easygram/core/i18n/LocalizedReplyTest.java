package uz.osoncode.easygram.core.i18n;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LocalizedReplyTest {

    @Test
    void of_setsKeyAndDefaults() {
        LocalizedReply reply = LocalizedReply.of("welcome");
        assertThat(reply.getKey()).isEqualTo("welcome");
        assertThat(reply.getArgs()).isEmpty();
        assertThat(reply.getMarkupId()).isNull();
        assertThat(reply.getKeyboard()).isNull();
        assertThat(reply.isRemoveMarkup()).isFalse();
        assertThat(reply.isEditMessage()).isFalse();
    }

    @Test
    void of_withArgs_setsArgs() {
        LocalizedReply reply = LocalizedReply.of("greeting", "Alice");
        assertThat(reply.getArgs()).containsExactly("Alice");
    }

    @Test
    void withMarkup_setsMarkupId() {
        LocalizedReply reply = LocalizedReply.of("hello").withMarkup("main_menu");
        assertThat(reply.getMarkupId()).isEqualTo("main_menu");
        assertThat(reply.getKey()).isEqualTo("hello");
    }

    @Test
    void withMarkup_withParams() {
        Map<String, Object> params = Map.of("page", 1);
        LocalizedReply reply = LocalizedReply.of("msg").withMarkup("items", params);
        assertThat(reply.getMarkupId()).isEqualTo("items");
        assertThat(reply.getMarkupParams()).isEqualTo(params);
    }

    @Test
    void withKeyboard_setsKeyboard() {
        ReplyKeyboard kb = new ReplyKeyboard() {};
        LocalizedReply reply = LocalizedReply.of("hi").withKeyboard(kb);
        assertThat(reply.getKeyboard()).isSameAs(kb);
    }

    @Test
    void removeMarkup_setsFlag() {
        LocalizedReply reply = LocalizedReply.of("bye").removeMarkup();
        assertThat(reply.isRemoveMarkup()).isTrue();
    }

    @Test
    void withEditMessage_setsFlag() {
        LocalizedReply reply = LocalizedReply.of("edit").withEditMessage();
        assertThat(reply.isEditMessage()).isTrue();
    }

    @Test
    void builder_fullCreation() {
        ReplyKeyboard kb = new ReplyKeyboard() {};
        LocalizedReply reply = LocalizedReply.builder()
                .key("greet")
                .args("Bob", 25)
                .markupId("menu")
                .keyboard(kb)
                .editMessage(true)
                .callbackAlert(true)
                .callbackUrl("https://t.me")
                .callbackCacheTime(30)
                .build();

        assertThat(reply.getKey()).isEqualTo("greet");
        assertThat(reply.getArgs()).containsExactly("Bob", 25);
        assertThat(reply.getMarkupId()).isEqualTo("menu");
        assertThat(reply.getKeyboard()).isSameAs(kb);
        assertThat(reply.isEditMessage()).isTrue();
        assertThat(reply.isCallbackAlert()).isTrue();
        assertThat(reply.getCallbackUrl()).isEqualTo("https://t.me");
        assertThat(reply.getCallbackCacheTime()).isEqualTo(30);
        assertThat(reply.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void withMarkup_isImmutable() {
        LocalizedReply original = LocalizedReply.of("hello");
        LocalizedReply modified = original.withMarkup("other_menu");
        assertThat(original.getMarkupId()).isNull();
        assertThat(modified.getMarkupId()).isEqualTo("other_menu");
    }
}
