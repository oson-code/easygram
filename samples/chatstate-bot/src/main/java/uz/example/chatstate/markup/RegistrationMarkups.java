package uz.example.chatstate.markup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.osoncode.easygram.core.annotation.BotConfiguration;
import uz.osoncode.easygram.core.annotation.BotMarkup;
import uz.osoncode.easygram.core.i18n.keyboard.BotKeyboardFactory;

/**
 * Configuration class for defining reply markups (keyboards) used in the registration flow.
 *
 * <p>This class uses {@link BotMarkup} to register reusable keyboards that can be referenced
 * by ID in {@link uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup} annotations.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@BotConfiguration
@RequiredArgsConstructor
public class RegistrationMarkups {

    private final BotKeyboardFactory botKeyboardFactory;

    @BotMarkup("kb_cancel")
    public ReplyKeyboard cancelKeyboard(User user) {
        log.info("user : {}", user.getUserName());
        return ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow("❌ Cancel"))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }
}
