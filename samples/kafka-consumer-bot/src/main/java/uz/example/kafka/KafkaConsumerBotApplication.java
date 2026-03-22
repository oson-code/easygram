package uz.example.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application that demonstrates an Easygram bot receiving updates via Kafka consumer transport.
 * The bot echoes back messages received through a Kafka consumer.
 *
 * @since 0.0.1
 */
@SpringBootApplication
public class KafkaConsumerBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaConsumerBotApplication.class, args);
    }
}
