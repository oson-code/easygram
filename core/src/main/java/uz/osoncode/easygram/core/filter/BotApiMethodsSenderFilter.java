package uz.osoncode.easygram.core.filter;


import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

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
     * Continues the filter chain, then asynchronously sends every {@link BotApiMethod}
     * accumulated in {@link BotResponse}.
     *
     * <p>Sends are performed via {@link TelegramClient#executeAsync(BotApiMethod)} obtained
     * from {@link BotRequest#getTelegramClient()}. Any {@link Throwable} thrown during
     * sending is caught and logged as an error without interrupting delivery of the
     * remaining methods.
     *
     * @param botRequest  the current bot request providing the {@link TelegramClient}.
     * @param botResponse the response object holding the list of API methods to send.
     * @param filterChain the remaining filter chain to invoke before sending responses.
     */
    @Override
    public void doFilter(BotRequest botRequest, BotResponse botResponse, BotFilterChain filterChain) {

        filterChain.doFilter(botRequest, botResponse);
        TelegramClient telegramClient = botRequest.getTelegramClient();

        try {
            for (BotApiMethod<?> botApiMethod : botResponse.getBotApiMethods()) {
                telegramClient.executeAsync(botApiMethod);
            }
        } catch (Throwable e) {
            log.error("Error while sending BotApiMethod for update: {}", botRequest.getUpdate(), e);
        }
    }
}
