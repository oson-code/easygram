package uz.osoncode.easygram.core.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

/**
 * Built-in {@link BotFilter} that establishes MDC (Mapped Diagnostic Context) correlation keys
 * for every incoming Telegram update, enabling production-grade log correlation and tracing.
 *
 * <p>This filter runs at {@link BotFilterOrder#MDC_CONTEXT} — the very first position in the
 * chain — so that every downstream log statement (context setter, dispatcher, handlers, broker
 * filters) automatically carries the update's correlation context without any extra plumbing.</p>
 *
 * <h2>MDC keys set by this filter</h2>
 * <table border="1">
 *   <caption>MDC key reference</caption>
 *   <tr><th>Key</th><th>Value</th><th>Available from</th></tr>
 *   <tr><td>{@code bot.update.id}</td><td>Telegram update ID (integer)</td><td>Immediately</td></tr>
 *   <tr><td>{@code bot.transport}</td><td>Transport type name, e.g. {@code LONG_POLLING}</td><td>Immediately</td></tr>
 *   <tr><td>{@code bot.user.id}</td><td>Telegram user ID (when resolvable)</td><td>After context-setter runs</td></tr>
 *   <tr><td>{@code bot.chat.id}</td><td>Telegram chat ID (when resolvable)</td><td>After context-setter runs</td></tr>
 * </table>
 *
 * <p><strong>MDC lifecycle:</strong> All keys are removed in a {@code finally} block after the
 * entire downstream chain returns, so they never leak into unrelated thread-pool tasks. This
 * filter is safe for both single-threaded and executor-based deployments.</p>
 *
 * <p><strong>Log pattern example (logback-spring.xml):</strong></p>
 * <pre>{@code
 * <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level
 *     [updateId=%X{bot.update.id} chatId=%X{bot.chat.id} userId=%X{bot.user.id}]
 *     %logger{36} - %msg%n</pattern>
 * }</pre>
 *
 * <p>To disable or customise MDC context, declare your own {@code BotMdcFilter} bean; the
 * framework's default will be skipped via {@code @ConditionalOnMissingBean}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.4
 * @see BotFilterOrder#MDC_CONTEXT
 */
@Slf4j
@RequiredArgsConstructor
public class BotMdcFilter implements BotFilter {

    /** MDC key for the Telegram update ID. */
    public static final String MDC_UPDATE_ID  = "bot.update.id";

    /** MDC key for the active transport type. */
    public static final String MDC_TRANSPORT  = "bot.transport";

    /** MDC key for the Telegram user ID (set after {@code BotContextSetterFilter} runs). */
    public static final String MDC_USER_ID    = "bot.user.id";

    /** MDC key for the Telegram chat ID (set after {@code BotContextSetterFilter} runs). */
    public static final String MDC_CHAT_ID    = "bot.chat.id";

    /** Provides the active transport type for the {@link #MDC_TRANSPORT} key. */
    private final BotConfigurer botConfigurer;

    /**
     * Returns {@link BotFilterOrder#MDC_CONTEXT}, making this the first filter to execute.
     *
     * @return {@link BotFilterOrder#MDC_CONTEXT}
     */
    @Override
    public int getOrder() {
        return BotFilterOrder.MDC_CONTEXT;
    }

    /**
     * Sets MDC correlation keys and invokes the downstream chain.
     * All keys are removed in a {@code finally} block regardless of outcome.
     *
     * <p>{@code bot.update.id} and {@code bot.transport} are populated immediately.
     * {@code bot.user.id} and {@code bot.chat.id} are pushed by
     * {@link BotContextSetterFilter} (the next filter in the chain), which resolves
     * user and chat from the update and writes them to MDC before continuing the chain.
     * This ensures all downstream filters and handlers have the full correlation context.</p>
     *
     * @param botRequest  the current bot request; never {@code null}
     * @param botResponse the mutable response accumulator; never {@code null}
     * @param filterChain the remaining filter chain; never {@code null}
     */
    @Override
    public void doFilter(BotRequest botRequest, BotResponse botResponse, BotFilterChain filterChain) {
        int updateId = botRequest.getUpdate().getUpdateId();
        MDC.put(MDC_UPDATE_ID, String.valueOf(updateId));
        MDC.put(MDC_TRANSPORT, botConfigurer.transportType().name());
        log.trace("MDC initialised: updateId={} transport={}", updateId, botConfigurer.transportType().name());
        try {
            // BotContextSetterFilter (next in chain) will populate user/chat on botRequest
            // and immediately push MDC_USER_ID / MDC_CHAT_ID so they are available to all
            // downstream handlers and filters during the same synchronous execution.
            filterChain.doFilter(botRequest, botResponse);
        } finally {
            log.trace("MDC cleared: updateId={}", updateId);
            MDC.remove(MDC_UPDATE_ID);
            MDC.remove(MDC_TRANSPORT);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CHAT_ID);
        }
    }
}
