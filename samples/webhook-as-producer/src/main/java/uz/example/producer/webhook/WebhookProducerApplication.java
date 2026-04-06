package uz.example.producer.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application demonstrating an Easygram webhook producer.
 *
 * <p>Receives Telegram updates via HTTPS webhook and publishes them to a message broker
 * (Kafka or RabbitMQ) for asynchronous downstream processing.</p>
 *
 * <p>All infrastructure beans ({@code TelegramUrl}, {@code OkHttpClient}, etc.) use
 * framework defaults. Override individual provider beans only when customisation is needed.</p>
 *
 * @since 0.0.1
 */
@SpringBootApplication
public class WebhookProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookProducerApplication.class, args);
    }
}
