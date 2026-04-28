package uz.osoncode.easygram.core.reply;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PlainReplyTest {

    @Test
    void of_setsTextAndDefaults() {
        PlainReply reply = PlainReply.of("Hello!");
        assertThat(reply.getText()).isEqualTo("Hello!");
        assertThat(reply.getMarkupId()).isNull();
        assertThat(reply.getKeyboard()).isNull();
        assertThat(reply.isRemoveMarkup()).isFalse();
        assertThat(reply.isEditMessage()).isFalse();
        assertThat(reply.isAnswerCallbackQuery()).isFalse();
        assertThat(reply.isCallbackAlert()).isFalse();
        assertThat(reply.getCallbackUrl()).isNull();
        assertThat(reply.getCallbackCacheTime()).isNull();
    }

    @Test
    void of_nullText_throwsNpe() {
        assertThatNullPointerException().isThrownBy(() -> PlainReply.of(null));
    }

    @Test
    void withMarkup_setsMarkupId() {
        PlainReply reply = PlainReply.of("Hello!").withMarkup("main_menu");
        assertThat(reply.getMarkupId()).isEqualTo("main_menu");
        assertThat(reply.getText()).isEqualTo("Hello!");
    }

    @Test
    void withMarkup_withParams_setsMarkupIdAndParams() {
        Map<String, Object> params = Map.of("page", 2);
        PlainReply reply = PlainReply.of("Hello!").withMarkup("item_list", params);
        assertThat(reply.getMarkupId()).isEqualTo("item_list");
        assertThat(reply.getMarkupParams()).isEqualTo(params);
    }

    @Test
    void withKeyboard_setsKeyboard() {
        ReplyKeyboard keyboard = new ReplyKeyboard() {};
        PlainReply reply = PlainReply.of("Hello!").withKeyboard(keyboard);
        assertThat(reply.getKeyboard()).isSameAs(keyboard);
        assertThat(reply.getMarkupId()).isNull();
    }

    @Test
    void removeMarkup_setsFlag() {
        PlainReply reply = PlainReply.of("Bye!").removeMarkup();
        assertThat(reply.isRemoveMarkup()).isTrue();
    }

    @Test
    void builder_fullCreation() {
        ReplyKeyboard keyboard = new ReplyKeyboard() {};
        PlainReply reply = PlainReply.builder()
                .text("Choose:")
                .markupId("main_menu")
                .keyboard(keyboard)
                .editMessage(true)
                .answerCallbackQuery(true)
                .callbackAlert(true)
                .callbackUrl("https://example.com")
                .callbackCacheTime(60)
                .build();

        assertThat(reply.getText()).isEqualTo("Choose:");
        assertThat(reply.getMarkupId()).isEqualTo("main_menu");
        assertThat(reply.getKeyboard()).isSameAs(keyboard);
        assertThat(reply.isEditMessage()).isTrue();
        assertThat(reply.isAnswerCallbackQuery()).isTrue();
        assertThat(reply.isCallbackAlert()).isTrue();
        assertThat(reply.getCallbackUrl()).isEqualTo("https://example.com");
        assertThat(reply.getCallbackCacheTime()).isEqualTo(60);
    }

    @Test
    void builder_callbackAlert_impliesAnswerCallbackQuery() {
        PlainReply reply = PlainReply.builder().text("Alert!").callbackAlert(true).build();
        assertThat(reply.isCallbackAlert()).isTrue();
        assertThat(reply.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void builder_callbackUrl_impliesAnswerCallbackQuery() {
        PlainReply reply = PlainReply.builder().text("Link!").callbackUrl("https://t.me").build();
        assertThat(reply.getCallbackUrl()).isEqualTo("https://t.me");
        assertThat(reply.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void builder_callbackCacheTime_impliesAnswerCallbackQuery() {
        PlainReply reply = PlainReply.builder().text("Cache!").callbackCacheTime(30).build();
        assertThat(reply.getCallbackCacheTime()).isEqualTo(30);
        assertThat(reply.isAnswerCallbackQuery()).isTrue();
    }

    @Test
    void withMarkup_isImmutable_originalUnchanged() {
        PlainReply original = PlainReply.of("Hello!");
        PlainReply modified = original.withMarkup("other_menu");
        assertThat(original.getMarkupId()).isNull();
        assertThat(modified.getMarkupId()).isEqualTo("other_menu");
    }

    @Test
    void withKeyboard_isImmutable_originalUnchanged() {
        PlainReply original = PlainReply.of("Hello!");
        ReplyKeyboard keyboard = new ReplyKeyboard() {};
        PlainReply modified = original.withKeyboard(keyboard);
        assertThat(original.getKeyboard()).isNull();
        assertThat(modified.getKeyboard()).isSameAs(keyboard);
    }

    @Test
    void removeMarkup_isImmutable_originalUnchanged() {
        PlainReply original = PlainReply.of("Hello!");
        PlainReply modified = original.removeMarkup();
        assertThat(original.isRemoveMarkup()).isFalse();
        assertThat(modified.isRemoveMarkup()).isTrue();
    }

    @Test
    void builder_removeMarkup_setsFlag() {
        PlainReply reply = PlainReply.builder().text("Clean!").removeMarkup().build();
        assertThat(reply.isRemoveMarkup()).isTrue();
    }

    @Test
    void builder_markupParams_setCorrectly() {
        Map<String, Object> params = Map.of("k", "v");
        PlainReply reply = PlainReply.builder().text("x").markupParams(params).build();
        assertThat(reply.getMarkupParams()).isEqualTo(params);
    }
}
