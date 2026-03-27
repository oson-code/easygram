package uz.example.producer.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramUrl;
import uz.osoncode.easygram.core.provider.BotTelegramUrlProvider;

/**
 * Spring Boot application that demonstrates an Easygram webhook producer.
 * Receives Telegram webhook updates and produces them to a message broker for asynchronous processing.
 *
 * @since 0.0.1
 */
@SpringBootApplication
public class WebhookProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookProducerApplication.class, args);
    }

    @Bean
    public BotTelegramUrlProvider botTelegramUrlProvider() {
        return () -> TelegramUrl.builder()
                .schema("https")
                .host("api.telegram.org")
                .port(443)
                .build();
    }
}
