package uz.example.i18n.markup;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import uz.osoncode.easygram.core.annotation.BotConfiguration;
import uz.osoncode.easygram.core.annotation.BotMarkup;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.i18n.keyboard.BotKeyboardFactory;
import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Locale-aware keyboard definitions for the registration wizard.
 *
 * <p>All button labels are resolved through {@link BotKeyboardFactory} using the
 * {@link BotRequest}'s locale, so users with different Telegram language settings
 * automatically see button text in their own language.</p>
 *
 * <h2>State-bound keyboards</h2>
 * <p>Each {@code @BotMarkup} method is also annotated with {@code @BotChatState},
 * which binds the keyboard to one or more chat states. When a handler method forwards
 * the chat into one of those states and does <em>not</em> declare an explicit
 * {@code @BotReplyMarkup}, the framework auto-attaches the bound keyboard — no
 * annotation is needed on the controller side.</p>
 *
 * <h3>Registered keyboards and their state bindings</h3>
 * <ul>
 *   <li>{@code kb_cancel} — single-button row with a localised "Cancel" button;
 *       auto-attached when entering {@code AWAITING_NAME} or {@code AWAITING_CITY}.</li>
 *   <li>{@code kb_send_phone} — single-button row with a "Share phone" request button;
 *       auto-attached when entering or staying in {@code AWAITING_PHONE}.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@BotConfiguration
@RequiredArgsConstructor
public class RegistrationMarkups {

    private final BotKeyboardFactory keyboardFactory;

    /**
     * Returns a reply keyboard with a single localised "Cancel" button.
     *
     * <p>Annotated with {@code @BotChatState({"AWAITING_NAME", "AWAITING_CITY"})} so it is
     * automatically attached to any handler that forwards into those states, without needing
     * an explicit {@code @BotReplyMarkup("kb_cancel")} annotation on each handler method.</p>
     *
     * <p>The button label is resolved from the message bundle key {@code btn.cancel},
     * so it automatically adapts to the user's Telegram language.</p>
     *
     * @param request the current bot request; used to determine the user's locale
     * @return a one-row {@link ReplyKeyboard} with the localised cancel button
     */
    @BotMarkup("kb_cancel")
    @BotChatState({"AWAITING_NAME", "AWAITING_CITY"})
    public ReplyKeyboard cancelKeyboard(BotRequest request) {
        return keyboardFactory.reply(request)
                .row("btn.cancel")
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }

    /**
     * Returns a reply keyboard with a single localised "Share phone number" button.
     *
     * <p>Annotated with {@code @BotChatState("AWAITING_PHONE")} so it is automatically
     * attached to any handler that enters or stays in the {@code AWAITING_PHONE} state —
     * both when the phone step starts and when the user enters an invalid number.</p>
     *
     * <p>The button label is resolved from the message bundle key {@code btn.send.phone}:
     * <ul>
     *   <li>English: 📱 Share phone number</li>
     *   <li>Uzbek:   📱 Telefon raqamni ulashing</li>
     *   <li>Russian: 📱 Поделиться номером</li>
     * </ul>
     *
     * @param botRequest the current bot request; used to determine the user's locale
     * @return a one-row {@link ReplyKeyboard} with the localised phone-sharing button
     */
    @BotMarkup("kb_send_phone")
    @BotChatState("AWAITING_PHONE")
    public ReplyKeyboard phone(BotRequest botRequest) {
        KeyboardButton keyboardButton = keyboardFactory.replyButton("btn.send.phone", botRequest);
        keyboardButton.setRequestContact(true);
        return keyboardFactory.reply(botRequest)
                .row(keyboardButton)
                .build();
    }
}
