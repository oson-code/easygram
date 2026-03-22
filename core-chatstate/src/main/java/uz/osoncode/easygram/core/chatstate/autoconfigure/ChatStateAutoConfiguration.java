package uz.osoncode.easygram.core.chatstate.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.chatstate.InMemoryBotChatStateService;

/**
 * Auto-configuration that registers the default in-memory chat state service.
 *
 * <p>Provides {@link InMemoryBotChatStateService} as the default {@link BotChatStateService}
 * unless a custom implementation is already present in the application context.
 * Replace by declaring your own {@link BotChatStateService} bean (e.g. Redis-backed).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
public class ChatStateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BotChatStateService.class)
    public BotChatStateService inMemoryBotChatStateService() {
        return new InMemoryBotChatStateService();
    }
}
