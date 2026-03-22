package uz.osoncode.easygram.core.i18n.keyboard;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Factory for building localised Telegram keyboard markup objects.
 *
 * <p>All button texts are resolved through {@link BotMessageSource} using the locale
 * determined from the incoming {@link BotRequest} (or an explicitly supplied
 * {@link Locale}), so the same handler code produces the correct language for every user.</p>
 *
 * <h2>Inline keyboard (fluent builder)</h2>
 * <pre>{@code
 * InlineKeyboardMarkup keyboard = keyboardFactory.inline(request)
 *     .row("btn.yes", "cb_yes",  "btn.no",  "cb_no")   // (textCode, callbackData) pairs
 *     .row("btn.cancel", "cb_cancel")
 *     .build();
 *
 * return SendMessage.builder()
 *     .chatId(chatId)
 *     .text(messages.getMessage("choose.option", request))
 *     .replyMarkup(keyboard)
 *     .build();
 * }</pre>
 *
 * <h2>Reply keyboard (fluent builder)</h2>
 * <pre>{@code
 * ReplyKeyboardMarkup keyboard = keyboardFactory.reply(request)
 *     .row("btn.profile", "btn.settings")   // text codes per row
 *     .row("btn.help")
 *     .resizeKeyboard(true)
 *     .build();
 * }</pre>
 *
 * <h2>Single-button helpers</h2>
 * <pre>{@code
 * InlineKeyboardButton btn = keyboardFactory.inlineButton("btn.settings", "cb_settings", request);
 * KeyboardButton        kb  = keyboardFactory.replyButton("btn.profile", request);
 * }</pre>
 *
 * <p>Message bundle example ({@code messages/bot_uz.properties}):</p>
 * <pre>
 * btn.yes     = Ha
 * btn.no      = Yo'q
 * btn.cancel  = Bekor qilish
 * btn.profile = Profil
 * btn.settings= Sozlamalar
 * btn.help    = Yordam
 * </pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotKeyboardFactory {

    private final BotMessageSource messageSource;

    // -------------------------------------------------------------------------
    // Inline keyboard — direct helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a single {@link InlineKeyboardButton} whose label is resolved from
     * the message bundle using the locale of the current bot request.
     *
     * @param textCode     message key for the button label
     * @param callbackData callback data sent when the button is pressed
     * @param request      current bot request (used to determine locale)
     * @return a fully-built {@link InlineKeyboardButton}
     */
    public InlineKeyboardButton inlineButton(String textCode, String callbackData, BotRequest request) {
        return InlineKeyboardButton.builder()
                .text(messageSource.getMessage(textCode, request))
                .callbackData(callbackData)
                .build();
    }

    /**
     * Creates a single {@link InlineKeyboardButton} using an explicit locale.
     *
     * @param textCode     message key for the button label
     * @param callbackData callback data sent when the button is pressed
     * @param locale       target locale
     * @return a fully-built {@link InlineKeyboardButton}
     */
    public InlineKeyboardButton inlineButton(String textCode, String callbackData, Locale locale) {
        return InlineKeyboardButton.builder()
                .text(messageSource.getMessage(textCode, locale))
                .callbackData(callbackData)
                .build();
    }

    /**
     * Creates an {@link InlineKeyboardRow} from pre-built buttons.
     *
     * @param buttons one or more buttons to place in the row
     * @return a new {@link InlineKeyboardRow}
     */
    public InlineKeyboardRow inlineRow(InlineKeyboardButton... buttons) {
        return new InlineKeyboardRow(List.of(buttons));
    }

    /**
     * Creates an {@link InlineKeyboardMarkup} from pre-built rows.
     *
     * @param rows the rows to include in the markup
     * @return a fully-built {@link InlineKeyboardMarkup}
     */
    public InlineKeyboardMarkup inlineMarkup(InlineKeyboardRow... rows) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
        for (InlineKeyboardRow row : rows) {
            builder.keyboardRow(row);
        }
        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Inline keyboard — fluent builder
    // -------------------------------------------------------------------------

    /**
     * Returns a fluent {@link InlineKeyboardBuilder} that resolves button labels
     * using the locale from the given bot request.
     *
     * @param request current bot request
     * @return a new builder instance
     */
    public InlineKeyboardBuilder inline(BotRequest request) {
        return new InlineKeyboardBuilder(request, null);
    }

    /**
     * Returns a fluent {@link InlineKeyboardBuilder} that resolves button labels
     * using the supplied locale.
     *
     * @param locale target locale
     * @return a new builder instance
     */
    public InlineKeyboardBuilder inline(Locale locale) {
        return new InlineKeyboardBuilder(null, locale);
    }

    // -------------------------------------------------------------------------
    // Reply keyboard — direct helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a single {@link KeyboardButton} whose label is resolved from
     * the message bundle using the locale of the current bot request.
     *
     * @param textCode message key for the button label
     * @param request  current bot request (used to determine locale)
     * @return a fully-built {@link KeyboardButton}
     */
    public KeyboardButton replyButton(String textCode, BotRequest request) {
        return KeyboardButton.builder()
                .text(messageSource.getMessage(textCode, request))
                .build();
    }

    /**
     * Creates a single {@link KeyboardButton} using an explicit locale.
     *
     * @param textCode message key for the button label
     * @param locale   target locale
     * @return a fully-built {@link KeyboardButton}
     */
    public KeyboardButton replyButton(String textCode, Locale locale) {
        return KeyboardButton.builder()
                .text(messageSource.getMessage(textCode, locale))
                .build();
    }

    // -------------------------------------------------------------------------
    // Reply keyboard — fluent builder
    // -------------------------------------------------------------------------

    /**
     * Returns a fluent {@link ReplyKeyboardBuilder} that resolves button labels
     * using the locale from the given bot request.
     *
     * @param request current bot request
     * @return a new builder instance
     */
    public ReplyKeyboardBuilder reply(BotRequest request) {
        return new ReplyKeyboardBuilder(request, null);
    }

    /**
     * Returns a fluent {@link ReplyKeyboardBuilder} that resolves button labels
     * using the supplied locale.
     *
     * @param locale target locale
     * @return a new builder instance
     */
    public ReplyKeyboardBuilder reply(Locale locale) {
        return new ReplyKeyboardBuilder(null, locale);
    }

    // =========================================================================
    // Inner builder: InlineKeyboardBuilder
    // =========================================================================

    /**
     * Fluent builder for {@link InlineKeyboardMarkup} with localised button labels.
     *
     * <p>Each {@code row()} call accepts alternating {@code (textCode, callbackData)} pairs:</p>
     * <pre>{@code
     * factory.inline(request)
     *     .row("btn.yes", "cb_yes",  "btn.no", "cb_no")
     *     .row("btn.cancel", "cb_cancel")
     *     .build();
     * }</pre>
     */
    public class InlineKeyboardBuilder {

        private final BotRequest request;
        private final Locale locale;
        private final List<InlineKeyboardRow> rows = new ArrayList<>();

        private InlineKeyboardBuilder(BotRequest request, Locale locale) {
            this.request = request;
            this.locale = locale;
        }

        /**
         * Adds a row of inline buttons.
         *
         * @param textAndCallbackPairs alternating {@code (messageCode, callbackData)} pairs;
         *                             must have an even number of elements
         * @return this builder
         * @throws IllegalArgumentException if the number of arguments is odd
         */
        public InlineKeyboardBuilder row(String... textAndCallbackPairs) {
            if (textAndCallbackPairs.length % 2 != 0) {
                throw new IllegalArgumentException(
                        "Arguments must be in (textCode, callbackData) pairs but got " +
                        textAndCallbackPairs.length + " elements");
            }
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (int i = 0; i < textAndCallbackPairs.length; i += 2) {
                String text = locale != null
                        ? messageSource.getMessage(textAndCallbackPairs[i], locale)
                        : messageSource.getMessage(textAndCallbackPairs[i], request);
                buttons.add(InlineKeyboardButton.builder()
                        .text(text)
                        .callbackData(textAndCallbackPairs[i + 1])
                        .build());
            }
            rows.add(new InlineKeyboardRow(buttons));
            return this;
        }

        /**
         * Adds a row containing pre-built {@link InlineKeyboardButton} objects.
         *
         * @param buttons pre-built buttons
         * @return this builder
         */
        public InlineKeyboardBuilder row(InlineKeyboardButton... buttons) {
            rows.add(new InlineKeyboardRow(List.of(buttons)));
            return this;
        }

        /**
         * Builds the {@link InlineKeyboardMarkup}.
         *
         * @return the constructed markup
         */
        public InlineKeyboardMarkup build() {
            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
            rows.forEach(builder::keyboardRow);
            return builder.build();
        }
    }

    // =========================================================================
    // Inner builder: ReplyKeyboardBuilder
    // =========================================================================

    /**
     * Fluent builder for {@link ReplyKeyboardMarkup} with localised button labels.
     *
     * <p>Each {@code row()} call accepts message keys for the buttons in that row:</p>
     * <pre>{@code
     * factory.reply(request)
     *     .row("btn.profile", "btn.settings")
     *     .row("btn.help")
     *     .resizeKeyboard(true)
     *     .oneTimeKeyboard(false)
     *     .build();
     * }</pre>
     */
    public class ReplyKeyboardBuilder {

        private final BotRequest request;
        private final Locale locale;
        private final List<KeyboardRow> rows = new ArrayList<>();
        private boolean resizeKeyboard = true;
        private boolean oneTimeKeyboard = false;

        private ReplyKeyboardBuilder(BotRequest request, Locale locale) {
            this.request = request;
            this.locale = locale;
        }

        /**
         * Adds a row of reply keyboard buttons.
         *
         * @param textCodes message keys for each button in the row
         * @return this builder
         */
        public ReplyKeyboardBuilder row(String... textCodes) {
            KeyboardRow keyboardRow = new KeyboardRow();
            for (String textCode : textCodes) {
                String text = locale != null
                        ? messageSource.getMessage(textCode, locale)
                        : messageSource.getMessage(textCode, request);
                keyboardRow.add(KeyboardButton.builder().text(text).build());
            }
            rows.add(keyboardRow);
            return this;
        }

        /**
         * Adds a row containing pre-built {@link KeyboardButton} objects.
         *
         * @param buttons pre-built buttons
         * @return this builder
         */
        public ReplyKeyboardBuilder row(KeyboardButton... buttons) {
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.addAll(List.of(buttons));
            rows.add(keyboardRow);
            return this;
        }

        /**
         * Requests the keyboard to be shown at reduced size. Defaults to {@code true}.
         *
         * @param resize whether to resize
         * @return this builder
         */
        public ReplyKeyboardBuilder resizeKeyboard(boolean resize) {
            this.resizeKeyboard = resize;
            return this;
        }

        /**
         * Requests the keyboard to hide after a single use. Defaults to {@code false}.
         *
         * @param oneTime whether to hide after use
         * @return this builder
         */
        public ReplyKeyboardBuilder oneTimeKeyboard(boolean oneTime) {
            this.oneTimeKeyboard = oneTime;
            return this;
        }

        /**
         * Builds the {@link ReplyKeyboardMarkup}.
         *
         * @return the constructed markup
         */
        public ReplyKeyboardMarkup build() {
            return ReplyKeyboardMarkup.builder()
                    .keyboard(rows)
                    .resizeKeyboard(resizeKeyboard)
                    .oneTimeKeyboard(oneTimeKeyboard)
                    .build();
        }
    }
}
