package uz.osoncode.easygram.core.observability.autoconfigure;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.bot.Bot;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.observability.BotHealthIndicator;
import uz.osoncode.easygram.core.observability.BotInfoContributor;

/**
 * Spring Boot auto-configuration for Telegram bot Actuator integration.
 *
 * <p>Registers {@link BotHealthIndicator} and {@link BotInfoContributor} beans when
 * ALL of the following conditions are met:</p>
 * <ul>
 *   <li>{@link HealthIndicator} is present on the classpath
 *       ({@code org.springframework.boot:spring-boot-actuator}).</li>
 *   <li>A {@link Bot} bean and a {@link BotConfigurer} bean exist in the application context.</li>
 *   <li>No custom bean of the same type has already been declared.</li>
 * </ul>
 *
 * <p>Once active, the following Actuator endpoints are enriched:</p>
 * <ul>
 *   <li>{@code /actuator/health} — reports {@code UP} with bot {@code id}, {@code username},
 *       {@code firstName} and {@code transport} details when the bot has successfully
 *       authenticated with the Telegram Bot API.</li>
 *   <li>{@code /actuator/info} — adds a {@code telegram-bot} section with the same details.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnClass({HealthIndicator.class, InfoContributor.class})
@ConditionalOnBean({Bot.class, BotConfigurer.class})
@AutoConfigureAfter(name = "uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration")
public class BotActuatorAutoConfiguration {

    /** Creates a new {@code BotActuatorAutoConfiguration} instance. */
    public BotActuatorAutoConfiguration() {
    }

    /**
     * Registers a {@link BotHealthIndicator} that exposes bot status at
     * {@code /actuator/health}.
     *
     * @param bot           the bot instance to inspect
     * @param botConfigurer the configurer providing the active transport type
     * @return a configured {@link BotHealthIndicator}
     */
    @Bean
    @ConditionalOnMissingBean
    public BotHealthIndicator botHealthIndicator(Bot bot, BotConfigurer botConfigurer) {
        return new BotHealthIndicator(bot, botConfigurer);
    }

    /**
     * Registers a {@link BotInfoContributor} that exposes bot metadata at
     * {@code /actuator/info}.
     *
     * @param bot           the bot instance to inspect
     * @param botConfigurer the configurer providing the active transport type
     * @return a configured {@link BotInfoContributor}
     */
    @Bean
    @ConditionalOnMissingBean
    public BotInfoContributor botInfoContributor(Bot bot, BotConfigurer botConfigurer) {
        return new BotInfoContributor(bot, botConfigurer);
    }
}
