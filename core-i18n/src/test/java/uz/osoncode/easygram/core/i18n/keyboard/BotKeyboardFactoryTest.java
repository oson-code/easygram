package uz.osoncode.easygram.core.i18n.keyboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackQueryService;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BotKeyboardFactory}.
 */
class BotKeyboardFactoryTest {

    private BotMessageSource messageSource;
    private BotKeyboardFactory factory;
    private BotKeyboardFactory factoryWithDynamic;
    private BotRequest request;

    /**
     * Simple in-memory store to capture what gets saved via {@link BotDynamicCallbackQueryService#store}.
     */
    private final Map<String, BotDynamicCallbackData> storedPayloads = new HashMap<>();
    private final BotDynamicCallbackQueryService dynamicService = new BotDynamicCallbackQueryService() {
        @Override
        public BotDynamicCallbackData resolve(String key) {
            return storedPayloads.get(key);
        }

        @Override
        public void store(String key, BotDynamicCallbackData payload) {
            storedPayloads.put(key, payload);
        }

        @Override
        public void remove(String key) {
            storedPayloads.remove(key);
        }
    };

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getLanguageCode()).thenReturn("en");

        request = new BotRequest();
        request.setUser(user);

        messageSource = mock(BotMessageSource.class);
        when(messageSource.getMessage("btn.yes", request)).thenReturn("Yes");
        when(messageSource.getMessage("btn.no", request)).thenReturn("No");
        when(messageSource.getMessage("btn.cancel", request)).thenReturn("Cancel");
        when(messageSource.getMessage("btn.yes", Locale.ENGLISH)).thenReturn("Yes");
        when(messageSource.getMessage("btn.no", Locale.ENGLISH)).thenReturn("No");
        when(messageSource.getMessage("btn.dyn", request)).thenReturn("Dynamic");
        when(messageSource.getMessage("btn.dyn", Locale.ENGLISH)).thenReturn("Dynamic");

        factory = new BotKeyboardFactory(messageSource, null);
        factoryWithDynamic = new BotKeyboardFactory(messageSource, dynamicService);
    }

    // -------------------------------------------------------------------------
    // inlineButton — direct helpers
    // -------------------------------------------------------------------------

    @Test
    void inlineButton_withRequest_resolvesTextAndSetsCallbackData() {
        InlineKeyboardButton btn = factory.inlineButton("btn.yes", "cb_yes", request);

        assertThat(btn.getText()).isEqualTo("Yes");
        assertThat(btn.getCallbackData()).isEqualTo("cb_yes");
    }

    @Test
    void inlineButton_withLocale_resolvesTextAndSetsCallbackData() {
        InlineKeyboardButton btn = factory.inlineButton("btn.yes", "cb_yes", Locale.ENGLISH);

        assertThat(btn.getText()).isEqualTo("Yes");
        assertThat(btn.getCallbackData()).isEqualTo("cb_yes");
    }

    // -------------------------------------------------------------------------
    // replyButton — direct helpers
    // -------------------------------------------------------------------------

    @Test
    void replyButton_withRequest_resolvesText() {
        KeyboardButton btn = factory.replyButton("btn.yes", request);

        assertThat(btn.getText()).isEqualTo("Yes");
    }

    @Test
    void replyButton_withLocale_resolvesText() {
        KeyboardButton btn = factory.replyButton("btn.yes", Locale.ENGLISH);

        assertThat(btn.getText()).isEqualTo("Yes");
    }

    // -------------------------------------------------------------------------
    // inlineRow / inlineMarkup — assembly helpers
    // -------------------------------------------------------------------------

    @Test
    void inlineRow_wrapsButtonsIntoRow() {
        InlineKeyboardButton b1 = factory.inlineButton("btn.yes", "cb_yes", request);
        InlineKeyboardButton b2 = factory.inlineButton("btn.no", "cb_no", request);

        InlineKeyboardRow row = factory.inlineRow(b1, b2);

        assertThat(row).hasSize(2);
        assertThat(row.get(0).getText()).isEqualTo("Yes");
        assertThat(row.get(1).getText()).isEqualTo("No");
    }

    @Test
    void inlineMarkup_assemblesRowsIntoMarkup() {
        InlineKeyboardRow row1 = factory.inlineRow(factory.inlineButton("btn.yes", "y", request));
        InlineKeyboardRow row2 = factory.inlineRow(factory.inlineButton("btn.no", "n", request));

        InlineKeyboardMarkup markup = factory.inlineMarkup(row1, row2);

        assertThat(markup.getKeyboard()).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // InlineKeyboardBuilder — fluent builder
    // -------------------------------------------------------------------------

    @Test
    void inlineBuilder_withRequest_rowCodes_usesCodeAsBothTextAndCallbackData() {
        InlineKeyboardMarkup markup = factory.inline(request)
                .row("btn.yes", "btn.no")
                .row("btn.cancel")
                .build();

        assertThat(markup.getKeyboard()).hasSize(2);
        InlineKeyboardRow firstRow = markup.getKeyboard().get(0);
        assertThat(firstRow.get(0).getText()).isEqualTo("Yes");
        assertThat(firstRow.get(0).getCallbackData()).isEqualTo("btn.yes");
        assertThat(firstRow.get(1).getText()).isEqualTo("No");
        assertThat(firstRow.get(1).getCallbackData()).isEqualTo("btn.no");
    }

    @Test
    void inlineBuilder_withLocale_rowCodes_resolvesViaLocale() {
        InlineKeyboardMarkup markup = factory.inline(Locale.ENGLISH)
                .row("btn.yes", "btn.no")
                .build();

        InlineKeyboardRow row = markup.getKeyboard().get(0);
        assertThat(row.get(0).getText()).isEqualTo("Yes");
        assertThat(row.get(1).getText()).isEqualTo("No");
    }

    @Test
    void inlineBuilder_rowWithPrebuiltButtons_passesThrough() {
        InlineKeyboardButton prebuilt = InlineKeyboardButton.builder()
                .text("Custom")
                .callbackData("custom_cb")
                .build();

        InlineKeyboardMarkup markup = factory.inline(request)
                .row(prebuilt)
                .build();

        assertThat(markup.getKeyboard()).hasSize(1);
        assertThat(markup.getKeyboard().get(0).get(0).getText()).isEqualTo("Custom");
    }

    // -------------------------------------------------------------------------
    // ReplyKeyboardBuilder — fluent builder
    // -------------------------------------------------------------------------

    @Test
    void replyBuilder_withRequest_rowCodes_resolvesButtonTexts() {
        ReplyKeyboardMarkup markup = factory.reply(request)
                .row("btn.yes", "btn.no")
                .row("btn.cancel")
                .build();

        assertThat(markup.getKeyboard()).hasSize(2);
        assertThat(markup.getKeyboard().get(0).get(0).getText()).isEqualTo("Yes");
        assertThat(markup.getKeyboard().get(0).get(1).getText()).isEqualTo("No");
    }

    @Test
    void replyBuilder_withLocale_rowCodes_resolvesViaLocale() {
        ReplyKeyboardMarkup markup = factory.reply(Locale.ENGLISH)
                .row("btn.yes", "btn.no")
                .build();

        assertThat(markup.getKeyboard().get(0).get(0).getText()).isEqualTo("Yes");
        assertThat(markup.getKeyboard().get(0).get(1).getText()).isEqualTo("No");
    }

    @Test
    void replyBuilder_rowWithPrebuiltButtons_passesThrough() {
        KeyboardButton prebuilt = KeyboardButton.builder().text("Prebuilt").build();

        ReplyKeyboardMarkup markup = factory.reply(request)
                .row(prebuilt)
                .build();

        assertThat(markup.getKeyboard()).hasSize(1);
        assertThat(markup.getKeyboard().get(0).get(0).getText()).isEqualTo("Prebuilt");
    }

    @Test
    void replyBuilder_resizeKeyboardFalse_propagatesToMarkup() {
        ReplyKeyboardMarkup markup = factory.reply(request)
                .row("btn.yes")
                .resizeKeyboard(false)
                .build();

        assertThat(markup.getResizeKeyboard()).isFalse();
    }

    @Test
    void replyBuilder_defaultResizeKeyboard_isTrue() {
        ReplyKeyboardMarkup markup = factory.reply(request)
                .row("btn.yes")
                .build();

        assertThat(markup.getResizeKeyboard()).isTrue();
    }

    @Test
    void replyBuilder_oneTimeKeyboardTrue_propagatesToMarkup() {
        ReplyKeyboardMarkup markup = factory.reply(request)
                .row("btn.yes")
                .oneTimeKeyboard(true)
                .build();

        assertThat(markup.getOneTimeKeyboard()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Dynamic inline buttons
    // -------------------------------------------------------------------------

    @Test
    void dynamicInlineButton_withoutService_throwsIllegalState() {
        BotDynamicCallbackData payload = BotDynamicCallbackData.builder().type("test").build();

        assertThatThrownBy(() -> factory.dynamicInlineButton("btn.dyn", payload, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BotDynamicCallbackQueryService");
    }

    @Test
    void dynamicInlineButton_withRequest_storesPayloadAndReturnsUuidKey() {
        BotDynamicCallbackData payload = BotDynamicCallbackData.builder().type("order").build();

        InlineKeyboardButton btn = factoryWithDynamic.dynamicInlineButton("btn.dyn", payload, request);

        assertThat(btn.getText()).isEqualTo("Dynamic");
        String key = btn.getCallbackData();
        assertThat(key).isNotEmpty();
        assertThat(storedPayloads).containsKey(key);
        assertThat(storedPayloads.get(key)).isSameAs(payload);
    }

    @Test
    void dynamicInlineButton_withLocale_storesPayloadAndReturnsUuidKey() {
        BotDynamicCallbackData payload = BotDynamicCallbackData.builder().type("order").build();

        InlineKeyboardButton btn = factoryWithDynamic.dynamicInlineButton("btn.dyn", payload, Locale.ENGLISH);

        assertThat(btn.getText()).isEqualTo("Dynamic");
        assertThat(storedPayloads).containsKey(btn.getCallbackData());
    }

    @Test
    void dynamicInlineButton_eachCallGeneratesUniqueKey() {
        BotDynamicCallbackData p1 = BotDynamicCallbackData.builder().type("t1").build();
        BotDynamicCallbackData p2 = BotDynamicCallbackData.builder().type("t2").build();

        InlineKeyboardButton btn1 = factoryWithDynamic.dynamicInlineButton("btn.dyn", p1, request);
        InlineKeyboardButton btn2 = factoryWithDynamic.dynamicInlineButton("btn.dyn", p2, request);

        assertThat(btn1.getCallbackData()).isNotEqualTo(btn2.getCallbackData());
    }

    // -------------------------------------------------------------------------
    // InlineKeyboardBuilder — dynamic rows
    // -------------------------------------------------------------------------

    @Test
    void inlineBuilder_dynamicRow_singleEntry_storesPayload() {
        BotDynamicCallbackData payload = BotDynamicCallbackData.builder().type("buy").build();

        InlineKeyboardMarkup markup = factoryWithDynamic.inline(request)
                .dynamicRow("btn.dyn", payload)
                .build();

        assertThat(markup.getKeyboard()).hasSize(1);
        InlineKeyboardButton btn = markup.getKeyboard().get(0).get(0);
        assertThat(btn.getText()).isEqualTo("Dynamic");
        assertThat(storedPayloads).containsKey(btn.getCallbackData());
    }

    @Test
    void inlineBuilder_dynamicRow_multipleEntries_eachButtonHasUniqueKey() {
        BotDynamicCallbackData p1 = BotDynamicCallbackData.builder().type("t1").build();
        BotDynamicCallbackData p2 = BotDynamicCallbackData.builder().type("t2").build();

        InlineKeyboardMarkup markup = factoryWithDynamic.inline(request)
                .dynamicRow(
                        BotKeyboardFactory.entry("btn.dyn", p1),
                        BotKeyboardFactory.entry("btn.dyn", p2))
                .build();

        InlineKeyboardRow row = markup.getKeyboard().get(0);
        assertThat(row).hasSize(2);
        assertThat(row.get(0).getCallbackData()).isNotEqualTo(row.get(1).getCallbackData());
        assertThat(storedPayloads).containsKeys(
                row.get(0).getCallbackData(),
                row.get(1).getCallbackData());
    }

    // -------------------------------------------------------------------------
    // DynamicButtonEntry factory method
    // -------------------------------------------------------------------------

    @Test
    void entry_createsEntryThatWorksEndToEnd() {
        BotDynamicCallbackData payload = BotDynamicCallbackData.builder().type("demo").build();

        BotKeyboardFactory.DynamicButtonEntry e = BotKeyboardFactory.entry("btn.dyn", payload);

        assertThat(e).isNotNull();
        InlineKeyboardMarkup markup = factoryWithDynamic.inline(request)
                .dynamicRow(e)
                .build();
        assertThat(markup.getKeyboard()).hasSize(1);
        assertThat(markup.getKeyboard().get(0)).hasSize(1);
    }
}
