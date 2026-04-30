package uz.osoncode.easygram.core.chatstate.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.chatstate.InMemoryBotChatStateService;

import java.util.Optional;

/**
 * Auto-configuration that registers the default in-memory chat state service.
 *
 * <p>Provides {@link InMemoryBotChatStateService} as the default {@link BotChatStateService}
 * unless a custom implementation is already present in the application context.
 * Replace by declaring your own {@link BotChatStateService} bean (e.g. Redis-backed).</p>
 *
 * <p>When a Micrometer {@link MeterRegistry} is present on the classpath, the service
 * will automatically record {@code easygram.chatstate.get}, {@code easygram.chatstate.set},
 * and {@code easygram.chatstate.clear} counters.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
public class ChatStateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BotChatStateService.class)
    public BotChatStateService inMemoryBotChatStateService(Optional<MeterRegistry> meterRegistry) {
        return new InMemoryBotChatStateService(meterRegistry);
    }
}
