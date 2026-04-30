package uz.example.tracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the tracing-bot sample.
 *
 * <p>This sample demonstrates end-to-end distributed tracing for an Easygram Kafka consumer bot:
 * <ol>
 *   <li>A separate producer (e.g., {@code longpolling-as-producer}) publishes Telegram updates to Kafka.</li>
 *   <li>This bot consumes updates from Kafka; Spring Kafka + Micrometer automatically propagate
 *       the OTel trace context from the producer span through the Kafka record headers.</li>
 *   <li>Every {@code easygram.update} span created by {@code BotObservabilityFilter} is a child of
 *       the upstream Kafka consumer span, forming a complete cross-service trace.</li>
 *   <li>Spans are exported to Grafana Tempo via OTLP HTTP; view them at
 *       <a href="http://localhost:3000">http://localhost:3000</a>.</li>
 * </ol>
 *
 * <p>Run with:
 * <pre>{@code
 *   docker compose up -d          # start Kafka, Tempo, Grafana
 *   BOT_TOKEN=<token> mvn spring-boot:run
 * }</pre>
 *
 * @since 0.0.7
 */
@SpringBootApplication
public class TracingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TracingBotApplication.class, args);
    }
}
