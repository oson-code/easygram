package uz.osoncode.easygram.core.reply;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PlainTextTemplateTest {

    @Test
    void of_noArgs_setsTemplate() {
        PlainTextTemplate t = PlainTextTemplate.of("Hello!");
        assertThat(t.getTemplate()).isEqualTo("Hello!");
        assertThat(t.getArgs()).isEmpty();
    }

    @Test
    void of_nullTemplate_throwsNpe() {
        assertThatNullPointerException().isThrownBy(() -> PlainTextTemplate.of(null));
    }

    @Test
    void of_withArgs_setsArgs() {
        PlainTextTemplate t = PlainTextTemplate.of("Hello #{0}!", "World");
        assertThat(t.getArgs()).containsExactly("World");
    }

    @Test
    void withMarkup_setsMarkupId() {
        PlainTextTemplate t = PlainTextTemplate.of("Hello!").withMarkup("main_menu");
        assertThat(t.getMarkupId()).isEqualTo("main_menu");
        assertThat(t.getTemplate()).isEqualTo("Hello!");
    }

    @Test
    void withMarkup_withParams() {
        Map<String, Object> params = Map.of("page", 1);
        PlainTextTemplate t = PlainTextTemplate.of("Hi!").withMarkup("items", params);
        assertThat(t.getMarkupId()).isEqualTo("items");
        assertThat(t.getMarkupParams()).isEqualTo(params);
    }

    @Test
    void withKeyboard_setsKeyboard() {
        ReplyKeyboard kb = new ReplyKeyboard() {};
        PlainTextTemplate t = PlainTextTemplate.of("Hi!").withKeyboard(kb);
        assertThat(t.getKeyboard()).isSameAs(kb);
    }

    @Test
    void removeMarkup_setsFlag() {
        PlainTextTemplate t = PlainTextTemplate.of("Bye!").removeMarkup();
        assertThat(t.isRemoveMarkup()).isTrue();
    }

    @Test
    void withEditMessage_setsFlag() {
        PlainTextTemplate t = PlainTextTemplate.of("Edit!").withEditMessage();
        assertThat(t.isEditMessage()).isTrue();
    }

    @Test
    void asAnswerCallbackQuery_setsFlag() {
        PlainTextTemplate t = PlainTextTemplate.of("Answer!").asAnswerCallbackQuery();
        assertThat(t.isAnswerCallbackQuery()).isTrue();
        assertThat(t.isCallbackAlert()).isFalse();
    }

    @Test
    void withCallbackAlert_impliesAnswerCallbackQuery() {
        PlainTextTemplate t = PlainTextTemplate.of("Alert!").withCallbackAlert();
        assertThat(t.isCallbackAlert()).isTrue();
        assertThat(t.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void withCallbackUrl_impliesAnswerCallbackQuery() {
        PlainTextTemplate t = PlainTextTemplate.of("URL!").withCallbackUrl("https://t.me");
        assertThat(t.getCallbackUrl()).isEqualTo("https://t.me");
        assertThat(t.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void withCallbackCacheTime_impliesAnswerCallbackQuery() {
        PlainTextTemplate t = PlainTextTemplate.of("Cache!").withCallbackCacheTime(60);
        assertThat(t.getCallbackCacheTime()).isEqualTo(60);
        assertThat(t.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void builder_fullCreation() {
        ReplyKeyboard kb = new ReplyKeyboard() {};
        PlainTextTemplate t = PlainTextTemplate.builder()
                .template("Hello #{0}! You are #{1}.")
                .args("Alice", 30)
                .markupId("menu")
                .keyboard(kb)
                .editMessage(true)
                .callbackAlert(true)
                .callbackUrl("https://example.com")
                .callbackCacheTime(10)
                .build();

        assertThat(t.getTemplate()).isEqualTo("Hello #{0}! You are #{1}.");
        assertThat(t.getArgs()).containsExactly("Alice", 30);
        assertThat(t.getMarkupId()).isEqualTo("menu");
        assertThat(t.getKeyboard()).isSameAs(kb);
        assertThat(t.isEditMessage()).isTrue();
        assertThat(t.isCallbackAlert()).isTrue();
        assertThat(t.getCallbackUrl()).isEqualTo("https://example.com");
        assertThat(t.getCallbackCacheTime()).isEqualTo(10);
        assertThat(t.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void isImmutable_originalUnchangedAfterWithMarkup() {
        PlainTextTemplate original = PlainTextTemplate.of("Hi!");
        PlainTextTemplate modified = original.withMarkup("menu");
        assertThat(original.getMarkupId()).isNull();
        assertThat(modified.getMarkupId()).isEqualTo("menu");
    }

    @Test
    void isImmutable_originalUnchangedAfterRemoveMarkup() {
        PlainTextTemplate original = PlainTextTemplate.of("Hi!");
        PlainTextTemplate modified = original.removeMarkup();
        assertThat(original.isRemoveMarkup()).isFalse();
        assertThat(modified.isRemoveMarkup()).isTrue();
    }

    @Test
    void getArgs_returnsCopy() {
        PlainTextTemplate t = PlainTextTemplate.of("Hi #{0}!", "World");
        Object[] args1 = t.getArgs();
        Object[] args2 = t.getArgs();
        assertThat(args1).isNotSameAs(args2);
        assertThat(args1).containsExactly(args2);
    }

    @Test
    void builder_noArgs_getArgsIsNull() {
        PlainTextTemplate t = PlainTextTemplate.builder().template("Hi!").build();
        assertThat(t.getArgs()).isNull();
    }

    @Test
    void builder_removeMarkup_setsFlag() {
        PlainTextTemplate t = PlainTextTemplate.builder().template("Clean!").removeMarkup().build();
        assertThat(t.isRemoveMarkup()).isTrue();
    }

    @Test
    void builder_markupParams_setCorrectly() {
        Map<String, Object> params = Map.of("key", "val");
        PlainTextTemplate t = PlainTextTemplate.builder().template("x").markupParams(params).build();
        assertThat(t.getMarkupParams()).isEqualTo(params);
    }
}
