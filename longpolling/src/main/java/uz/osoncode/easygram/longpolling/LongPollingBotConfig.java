package uz.osoncode.easygram.longpolling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import uz.osoncode.easygram.longpolling.provider.EasygramBackOffProvider;
import uz.osoncode.easygram.longpolling.provider.EasygramGetUpdatesGeneratorProvider;
import uz.osoncode.easygram.longpolling.provider.EasygramScheduledExecutorServiceProvider;

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
     * Default {@link EasygramScheduledExecutorServiceProvider} backed by a single-threaded
     * scheduled executor.
     *
     * @return a {@link EasygramScheduledExecutorServiceProvider} backed by a single-threaded scheduled executor
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramScheduledExecutorServiceProvider botScheduledExecutorServiceProvider() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        return () -> scheduler;
    }

    /**
     * Default {@link EasygramBackOffProvider} using an exponential back-off strategy.
     *
     * @return a {@link EasygramBackOffProvider} that creates {@link ExponentialBackOff} instances
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramBackOffProvider botBackOffProvider() {
        return ExponentialBackOff::new;
    }

    /**
     * Default {@link EasygramGetUpdatesGeneratorProvider} driven by
     * {@link EasygramLongPollingProperties}.
     *
     * <p>The limit and timeout values default to {@code 100} and {@code 50}
     * respectively and can be overridden via:
     * <pre>{@code
     * easygram:
     *   update:
     *     long-polling:
     *       limit: 100
     *       timeout-seconds: 50
     * }</pre>
     *
     * @param props long-polling tuning properties
     * @return a {@link EasygramGetUpdatesGeneratorProvider} that builds {@code GetUpdates} requests
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramGetUpdatesGeneratorProvider botGetUpdatesGeneratorProvider(
            EasygramLongPollingProperties props) {
        int limit = props.limit();
        int timeout = props.timeoutSeconds();
        Function<Integer, GetUpdates> generator = offset ->
                GetUpdates.builder().offset(offset + 1).limit(limit).timeout(timeout).build();
        return () -> generator;
    }
}

