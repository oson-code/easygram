package uz.osoncode.easygram.core.chatstate.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uz.osoncode.easygram.core.chatstate.BotChatStateMetrics;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.chatstate.InMemoryBotChatStateService;
import uz.osoncode.easygram.core.chatstate.MicrometerBotChatStateMetrics;

/**
 * Auto-configuration that registers the default in-memory chat state service.
 *
 * <p>Provides {@link InMemoryBotChatStateService} as the default {@link BotChatStateService}
 * unless a custom implementation is already present in the application context.
 * Replace by declaring your own {@link BotChatStateService} bean (e.g. Redis-backed).</p>
 *
 * <p>When {@code micrometer-core} and a {@link MeterRegistry} bean are present on the
 * classpath, the service is automatically enhanced with
 * {@code easygram.chatstate.get}, {@code easygram.chatstate.set}, and
 * {@code easygram.chatstate.clear} counters via {@link MicrometerBotChatStateMetrics}.
 * When Micrometer is absent the service starts with no-op metrics — the application
 * context is never affected by the absence of {@code micrometer-core}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
public class ChatStateAutoConfiguration {

    /**
     * Registers an instrumented {@link InMemoryBotChatStateService} when both
     * {@code micrometer-core} is on the classpath and a {@link MeterRegistry} bean exists.
     *
     * <p>This inner configuration is completely skipped (not even loaded) when
     * {@code micrometer-core} is absent, which prevents any {@link ClassNotFoundException}
     * at application startup.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    static class WithMicrometerConfig {

        /**
         * Registers the Micrometer-instrumented chat-state service.
         *
         * @param meterRegistry the Micrometer registry to record counters against
         * @return an {@link InMemoryBotChatStateService} backed by {@link MicrometerBotChatStateMetrics}
         */
        @Bean
        @ConditionalOnMissingBean(BotChatStateService.class)
        @ConditionalOnBean(MeterRegistry.class)
        public BotChatStateService inMemoryBotChatStateService(MeterRegistry meterRegistry) {
            return new InMemoryBotChatStateService(new MicrometerBotChatStateMetrics(meterRegistry));
        }
    }

    /**
     * Registers a plain {@link InMemoryBotChatStateService} without any metrics
     * instrumentation. This bean is used when {@code micrometer-core} is absent from the
     * classpath or when no {@link MeterRegistry} bean is available.
     *
     * @return an {@link InMemoryBotChatStateService} with no-op metrics
     */
    @Bean
    @ConditionalOnMissingBean(BotChatStateService.class)
    public BotChatStateService inMemoryBotChatStateServiceNoMetrics() {
        return new InMemoryBotChatStateService();
    }
}

