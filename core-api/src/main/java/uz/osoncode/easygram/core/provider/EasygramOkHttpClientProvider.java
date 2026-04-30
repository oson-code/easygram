package uz.osoncode.easygram.core.provider;

import okhttp3.OkHttpClient;

/**
 * Provider for the {@link OkHttpClient} used by the bot's HTTP transport.
 *
 * <p>Register a Spring bean of this type to customise connection timeouts, interceptors, TLS
 * settings, or any other {@code OkHttpClient} attribute without touching transport-specific
 * configuration:</p>
 *
 * <pre>{@code
 * @Bean
 * public EasygramOkHttpClientProvider botOkHttpClientProvider() {
 *     OkHttpClient client = new OkHttpClient.Builder()
 *             .connectTimeout(30, TimeUnit.SECONDS)
 *             .readTimeout(60, TimeUnit.SECONDS)
 *             .addInterceptor(new LoggingInterceptor())
 *             .build();
 *     return () -> client;
 * }
 * }</pre>
 *
 * <p>The returned instance is shared across all internal consumers; create it once and return
 * the same object from every call to {@link #provide()}.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface EasygramOkHttpClientProvider {

    /**
     * Returns the {@link OkHttpClient} to use for outbound Telegram API calls.
     *
     * @return a non-null {@link OkHttpClient}
     */
    OkHttpClient provide();
}
