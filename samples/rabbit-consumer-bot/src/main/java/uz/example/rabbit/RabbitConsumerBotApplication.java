package uz.example.rabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramUrl;
import uz.osoncode.easygram.core.provider.BotTelegramUrlProvider;

/**
 * Spring Boot application that demonstrates an Easygram bot receiving updates via RabbitMQ consumer transport.
 * The bot echoes back messages received through a RabbitMQ consumer.
 *
 * @since 0.0.1
 */
@SpringBootApplication
public class RabbitConsumerBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitConsumerBotApplication.class, args);
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
