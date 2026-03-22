package uz.example.producer.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
}
