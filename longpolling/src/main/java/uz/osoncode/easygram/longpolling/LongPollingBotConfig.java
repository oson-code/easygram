package uz.osoncode.easygram.longpolling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import uz.osoncode.easygram.longpolling.provider.BotBackOffProvider;
import uz.osoncode.easygram.longpolling.provider.BotGetUpdatesGeneratorProvider;
import uz.osoncode.easygram.longpolling.provider.BotScheduledExecutorServiceProvider;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Spring configuration class that registers default long-polling-specific provider beans.
 *
 * <p>Imported by
 * {@link uz.osoncode.easygram.longpolling.autoconfigure.LongPollingAutoConfiguration}
 * via {@code @Import}. Consumer applications should not reference this class directly.</p>
 *
 * <p>Each bean is guarded by {@link ConditionalOnMissingBean} so that applications can replace
 * any individual provider without touching the others.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class LongPollingBotConfig {

    /**
     * Default {@link BotScheduledExecutorServiceProvider} backed by a single-threaded
     * scheduled executor.
     *
     * @return a {@link BotScheduledExecutorServiceProvider} backed by a single-threaded scheduled executor
     */
    @Bean
    @ConditionalOnMissingBean
    public BotScheduledExecutorServiceProvider botScheduledExecutorServiceProvider() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        return () -> scheduler;
    }

    /**
     * Default {@link BotBackOffProvider} using an exponential back-off strategy.
     *
     * @return a {@link BotBackOffProvider} that creates {@link ExponentialBackOff} instances
     */
    @Bean
    @ConditionalOnMissingBean
    public BotBackOffProvider botBackOffProvider() {
        return ExponentialBackOff::new;
    }

    /**
     * Default {@link BotGetUpdatesGeneratorProvider} requesting up to 100 updates per poll
     * with a 50-second long-poll timeout.
     *
     * @return a {@link BotGetUpdatesGeneratorProvider} that builds {@code GetUpdates} requests
     */
    @Bean
    @ConditionalOnMissingBean
    public BotGetUpdatesGeneratorProvider botGetUpdatesGeneratorProvider() {
        Function<Integer, GetUpdates> generator = offset ->
                GetUpdates.builder().offset(offset + 1).limit(100).timeout(50).build();
        return () -> generator;
    }
}
