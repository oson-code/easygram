package uz.example.chatstate;

import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramUrl;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.provider.BotOkHttpClientProvider;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.core.provider.BotTelegramUrlProvider;

import java.time.Duration;

/**
 * Spring Boot application demonstrating Easygram conversation state management.
 *
 * <p>Overrides the OkHttp client to increase timeouts for long-polling reliability.
 * All other infrastructure beans (TelegramUrl, ObjectMapper, etc.) use framework defaults.</p>
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
                .connectTimeout(Duration.ofSeconds(50))
                .readTimeout(Duration.ofSeconds(50))
                .writeTimeout(Duration.ofSeconds(50))
                .build();
    }

    @Bean
    public BotTelegramUrlProvider  botTelegramUrlProvider() {
        return () -> TelegramUrl.builder()
                .port(443)
                .host("tg.imirsaburov.uz")
                .schema("https")
                .build();
    }
}
