package uz.osoncode.easygram.core.observability;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.bot.Bot;
import uz.osoncode.easygram.core.bot.BotConfigurer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Spring Boot Actuator {@link InfoContributor} that adds Telegram bot metadata to
 * the {@code /actuator/info} endpoint under the {@code telegram-bot} key.
 *
 * <p>Contributed fields (when bot metadata is available):</p>
 * <ul>
 *   <li>{@code id} — the bot's Telegram user ID.</li>
 *   <li>{@code username} — the bot's {@literal @}username.</li>
 *   <li>{@code firstName} — the bot's display name.</li>
 *   <li>{@code transport} — the active transport type (e.g. {@code LONG_POLLING}).</li>
 * </ul>
 *
 * <p>Nothing is contributed if the bot has not yet finished initializing.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class BotInfoContributor implements InfoContributor {

    /** The bot instance whose metadata is contributed to the info endpoint. */
    private final Bot bot;

    /** Provides the active transport type contributed to the info endpoint. */
    private final BotConfigurer botConfigurer;

    /**
     * Creates a new {@code BotInfoContributor}.
     *
     * @param bot           the bot instance to inspect
     * @param botConfigurer the configurer carrying the active transport type
     */
    public BotInfoContributor(Bot bot, BotConfigurer botConfigurer) {
        this.bot = bot;
        this.botConfigurer = botConfigurer;
    }

    /**
     * Contributes Telegram bot metadata to the info endpoint.
     *
     * <p>The {@code telegram-bot} key is only added when bot metadata is available
     * (i.e. after {@code afterPropertiesSet()} has completed successfully).</p>
     *
     * @param builder the info builder to contribute details to
     */
    @Override
    public void contribute(Info.Builder builder) {
        User metadata = bot.getBotMetaData();
        if (Objects.isNull(metadata)) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("id", metadata.getId());
        details.put("username", metadata.getUserName());
        details.put("firstName", metadata.getFirstName());
        details.put("transport", botConfigurer.transportType().name());
        builder.withDetail("telegram-bot", details);
    }
}
