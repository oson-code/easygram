package uz.example.rabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application demonstrating an Easygram bot receiving updates via RabbitMQ.
 *
 * <p>All infrastructure beans use framework defaults. No provider overrides needed
 * for a standard consumer-only bot.</p>
 *
 * @since 0.0.1
 */
@SpringBootApplication
public class RabbitConsumerBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitConsumerBotApplication.class, args);
    }
}
