package uz.osoncode.easygram.longpolling.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.provider.BotExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.BotObjectMapperProvider;
import uz.osoncode.easygram.core.provider.BotOkHttpClientProvider;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.core.provider.BotTelegramUrlProvider;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;
import uz.osoncode.easygram.longpolling.LongPollingBot;
import uz.osoncode.easygram.longpolling.LongPollingBotConfig;
import uz.osoncode.easygram.longpolling.provider.BotBackOffProvider;
import uz.osoncode.easygram.longpolling.provider.BotGetUpdatesGeneratorProvider;
import uz.osoncode.easygram.longpolling.provider.BotScheduledExecutorServiceProvider;

import java.util.List;

/**
 * Spring Boot auto-configuration class for the long-polling transport module.
 *
 * <p>This class is processed automatically by Spring Boot's auto-configuration mechanism.
 * It imports {@link LongPollingBotConfig} to ensure the long-polling-specific provider beans
 * ({@link BotScheduledExecutorServiceProvider}, {@link BotBackOffProvider},
 * {@link BotGetUpdatesGeneratorProvider}) are always present in the application context.</p>
 *
 * <p>Common infrastructure providers ({@link BotTelegramClientProvider},
 * {@link BotExecutorServiceProvider}, etc.) are registered by
 * {@link uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration}.</p>
 *
 * <p>The {@link LongPollingBot} bean is guarded by {@link ConditionalOnMissingBean} so
 * applications can supply a customised subclass if needed.</p>
 *
 * <p>This auto-configuration activates when:</p>
 * <ul>
 *   <li>{@code easygram.update.transport} is {@code LONG_POLLING} or absent (default), AND</li>
 *   <li>{@code easygram.messaging.type} is NOT {@code CONSUMER} — the second condition prevents
 *       long-polling from starting when the bot receives updates from a broker instead.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "easygram.update", name = "transport", havingValue = "LONG_POLLING", matchIfMissing = true)
@ConditionalOnProperty(prefix = "easygram.messaging", name = "type", havingValue = "PRODUCER", matchIfMissing = true)
@Import(LongPollingBotConfig.class)
public class LongPollingAutoConfiguration {

    /**
     * Registers the primary {@link LongPollingBot} bean wired from fine-grained provider beans.
     *
     * @param botProperties                    common bot properties containing the token
     * @param triggers                         startup triggers executed once after authentication
     * @param filters                          filters applied to every incoming update
     * @param botDispatcher                    dispatcher that routes updates to handler methods
     * @param botExceptionHandlerRegistry      registry of exception handler methods
     * @param telegramClientProvider           provider for the outbound {@code TelegramClient}
     * @param executorServiceProvider          provider for the update-processing thread pool
     * @param objectMapperProvider             provider for the Jackson {@code ObjectMapper}
     * @param okHttpClientProvider             provider for the underlying {@code OkHttpClient}
     * @param telegramUrlProvider              provider for the Telegram API base URL
     * @param scheduledExecutorServiceProvider provider for the long-poll scheduler
     * @param backOffProvider                  provider for the retry back-off strategy
     * @param getUpdatesGeneratorProvider      provider for the {@code GetUpdates} factory function
     * @return a fully configured {@link LongPollingBot} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public LongPollingBot longPollingBot(
            EasygramProperties botProperties,
            List<BotStartTrigger> triggers,
            List<BotFilter> filters,
            BotDispatcher botDispatcher,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            BotTelegramClientProvider telegramClientProvider,
            BotExecutorServiceProvider executorServiceProvider,
            BotObjectMapperProvider objectMapperProvider,
            BotOkHttpClientProvider okHttpClientProvider,
            BotTelegramUrlProvider telegramUrlProvider,
            BotScheduledExecutorServiceProvider scheduledExecutorServiceProvider,
            BotBackOffProvider backOffProvider,
            BotGetUpdatesGeneratorProvider getUpdatesGeneratorProvider) {
        return new LongPollingBot(
                botProperties,
                triggers,
                filters,
                botDispatcher,
                botExceptionHandlerRegistry,
                telegramClientProvider,
                executorServiceProvider,
                objectMapperProvider,
                okHttpClientProvider,
                telegramUrlProvider,
                scheduledExecutorServiceProvider,
                backOffProvider,
                getUpdatesGeneratorProvider);
    }
}
