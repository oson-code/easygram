package uz.example.longpolling;

import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.provider.BotOkHttpClientProvider;

import java.time.Duration;

/**
 * Sample long-polling bot that demonstrates overriding the {@link BotOkHttpClientProvider}
 * to customise OkHttp timeouts.
 *
 * <p>Only the provider beans you actually need to customise must be declared.
 * Everything else (ObjectMapper, TelegramUrl, ExecutorService, TelegramClient) falls back
 * to the framework defaults registered by {@code CoreAutoConfiguration}.</p>
 */
@SpringBootApplication
public class LongpollingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(LongpollingBotApplication.class, args);
    }

    /**
     * Override only the HTTP client — all other infrastructure providers use their defaults.
     */
    @Bean
    public BotOkHttpClientProvider botOkHttpClientProvider() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(50))
                .readTimeout(Duration.ofSeconds(50))
                .writeTimeout(Duration.ofSeconds(50))
                .build();
        return () -> client;
    }
}
