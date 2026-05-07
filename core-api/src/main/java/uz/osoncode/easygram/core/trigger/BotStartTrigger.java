package uz.osoncode.easygram.core.trigger;

import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Callback interface invoked once after the bot has started and successfully authenticated
 * with the Telegram Bot API.
 * Implement this interface to perform post-startup initialization such as setting up
 * bot commands, sending notifications, or scheduling background tasks.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface BotStartTrigger {

    /**
     * Executes post-startup logic using the authenticated bot's metadata and API client.
     *
     * @param bot            the {@link User} object representing the authenticated bot; must not be {@code null}
     * @param telegramClient the client used to issue API requests to the Telegram Bot API; must not be {@code null}
     */
    void execute(User bot, TelegramClient telegramClient);
}
