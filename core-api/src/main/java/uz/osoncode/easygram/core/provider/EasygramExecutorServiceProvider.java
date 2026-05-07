package uz.osoncode.easygram.core.provider;

import java.util.concurrent.ExecutorService;

/**
 * Provider for the {@link ExecutorService} used to process incoming Telegram updates.
 *
 * <p>Register a Spring bean of this type to control the thread-pool size, thread naming, or
 * rejection policy without touching transport-specific configuration:</p>
 *
 * <pre>{@code
 * @Bean
 * public EasygramExecutorServiceProvider botExecutorServiceProvider() {
 *     ExecutorService executor = Executors.newFixedThreadPool(4,
 *             new ThreadFactoryBuilder().setNameFormat("bot-worker-%d").build());
 *     return () -> executor;
 * }
 * }</pre>
 *
 * <p><strong>Important:</strong> the framework calls {@link #provide()} once during bot
 * startup and stores the returned instance. The same instance is shut down gracefully when the
 * application context is closed. Always return the same {@link ExecutorService} object on
 * every invocation — do not create a new one per call.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface EasygramExecutorServiceProvider {

    /**
     * Returns the {@link ExecutorService} used to dispatch incoming Telegram updates.
     *
     * @return a non-null, non-terminated {@link ExecutorService}
     */
    ExecutorService provide();
}
