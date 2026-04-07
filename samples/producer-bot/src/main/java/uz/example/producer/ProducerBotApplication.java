package uz.example.producer;

import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.provider.BotOkHttpClientProvider;

import java.time.Duration;

/**
 * Docker-ready producer bot.
 *
 * <p>Receives Telegram updates via long-polling or webhook and forwards every update to
 * the configured message broker (Kafka or RabbitMQ). No local handler code is required
 * because {@code easygram.messaging.forward-only=true} short-circuits before dispatch.</p>
 *
 * <p>All configuration is supplied via Spring property names set as environment variables
 * (e.g. {@code easygram.token}, {@code easygram.messaging.producer.type}).
 * See {@code .env.example} and the README for the full property reference.</p>
 */
@SpringBootApplication
public class ProducerBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerBotApplication.class, args);
    }

    /**
     * Configures OkHttp timeouts suitable for long-polling (60 s read timeout keeps
     * the connection alive during Telegram's 30 s long-poll wait).
     */
    @Bean
    public BotOkHttpClientProvider botOkHttpClientProvider() {
        return () -> new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(1))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }
}
