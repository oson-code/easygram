package uz.osoncode.easygram.core.i18n.keyboard;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackQueryService;
import uz.osoncode.easygram.core.i18n.BotMessageSource;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Factory for building localised Telegram keyboard markup objects.
 *
 * <p>All button texts are resolved through {@link BotMessageSource} using the locale
 * determined from the incoming {@link BotRequest} (or an explicitly supplied
 * {@link Locale}), so the same handler code produces the correct language for every user.</p>
 *
 * <h2>Activation</h2>
 * <p>This bean is registered automatically by
 * {@link uz.osoncode.easygram.core.i18n.autoconfigure.BotI18nAutoConfiguration}.
 * You must set the following in your {@code application.yml}:</p>
 * <pre>{@code
 * easygram:
 *   i18n:
 *     enabled: true          # activates BotKeyboardFactory and all other i18n beans
 *
 * spring:
 *   messages:
 *     basename: messages/bot  # points to your message bundle files
 * }</pre>
 * <p>Without {@code easygram.i18n.enabled=true}, the entire {@code BotI18nAutoConfiguration}
 * class is skipped and no i18n beans — including {@code BotKeyboardFactory} — will be present
 * in the application context. Injecting this bean without the property set will cause a
 * {@code NoSuchBeanDefinitionException} at startup.</p>
 *
 * <h2>Inline keyboard (fluent builder)</h2>
 * <pre>{@code
 * InlineKeyboardMarkup keyboard = keyboardFactory.inline(request)
 *     .row("btn.yes", "btn.no")       // button codes: text resolved from bundle, same code as callbackData
 *     .row("btn.cancel")
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
public class BotKeyboardFactory {

    private final BotMessageSource messageSource;

    /**
     * Optional service for storing dynamic callback payloads. When present, enables
     * {@link #dynamicInlineButton} and {@link InlineKeyboardBuilder#dynamicRow}.
     */
    private final BotDynamicCallbackQueryService dynamicCallbackQueryService;

    /**
     * Constructs a factory with full dynamic-callback support.
     *
     * @param messageSource              the message source for resolving button labels; must not be {@code null}
     * @param dynamicCallbackQueryService the service for storing dynamic payloads; may be {@code null}
     *                                    if dynamic button methods are not used
     */
    public BotKeyboardFactory(BotMessageSource messageSource,
                              BotDynamicCallbackQueryService dynamicCallbackQueryService) {
        this.messageSource = messageSource;
        this.dynamicCallbackQueryService = dynamicCallbackQueryService;
    }

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

    // -------------------------------------------------------------------------
    // Inline keyboard — dynamic callback helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a single {@link InlineKeyboardButton} whose callback data is a generated UUID key
     * mapped to the supplied {@link BotDynamicCallbackData} payload via
     * {@link BotDynamicCallbackQueryService}.
     *
     * <p>A UUID key is generated, the payload is stored via the service, and the key is used
     * as the button's {@code callbackData}. When the user presses the button, the framework
     * resolves the key back to the payload and routes to the matching
     * {@link uz.osoncode.easygram.core.bind.annotation.BotDynamicCallbackQuery} handler.</p>
     *
     * @param textCode message key for the button label
     * @param payload  the dynamic callback payload to store; must not be {@code null}
     * @param request  current bot request (used to determine locale)
     * @return a fully-built {@link InlineKeyboardButton}
     * @throws IllegalStateException if no {@link BotDynamicCallbackQueryService} is configured
     * @since 0.0.4
     */
    public InlineKeyboardButton dynamicInlineButton(String textCode, BotDynamicCallbackData payload,
                                                    BotRequest request) {
        return buildDynamicButton(textCode, payload, messageSource.getMessage(textCode, request));
    }

    /**
     * Creates a single {@link InlineKeyboardButton} whose callback data is a generated UUID key
     * mapped to the supplied {@link BotDynamicCallbackData} payload via
     * {@link BotDynamicCallbackQueryService}.
     *
     * @param textCode message key for the button label
     * @param payload  the dynamic callback payload to store; must not be {@code null}
     * @param locale   target locale for the button label
     * @return a fully-built {@link InlineKeyboardButton}
     * @throws IllegalStateException if no {@link BotDynamicCallbackQueryService} is configured
     * @since 0.0.4
     */
    public InlineKeyboardButton dynamicInlineButton(String textCode, BotDynamicCallbackData payload,
                                                    Locale locale) {
        return buildDynamicButton(textCode, payload, messageSource.getMessage(textCode, locale));
    }

    private InlineKeyboardButton buildDynamicButton(String textCode, BotDynamicCallbackData payload,
                                                    String resolvedText) {
        if (dynamicCallbackQueryService == null) {
            throw new IllegalStateException(
                    "BotDynamicCallbackQueryService is not configured. "
                    + "Ensure a BotDynamicCallbackQueryService bean is present to use dynamicInlineButton().");
        }
        String key = UUID.randomUUID().toString();
        dynamicCallbackQueryService.store(key, payload);
        return InlineKeyboardButton.builder()
                .text(resolvedText)
                .callbackData(key)
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
     * <p>Each {@code row()} call accepts button codes. Each code is used both as the
     * message-bundle key for the button label and as the {@code callbackData} sent when
     * the button is pressed:</p>
     * <pre>{@code
     * factory.inline(request)
     *     .row("btn.yes", "btn.no")
     *     .row("btn.cancel")
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
         * <p>Each string is a button code: the button label is resolved from the message bundle
         * using that code, and the same code is used as the {@code callbackData} sent when the
         * button is pressed.</p>
         *
         * @param buttonCodes one or more message-bundle keys; each is also used as callback data
         * @return this builder
         */
        public InlineKeyboardBuilder row(String... buttonCodes) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (String code : buttonCodes) {
                String text = Objects.nonNull(locale)
                        ? messageSource.getMessage(code, locale)
                        : messageSource.getMessage(code, request);
                buttons.add(InlineKeyboardButton.builder()
                        .text(text)
                        .callbackData(code)
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
         * Adds a row of dynamic-callback buttons. Each entry is a pair of a message-bundle
         * key (for the label) and a {@link BotDynamicCallbackData} payload. A UUID key is
         * generated per button, the payload is stored via
         * {@link BotDynamicCallbackQueryService}, and the UUID is used as {@code callbackData}.
         *
         * @param textCode first button's message-bundle key
         * @param payload  first button's dynamic payload
         * @return this builder
         * @throws IllegalStateException if no {@link BotDynamicCallbackQueryService} is configured
         * @since 0.0.4
         */
        public InlineKeyboardBuilder dynamicRow(String textCode, BotDynamicCallbackData payload) {
            rows.add(new InlineKeyboardRow(List.of(
                    Objects.nonNull(locale)
                            ? dynamicInlineButton(textCode, payload, locale)
                            : dynamicInlineButton(textCode, payload, request)
            )));
            return this;
        }

        /**
         * Adds a row of multiple dynamic-callback buttons. Each entry must be a
         * {@link DynamicButtonEntry} created via {@link BotKeyboardFactory#entry}.
         *
         * @param entries button definitions; each wraps a text code and a {@link BotDynamicCallbackData}
         * @return this builder
         * @throws IllegalStateException if no {@link BotDynamicCallbackQueryService} is configured
         * @since 0.0.4
         */
        public InlineKeyboardBuilder dynamicRow(DynamicButtonEntry... entries) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            for (DynamicButtonEntry e : entries) {
                buttons.add(Objects.nonNull(locale)
                        ? dynamicInlineButton(e.textCode, e.payload, locale)
                        : dynamicInlineButton(e.textCode, e.payload, request));
            }
            rows.add(new InlineKeyboardRow(buttons));
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
                String text = Objects.nonNull(locale)
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

    // =========================================================================
    // Dynamic button entry helper
    // =========================================================================

    /**
     * Immutable pair of a message-bundle key and a {@link BotDynamicCallbackData} payload,
     * used with {@link InlineKeyboardBuilder#dynamicRow(DynamicButtonEntry...)}.
     *
     * @since 0.0.4
     */
    public static final class DynamicButtonEntry {
        final String textCode;
        final BotDynamicCallbackData payload;

        private DynamicButtonEntry(String textCode, BotDynamicCallbackData payload) {
            this.textCode = textCode;
            this.payload = payload;
        }
    }

    /**
     * Creates a {@link DynamicButtonEntry} for use with
     * {@link InlineKeyboardBuilder#dynamicRow(DynamicButtonEntry...)}.
     *
     * @param textCode message-bundle key for the button label
     * @param payload  the dynamic callback payload
     * @return a new entry
     * @since 0.0.4
     */
    public static DynamicButtonEntry entry(String textCode, BotDynamicCallbackData payload) {
        return new DynamicButtonEntry(textCode, payload);
    }
}
