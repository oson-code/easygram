package uz.osoncode.easygram.messaging.rabbit.consumer;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import uz.osoncode.easygram.core.bot.Bot;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.provider.EasygramExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;
import uz.osoncode.easygram.messaging.rabbit.EasygramRabbitProperties;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Telegram bot implementation that uses a RabbitMQ queue as its update transport.
 *
 * <p>Instead of polling Telegram or exposing an HTTP endpoint, this bot receives
 * {@link org.telegram.telegrambots.meta.api.objects.Update} objects that have been
 * deserialized from a RabbitMQ queue by {@link RabbitBotUpdateListener}. Replies are still
 * sent back to Telegram directly via the configured {@code TelegramClient}.</p>
 *
 * <p>This class extends {@link Bot} and implements {@link InitializingBean} and
 * {@link DisposableBean} to hook into the Spring bean lifecycle:</p>
 * <ul>
 *   <li>On {@link #afterPropertiesSet()}: authenticates with Telegram and runs
 *       {@link BotStartTrigger}s (same as other transports).</li>
 *   <li>On {@link #destroy()}: shuts down the executor service gracefully.</li>
 * </ul>
 *
 * <p>All infrastructure concerns are supplied via fine-grained provider beans, each of which
 * can be overridden individually with {@code @ConditionalOnMissingBean}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class RabbitConsumerBot extends Bot implements InitializingBean, DisposableBean {

    /**
     * Constructs a new {@code RabbitConsumerBot} wired from fine-grained provider beans.
     *
     * @param botProperties               common bot properties containing the token
     * @param rabbitProperties            properties holding the queue and broker settings
     * @param triggers                    startup triggers executed once after authentication
     * @param filters                     filters applied to every incoming update
     * @param botDispatcher               dispatcher that routes updates to handler methods
     * @param botExceptionHandlerRegistry registry of exception handler methods
     * @param telegramClientProvider      provider for the outbound {@code TelegramClient}
     * @param executorServiceProvider     provider for the update-processing thread pool
     */
    public RabbitConsumerBot(
            EasygramProperties botProperties,
            EasygramRabbitProperties rabbitProperties,
            List<BotStartTrigger> triggers,
            List<BotFilter> filters,
            BotDispatcher botDispatcher,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            EasygramTelegramClientProvider telegramClientProvider,
            EasygramExecutorServiceProvider executorServiceProvider) {
        super(
                botProperties.token(),
                triggers,
                botDispatcher,
                filters,
                executorServiceProvider.provide(),
                telegramClientProvider.provide(botProperties.token()),
                botExceptionHandlerRegistry
        );
    }

    /**
     * Authenticates with Telegram and runs startup triggers.
     */
    @Override
    @SneakyThrows
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        log.info("Telegram RabbitMQ consumer bot started");
    }

    /**
     * Shuts down the executor service.
     */
    @Override
    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Telegram RabbitMQ consumer bot stopped");
    }
}
