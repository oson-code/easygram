package uz.osoncode.easygram.longpolling;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.BackOff;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import uz.osoncode.easygram.core.bot.Bot;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.provider.EasygramExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.EasygramObjectMapperProvider;
import uz.osoncode.easygram.core.provider.EasygramOkHttpClientProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramUrlProvider;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;
import uz.osoncode.easygram.longpolling.provider.EasygramBackOffProvider;
import uz.osoncode.easygram.longpolling.provider.EasygramGetUpdatesGeneratorProvider;
import uz.osoncode.easygram.longpolling.provider.EasygramScheduledExecutorServiceProvider;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Concrete Telegram bot implementation that uses the long-polling transport mechanism.
 *
 * <p>This class extends {@link Bot} and integrates with Telegram's long-polling infrastructure
 * via a {@link BotSession}. It implements {@link InitializingBean} and {@link DisposableBean}
 * to hook into the Spring bean lifecycle:</p>
 *
 * <ul>
 *   <li>On {@link #afterPropertiesSet()}: the parent {@link Bot#afterPropertiesSet()} is called
 *       first (which authenticates with Telegram and runs {@link BotStartTrigger}s), then a
 *       {@link BotSession} is created and started to begin receiving updates via long polling.</li>
 *   <li>On {@link #destroy()}: the active {@link BotSession} is stopped and both the main
 *       executor service and the scheduled executor service are shut down gracefully.</li>
 * </ul>
 *
 * <p>All infrastructure concerns (HTTP client, thread pools, back-off strategy, etc.) are
 * supplied via fine-grained provider beans, each of which can be overridden individually with
 * {@code @ConditionalOnMissingBean} in the application context.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class LongPollingBot extends Bot implements InitializingBean, DisposableBean {

    private final EasygramObjectMapperProvider objectMapperProvider;
    private final EasygramOkHttpClientProvider okHttpClientProvider;
    private final EasygramTelegramUrlProvider telegramUrlProvider;
    private final EasygramBackOffProvider backOffProvider;
    private final Function<Integer, GetUpdates> getUpdatesGenerator;
    private final EasygramProperties botProperties;
    private final ScheduledExecutorService scheduledExecutorService;

    private BotSession botSession;

    /**
     * Constructs a new {@code LongPollingBot} wiring all dependencies from fine-grained providers.
     *
     * @param botProperties                      common bot properties holding the token
     * @param triggers                           startup triggers executed once after authentication
     * @param filters                            filters applied to every incoming update
     * @param botDispatcher                      dispatcher that routes updates to handler methods
     * @param botExceptionHandlerRegistry        registry of exception handler methods
     * @param telegramClientProvider             provider for the outbound {@code TelegramClient}
     * @param executorServiceProvider            provider for the update-processing thread pool
     * @param objectMapperProvider               provider for the Jackson {@code ObjectMapper}
     * @param okHttpClientProvider               provider for the underlying {@code OkHttpClient}
     * @param telegramUrlProvider                provider for the Telegram API base URL
     * @param scheduledExecutorServiceProvider   provider for the polling scheduler
     * @param backOffProvider                    provider for the retry back-off strategy
     * @param getUpdatesGeneratorProvider        provider for the {@code GetUpdates} factory function
     */
    public LongPollingBot(
            EasygramProperties botProperties,
            List<BotStartTrigger> triggers,
            List<BotFilter> filters,
            BotDispatcher botDispatcher,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            EasygramTelegramClientProvider telegramClientProvider,
            EasygramExecutorServiceProvider executorServiceProvider,
            EasygramObjectMapperProvider objectMapperProvider,
            EasygramOkHttpClientProvider okHttpClientProvider,
            EasygramTelegramUrlProvider telegramUrlProvider,
            EasygramScheduledExecutorServiceProvider scheduledExecutorServiceProvider,
            EasygramBackOffProvider backOffProvider,
            EasygramGetUpdatesGeneratorProvider getUpdatesGeneratorProvider) {
        super(
                botProperties.token(),
                triggers,
                botDispatcher,
                filters,
                executorServiceProvider.provide(),
                telegramClientProvider.provide(botProperties.token()),
                botExceptionHandlerRegistry
        );
        this.botProperties = botProperties;
        this.objectMapperProvider = objectMapperProvider;
        this.okHttpClientProvider = okHttpClientProvider;
        this.telegramUrlProvider = telegramUrlProvider;
        this.backOffProvider = backOffProvider;
        this.getUpdatesGenerator = getUpdatesGeneratorProvider.provide();
        this.scheduledExecutorService = scheduledExecutorServiceProvider.provide();
    }

    /**
     * Initializes the bot after all Spring properties have been set.
     *
     * <p>Delegates to {@link Bot#afterPropertiesSet()} to authenticate with Telegram and run
     * startup triggers, then creates and starts a {@link BotSession} to begin long polling.</p>
     */
    @Override
    @SneakyThrows
    public void afterPropertiesSet() {
        super.afterPropertiesSet();

        botSession = new BotSession(
                objectMapperProvider.provide(),
                okHttpClientProvider.provide(),
                scheduledExecutorService,
                botProperties.token(),
                telegramUrlProvider::provide,
                getUpdatesGenerator,
                backOffProvider::provide,
                super::handleUpdates
        );
        botSession.start();
        log.info("Telegram long polling bot session started");
    }

    /**
     * Shuts down the bot and releases all associated resources.
     *
     * <p>Stops the active {@link BotSession}, then shuts down the main executor service and the
     * scheduled executor service.</p>
     */
    @Override
    public void destroy() {
        if (botSession != null) {
            botSession.stop();
        }
        executorService.shutdown();
        scheduledExecutorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Telegram long polling bot stopped");
    }
}
