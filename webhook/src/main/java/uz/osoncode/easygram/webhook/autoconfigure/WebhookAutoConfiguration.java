package uz.osoncode.easygram.webhook.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.provider.EasygramExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.EasygramObjectMapperProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.core.trigger.BotStartTrigger;
import uz.osoncode.easygram.webhook.WebhookBot;
import uz.osoncode.easygram.webhook.EasygramWebhookProperties;
import uz.osoncode.easygram.webhook.WebhookController;

import java.util.List;

/**
 * Spring Boot auto-configuration class for the webhook transport module.
 *
 * <p>This class is processed automatically by Spring Boot's auto-configuration mechanism.
 * It activates {@link EasygramWebhookProperties} binding (prefix {@code easygram.webhook}) and
 * wires the webhook beans from fine-grained provider beans supplied by
 * {@link uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration}.</p>
 *
 * <p>The following beans are registered by this configuration:</p>
 * <ul>
 *   <li>{@link WebhookBot} — the main webhook bot instance that registers with Telegram and
 *       delegates incoming updates to the core processing pipeline.</li>
 *   <li>{@link WebhookController} — the REST controller that receives POST requests from
 *       Telegram and forwards them to {@link WebhookBot}.</li>
 * </ul>
 *
 * <p>All beans are guarded by {@link ConditionalOnMissingBean} so applications can supply
 * their own customised implementations.</p>
 *
 * <p>This auto-configuration is suppressed for all transport values other than
 * {@code WEBHOOK}. Broker consumer transports ({@code KAFKA_CONSUMER},
 * {@code RABBIT_CONSUMER}) and {@code NONE} are all handled by their own
 * autoconfiguration classes with no cross-dependency on this module.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "easygram.update", name = "transport", havingValue = "WEBHOOK")
@EnableConfigurationProperties(EasygramWebhookProperties.class)
public class WebhookAutoConfiguration {

    /**
     * Registers the primary {@link WebhookBot} bean wired from fine-grained provider beans.
     *
     * @param botProperties               common bot properties containing the token
     * @param webhookBotProperties        properties holding the webhook URL and other settings
     * @param triggers                    startup triggers executed once after authentication
     * @param filters                     filters applied to every incoming update
     * @param botDispatcher               dispatcher that routes updates to handler methods
     * @param botExceptionHandlerRegistry registry of exception handler methods
     * @param telegramClientProvider      provider for the outbound {@code TelegramClient}
     * @param executorServiceProvider     provider for the update-processing thread pool
     * @return a fully configured {@link WebhookBot} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public WebhookBot webhookBot(
            EasygramProperties botProperties,
            EasygramWebhookProperties webhookBotProperties,
            List<BotStartTrigger> triggers,
            List<BotFilter> filters,
            BotDispatcher botDispatcher,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            EasygramTelegramClientProvider telegramClientProvider,
            EasygramExecutorServiceProvider executorServiceProvider) {
        if (webhookBotProperties.requireSecretToken() != null && webhookBotProperties.requireSecretToken()
                && (webhookBotProperties.secretToken() == null || webhookBotProperties.secretToken().isBlank())) {
            throw new BeanCreationException("webhookBot",
                    "easygram.update.webhook.require-secret-token is true but secret-token is blank. "
                    + "Set easygram.update.webhook.secret-token to a non-blank value or disable the check.");
        }
        if (webhookBotProperties.secretToken() == null || webhookBotProperties.secretToken().isBlank()) {
            log.warn("easygram: webhook secret-token is not configured — any client that knows your "
                    + "webhook URL can POST fake updates to {}. "
                    + "Set a secret token to secure your endpoint:\n"
                    + "  easygram:\n"
                    + "    update:\n"
                    + "      webhook:\n"
                    + "        secret-token: \"your-random-secret\"  # min 1 char, max 256 chars",
                    webhookBotProperties.path());
        }
        return new WebhookBot(
                botProperties,
                webhookBotProperties,
                triggers,
                filters,
                botDispatcher,
                botExceptionHandlerRegistry,
                telegramClientProvider,
                executorServiceProvider);
    }

    /**
     * Registers the {@link WebhookController} that exposes the webhook HTTP endpoint.
     *
     * @param webhookBot           the bot instance that processes incoming updates
     * @param webhookBotProperties properties used for path resolution and secret-token validation
     * @param objectMapperProvider provider whose {@code ObjectMapper} deserialises update payloads
     * @return a new {@link WebhookController} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public WebhookController webhookController(
            WebhookBot webhookBot,
            EasygramWebhookProperties webhookBotProperties,
            EasygramObjectMapperProvider objectMapperProvider) {
        return new WebhookController(webhookBot, webhookBotProperties, objectMapperProvider);
    }
}
