package uz.osoncode.easygram.core.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.bot.Bot;
import uz.osoncode.easygram.core.bot.BotConfigurer;

/**
 * Spring Boot Actuator {@link HealthIndicator} for the Telegram bot.
 *
 * <p>Reports {@code UP} once the bot has successfully authenticated with the Telegram
 * Bot API (i.e. {@code GetMe} completed and bot metadata is available).
 * Reports {@code UNKNOWN} while the bot is still initializing or if metadata is
 * absent for any reason.</p>
 *
 * <p>Health details exposed at {@code /actuator/health}:</p>
 * <ul>
 *   <li>{@code id} — the bot's Telegram user ID.</li>
 *   <li>{@code username} — the bot's {@literal @}username.</li>
 *   <li>{@code firstName} — the bot's display name.</li>
 *   <li>{@code transport} — the active transport type (e.g. {@code LONG_POLLING}).</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotHealthIndicator implements HealthIndicator {

    /** The bot instance whose metadata is checked for health status. */
    private final Bot bot;

    /** Provides the active transport type shown in health details. */
    private final BotConfigurer botConfigurer;

    /**
     * Creates a new {@code BotHealthIndicator}.
     *
     * @param bot           the bot instance to inspect
     * @param botConfigurer the configurer carrying the active transport type
     */
    public BotHealthIndicator(Bot bot, BotConfigurer botConfigurer) {
        this.bot = bot;
        this.botConfigurer = botConfigurer;
    }

    /**
     * Returns the health of the Telegram bot.
     *
     * <p>{@code UP} when bot metadata is populated (successful {@code GetMe}).
     * {@code UNKNOWN} when metadata is not yet available.</p>
     *
     * @return the current {@link Health} status with bot details
     */
    @Override
    public Health health() {
        User metadata = bot.getBotMetaData();
        if (metadata == null) {
            return Health.unknown()
                    .withDetail("reason", "bot metadata not yet populated — still initializing")
                    .build();
        }
        return Health.up()
                .withDetail("id", metadata.getId())
                .withDetail("username", metadata.getUserName())
                .withDetail("firstName", metadata.getFirstName())
                .withDetail("transport", botConfigurer.transportType().name())
                .build();
    }
}
