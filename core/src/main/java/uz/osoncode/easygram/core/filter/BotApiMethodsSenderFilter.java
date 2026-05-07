package uz.osoncode.easygram.core.filter;


import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.Objects;

/**
 * Built-in filter that sends all accumulated {@link BotApiMethod} responses after the
 * handler chain has finished processing.
 *
 * <p>This filter first lets the rest of the filter chain (including the dispatcher and
 * all downstream handlers) run. After control returns, it iterates over all
 * {@link BotApiMethod} entries collected in {@link BotResponse#getBotApiMethods()} and
 * dispatches each one asynchronously via
 * {@link TelegramClient#executeAsync(BotApiMethod)}.
 *
 * <p>The filter is ordered at {@link BotFilterOrder#API_SENDER} ({@code Integer.MIN_VALUE + 1}),
 * immediately after the context-setter, ensuring it wraps the entire processing pipeline
 * and can observe the fully-populated response.
 *
 * <p><strong>Error classification:</strong> Telegram API errors are classified by HTTP status:
 * <ul>
 *   <li><em>4xx (client errors)</em> — logged as {@code ERROR} and <em>suppressed</em>.
 *       These are permanent failures (e.g. bad parse mode, forbidden, not found). Retrying
 *       the same message will never succeed, and re-throwing would cause broker transports such
 *       as RabbitMQ to enter an infinite requeue loop. Fix the message payload in your handler.</li>
 *   <li><em>5xx / network errors</em> — logged as {@code ERROR} and re-thrown so that
 *       the caller (filter chain or transport layer) can apply its own retry strategy.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class BotApiMethodsSenderFilter implements BotFilter {

    /**
     * Returns the execution order of this filter.
     *
     * <p>Positioned at {@link BotFilterOrder#API_SENDER} so the request context
     * is always available when this filter sends API methods.
     *
     * @return {@link BotFilterOrder#API_SENDER}.
     */
    @Override
    public int getOrder() {
        return BotFilterOrder.API_SENDER;
    }

    /**
     * Continues the filter chain, then sends every {@link BotApiMethod}
     * accumulated in {@link BotResponse}.
     *
     * <p>Sends are performed via {@link TelegramClient#execute(BotApiMethod)} obtained
     * from {@link BotRequest#getTelegramClient()}. Errors are handled per
     * {@link BotApiMethodsSenderFilter class-level} error-classification rules:
     * 4xx Telegram errors are logged and suppressed; 5xx / network errors are logged and
     * re-thrown after all methods have been attempted so that remaining deliveries are
     * not interrupted.
     *
     * @param botRequest  the current bot request providing the {@link TelegramClient}.
     * @param botResponse the response object holding the list of API methods to send.
     * @param filterChain the remaining filter chain to invoke before sending responses.
     */
    @Override
    public void doFilter(BotRequest botRequest, BotResponse botResponse, BotFilterChain filterChain) {

        filterChain.doFilter(botRequest, botResponse);
        TelegramClient telegramClient = botRequest.getTelegramClient();

        if (Objects.isNull(telegramClient)) {
            log.warn("No TelegramClient available — skipping API method delivery for update: {}",
                    botRequest.getUpdate().getUpdateId());
            return;
        }

        RuntimeException sendFailure = null;
        for (BotApiMethod<?> botApiMethod : botResponse.getBotApiMethods()) {
            try {
                telegramClient.execute(botApiMethod);
            } catch (Throwable e) {
                if (isTelegramClientError(e)) {
                    log.error("[update={}] Telegram rejected '{}' with a permanent error (4xx) — skipping rethrow; fix the message payload in your handler",
                            botRequest.getUpdate().getUpdateId(), botApiMethod.getClass().getSimpleName(), e);
                } else {
                    log.error("[update={}] Failed to send '{}' — transient error, will rethrow after all methods are attempted",
                            botRequest.getUpdate().getUpdateId(), botApiMethod.getClass().getSimpleName(), e);
                    // Wrap and remember; attempt remaining methods before propagating.
                    sendFailure = (e instanceof RuntimeException re) ? re
                            : new RuntimeException("Telegram send failure for update " + botRequest.getUpdate().getUpdateId(), e);
                }
            }
        }

        if (sendFailure != null) {
            throw sendFailure;
        }
    }

    /**
     * Returns {@code true} if {@code e} or any exception in its cause chain is a
     * {@link TelegramApiRequestException} with an HTTP 4xx status code (400–499).
     *
     * <p>Such errors represent permanent client-side failures (bad request, forbidden,
     * not found). Retrying the same message payload will never succeed, so the caller
     * suppresses them instead of re-throwing.
     *
     * @param e the throwable to inspect
     * @return {@code true} for Telegram 4xx errors, {@code false} otherwise
     */
    private static boolean isTelegramClientError(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof TelegramApiRequestException tae) {
                int code = tae.getErrorCode();
                return code >= 400 && code < 500;
            }
            t = t.getCause();
        }
        return false;
    }
}
