package uz.example.chatstate.markup;

import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.osoncode.easygram.core.annotation.BotConfiguration;
import uz.osoncode.easygram.core.annotation.BotMarkup;

/**
 * Configuration class for defining reply markups (keyboards) used in the registration flow.
 *
 * <p>This class uses {@link BotMarkup} to register reusable keyboards that can be referenced
 * by ID in {@link uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup} annotations.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@BotConfiguration
public class RegistrationMarkups {

    @BotMarkup("kb_cancel")
    public ReplyKeyboard cancelKeyboard(User user) {
        return ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow("❌ Cancel"))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }
}
