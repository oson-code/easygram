package uz.example.linkedlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample: LinkedList in-memory transport bot.
 *
 * <p>Demonstrates the Easygram messaging SPI using a {@link java.util.concurrent.LinkedBlockingDeque}
 * as the "broker". The same JVM both publishes incoming Telegram updates into the deque
 * and consumes them for processing — no external Kafka or RabbitMQ instance required.</p>
 *
 * <p>Run with:</p>
 * <pre>{@code BOT_TOKEN=<your-token> mvn spring-boot:run}</pre>
 *
 * @since 0.0.6
 */
@SpringBootApplication
public class LinkedListBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkedListBotApplication.class, args);
    }
}
