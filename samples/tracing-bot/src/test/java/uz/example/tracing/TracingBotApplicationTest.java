package uz.example.tracing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies that the Spring Boot application context loads successfully.
 *
 * @since 0.0.7
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "easygram.token=test-token",
        "easygram.update.transport=KAFKA_CONSUMER",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "management.otlp.tracing.endpoint=http://localhost:4318/v1/traces"
})
class TracingBotApplicationTest {

    @Test
    void contextLoads() {
        // No assertion needed: the test passes if the context starts without throwing.
    }
}
