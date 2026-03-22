package uz.example.chatstate;

import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramUrl;
import uz.osoncode.easygram.core.provider.BotOkHttpClientProvider;
import uz.osoncode.easygram.core.provider.BotTelegramUrlProvider;

/**
 * Spring Boot application that demonstrates an Easygram bot with conversation state management.
 * Configures custom HTTP client and Telegram URL provider for handling stateful bot interactions.
 *
 * @since 0.0.1
 */
@SpringBootApplication
public class ChatStateBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatStateBotApplication.class, args);
    }

    @Bean
    public BotOkHttpClientProvider botOkHttpClientProvider() {
        return () -> new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(50))
                .readTimeout(java.time.Duration.ofSeconds(50))
                .writeTimeout(java.time.Duration.ofSeconds(50))
                .build();
    }

    @Bean
    public BotTelegramUrlProvider  botTelegramUrlProvider() {
        return () -> TelegramUrl.builder()
                .schema("https")
                .host("api.telegram.org")
                .port(443)
                .build();
    }
}
