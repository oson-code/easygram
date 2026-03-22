package uz.osoncode.easygram.webhook;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import uz.osoncode.easygram.core.bot.Bot;
import uz.osoncode.easygram.core.bot.BotProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.provider.BotExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Concrete Telegram bot implementation that uses the webhook transport mechanism.
 *
 * <p>This class extends {@link Bot} and integrates with Spring's lifecycle to register and
 * optionally deregister the Telegram webhook. It implements {@link InitializingBean} and
 * {@link DisposableBean}:</p>
 *
 * <ul>
 *   <li>On {@link #afterPropertiesSet()}: the parent {@link Bot#afterPropertiesSet()} is called
 *       first (which authenticates with Telegram and runs {@link BotStartTrigger}s), then a
 *       {@link SetWebhook} request is executed to register the configured public URL with Telegram.</li>
 *   <li>On {@link #destroy()}: the executor service is shut down, and if
 *       {@link WebhookBotProperties#unregisterOnShutdown()} is {@code true}, a
 *       {@link DeleteWebhook} request is sent to Telegram.</li>
 * </ul>
 *
 * <p>Incoming updates are delivered by {@link WebhookController} via {@link #handleUpdate}.</p>
 *
 * <p>All infrastructure concerns (HTTP client, thread pool) are supplied via fine-grained
 * provider beans, each of which can be overridden individually with
 * {@code @ConditionalOnMissingBean} in the application context.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class WebhookBot extends Bot implements InitializingBean, DisposableBean {

    private final WebhookBotProperties webhookBotProperties;
    private final ExecutorService executorService;

    /**
     * Constructs a new {@code WebhookBot} wired from fine-grained provider beans.
     *
     * @param botProperties               common bot properties containing the token
     * @param webhookBotProperties        properties holding the webhook URL and related settings
     * @param triggers                    startup triggers executed once after authentication
     * @param filters                     filters applied to every incoming update
     * @param botDispatcher               dispatcher that routes updates to handler methods
     * @param botExceptionHandlerRegistry registry of exception handler methods
     * @param telegramClientProvider      provider for the outbound {@code TelegramClient}
     * @param executorServiceProvider     provider for the update-processing thread pool
     */
    public WebhookBot(
            BotProperties botProperties,
            WebhookBotProperties webhookBotProperties,
            List<BotStartTrigger> triggers,
            List<BotFilter> filters,
            BotDispatcher botDispatcher,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            BotTelegramClientProvider telegramClientProvider,
            BotExecutorServiceProvider executorServiceProvider) {
        super(
                botProperties.token(),
                triggers,
                botDispatcher,
                filters,
                executorServiceProvider.provide(),
                telegramClientProvider.provide(botProperties.token()),
                botExceptionHandlerRegistry
        );
        this.webhookBotProperties = webhookBotProperties;
        this.executorService = executorServiceProvider.provide();
    }

    /**
     * Initializes the bot after all Spring properties have been set.
     *
     * <p>Delegates to {@link Bot#afterPropertiesSet()} to authenticate with Telegram and run
     * startup triggers, then registers the webhook URL with Telegram via {@link SetWebhook}.</p>
     */
    @Override
    @SneakyThrows
    public void afterPropertiesSet() {
        super.afterPropertiesSet();

        SetWebhook.SetWebhookBuilder builder = SetWebhook.builder()
                .url(webhookBotProperties.url())
                .dropPendingUpdates(webhookBotProperties.dropPendingUpdates());

        if (webhookBotProperties.secretToken() != null) {
            builder.secretToken(webhookBotProperties.secretToken());
        }

        if (webhookBotProperties.maxConnections() != null) {
            builder.maxConnections(webhookBotProperties.maxConnections());
        }

        Boolean result = telegramClient.execute(builder.build());
        log.info("Telegram webhook registered at {}. Result: {}", webhookBotProperties.url(), result);
    }

    /**
     * Shuts down the bot and releases all associated resources.
     *
     * <p>Shuts down the executor service, and, if
     * {@link WebhookBotProperties#unregisterOnShutdown()} is {@code true},
     * deletes the webhook from Telegram.</p>
     */
    @Override
    @SneakyThrows
    public void destroy() {
        if (Boolean.TRUE.equals(webhookBotProperties.unregisterOnShutdown())) {
            telegramClient.execute(DeleteWebhook.builder().build());
            log.info("Telegram webhook unregistered");
        }
        executorService.shutdown();
        log.info("Telegram webhook bot stopped");
    }
}
