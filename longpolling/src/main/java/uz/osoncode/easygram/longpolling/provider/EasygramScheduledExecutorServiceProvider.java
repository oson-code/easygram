package uz.osoncode.easygram.longpolling.provider;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Provider for the {@link ScheduledExecutorService} used internally by the long-polling
 * {@link org.telegram.telegrambots.longpolling.BotSession} to schedule polling tasks.
 *
 * <p>Register a Spring bean of this type to control the thread count or naming for the
 * scheduler without touching any other bot configuration:</p>
 *
 * <pre>{@code
 * @Bean
 * public EasygramScheduledExecutorServiceProvider botScheduledExecutorServiceProvider() {
 *     ScheduledExecutorService scheduler =
 *             Executors.newScheduledThreadPool(2,
 *                     new ThreadFactoryBuilder().setNameFormat("bot-scheduler-%d").build());
 *     return () -> scheduler;
 * }
 * }</pre>
 *
 * <p><strong>Important:</strong> the framework stores the returned instance and shuts it down
 * when the application context is closed. Always return the same object on every call — never
 * create a new scheduler per invocation.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface EasygramScheduledExecutorServiceProvider {

    /**
     * Returns the {@link ScheduledExecutorService} to use for long-poll scheduling.
     *
     * @return a non-null, non-terminated {@link ScheduledExecutorService}
     */
    ScheduledExecutorService provide();
}
