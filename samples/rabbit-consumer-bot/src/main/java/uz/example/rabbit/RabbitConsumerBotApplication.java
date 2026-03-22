package uz.example.rabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
}
