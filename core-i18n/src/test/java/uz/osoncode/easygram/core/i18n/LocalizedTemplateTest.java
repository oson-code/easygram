package uz.osoncode.easygram.core.i18n;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizedTemplateTest {

    @Test
    void of_setsTemplateAndDefaults() {
        LocalizedTemplate t = LocalizedTemplate.of("greeting");
        assertThat(t.getTemplate()).isEqualTo("greeting");
        assertThat(t.getArgs()).isEmpty();
        assertThat(t.getMarkupId()).isNull();
        assertThat(t.getKeyboard()).isNull();
        assertThat(t.isRemoveMarkup()).isFalse();
        assertThat(t.isEditMessage()).isFalse();
    }

    @Test
    void of_withArgs_setsArgs() {
        LocalizedTemplate t = LocalizedTemplate.of("greeting", "Alice", 30);
        assertThat(t.getArgs()).containsExactly("Alice", 30);
    }

    @Test
    void withMarkup_setsMarkupId() {
        LocalizedTemplate t = LocalizedTemplate.of("hello").withMarkup("main_menu");
        assertThat(t.getMarkupId()).isEqualTo("main_menu");
    }

    @Test
    void withMarkup_withParams() {
        Map<String, Object> params = Map.of("p", 1);
        LocalizedTemplate t = LocalizedTemplate.of("x").withMarkup("items", params);
        assertThat(t.getMarkupParams()).isEqualTo(params);
    }

    @Test
    void withKeyboard_setsKeyboard() {
        ReplyKeyboard kb = new ReplyKeyboard() {};
        LocalizedTemplate t = LocalizedTemplate.of("hi").withKeyboard(kb);
        assertThat(t.getKeyboard()).isSameAs(kb);
    }

    @Test
    void removeMarkup_setsFlag() {
        LocalizedTemplate t = LocalizedTemplate.of("bye").removeMarkup();
        assertThat(t.isRemoveMarkup()).isTrue();
    }

    @Test
    void withEditMessage_setsFlag() {
        LocalizedTemplate t = LocalizedTemplate.of("edit").withEditMessage();
        assertThat(t.isEditMessage()).isTrue();
    }

    @Test
    void builder_fullCreation() {
        ReplyKeyboard kb = new ReplyKeyboard() {};
        LocalizedTemplate t = LocalizedTemplate.builder()
                .template("key")
                .args("x", "y")
                .markupId("menu")
                .keyboard(kb)
                .editMessage(true)
                .callbackAlert(true)
                .callbackCacheTime(10)
                .build();
        assertThat(t.getTemplate()).isEqualTo("key");
        assertThat(t.getArgs()).containsExactly("x", "y");
        assertThat(t.getMarkupId()).isEqualTo("menu");
        assertThat(t.getKeyboard()).isSameAs(kb);
        assertThat(t.isEditMessage()).isTrue();
        assertThat(t.isCallbackAlert()).isTrue();
        assertThat(t.getCallbackCacheTime()).isEqualTo(10);
        assertThat(t.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void isImmutable_withMarkup() {
        LocalizedTemplate original = LocalizedTemplate.of("hi");
        LocalizedTemplate modified = original.withMarkup("new_menu");
        assertThat(original.getMarkupId()).isNull();
        assertThat(modified.getMarkupId()).isEqualTo("new_menu");
    }
}
