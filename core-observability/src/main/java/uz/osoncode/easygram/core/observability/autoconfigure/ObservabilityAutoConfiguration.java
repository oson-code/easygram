package uz.osoncode.easygram.core.observability.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.observability.BotObservabilityFilter;

/**
 * Spring Boot auto-configuration for the {@code core-observability} module.
 *
 * <p>Registers a {@link BotObservabilityFilter} bean when ALL of the following conditions
 * are met:</p>
 * <ul>
 *   <li>{@link ObservationRegistry} is present on the classpath
 *       ({@code io.micrometer:micrometer-observation}).</li>
 *   <li>An {@link ObservationRegistry} bean exists in the application context
 *       (i.e. Spring Boot Actuator / Micrometer is configured).</li>
 *   <li>No custom {@link BotObservabilityFilter} bean has already been declared.</li>
 * </ul>
 *
 * <p>The resulting filter automatically produces:</p>
 * <ul>
 *   <li>A {@code easygram.update} timer metric (count, duration, error rate) with
 *       {@code update.type} and {@code transport.type} tags.</li>
 *   <li>A distributed tracing span per update when a Brave or OpenTelemetry bridge is
 *       also on the classpath — no additional configuration required.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnClass(ObservationRegistry.class)
@ConditionalOnBean(ObservationRegistry.class)
@AutoConfigureAfter(name = "uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration")
public class ObservabilityAutoConfiguration {

    /** Creates a new {@code ObservabilityAutoConfiguration} instance. */
    public ObservabilityAutoConfiguration() {
    }

    /**
     * Registers the {@link BotObservabilityFilter} that instruments every Telegram update.
     *
     * @param observationRegistry the Micrometer registry to record observations into
     * @param botConfigurer       provides the active transport type for the {@code transport.type} tag
     * @return a configured {@link BotObservabilityFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotObservabilityFilter botObservabilityFilter(
            ObservationRegistry observationRegistry,
            BotConfigurer botConfigurer) {
        return new BotObservabilityFilter(observationRegistry, botConfigurer);
    }
}
