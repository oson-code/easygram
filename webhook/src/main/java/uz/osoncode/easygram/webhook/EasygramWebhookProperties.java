package uz.osoncode.easygram.webhook;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Telegram webhook bot transport.
 *
 * <p>Properties are bound from the {@code easygram.update.webhook} prefix in the application
 * configuration (e.g. {@code application.yml} or {@code application.properties}).
 * Bean Validation is applied at startup via {@link Validated}, so the application will fail
 * to start if any required property is missing or blank.</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * easygram:
 *   update:
 *     webhook:
 *       url: "https://example.com/webhook"
 *       path: "/webhook"
 *       secret-token: "my-secret"
 *       max-connections: 40
 *       drop-pending-updates: false
 *       unregister-on-shutdown: false
 * }</pre>
 *
 * @param url                   the public HTTPS URL Telegram will send updates to; must not be blank
 * @param path                  the local HTTP endpoint path that receives webhook updates; defaults to {@code /webhook}
 * @param secretToken           an optional secret token sent by Telegram in the
 *                              {@code X-Telegram-Bot-Api-Secret-Token} header for request validation
 * @param requireSecretToken    when {@code true}, the application fails to start if
 *                              {@code secretToken} is blank, ensuring the endpoint is never left
 *                              unprotected in production; defaults to {@code false}
 * @param maxBodyBytes          maximum allowed size of the incoming webhook request body in bytes;
 *                              requests exceeding this limit are rejected with {@code 413}; defaults
 *                              to {@code 1048576} (1 MB) to guard against malicious large payloads
 * @param maxConnections        maximum allowed number of simultaneous HTTPS connections
 *                              to the webhook (1–100); defaults to {@code 40} (Telegram's default)
 * @param dropPendingUpdates    when {@code true}, pending updates are dropped when the webhook
 *                              is registered; defaults to {@code false}
 * @param unregisterOnShutdown  when {@code true}, the webhook is deleted from Telegram when the
 *                              Spring application context shuts down; defaults to {@code false}
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Validated
@ConfigurationProperties("easygram.update.webhook")
public record EasygramWebhookProperties(

        /** The public HTTPS URL to which Telegram delivers webhook updates. */
        @NotBlank(message = "easygram.update.webhook.url must not be blank")
        @Pattern(regexp = "^https://.*",
                 message = "easygram.update.webhook.url must be an HTTPS URL (must start with https://)")
        String url,

        /** Local server path that accepts incoming webhook POST requests. */
        @DefaultValue("/webhook")
        String path,

        /** Optional secret token validated on every incoming webhook request. */
        String secretToken,

        /**
         * When {@code true}, the application refuses to start if {@code secretToken} is blank.
         * Use this in production to ensure the endpoint is never left unprotected.
         */
        @DefaultValue("false")
        Boolean requireSecretToken,

        /**
         * Maximum allowed request-body size in bytes.
         * Requests exceeding this limit are rejected with {@code 413 Payload Too Large}.
         * Defaults to 1 MB (1,048,576 bytes).
         */
        @DefaultValue("1048576")
        Long maxBodyBytes,

        /** Maximum number of simultaneous Telegram-to-server connections (1–100). Defaults to {@code 40}. */
        @DefaultValue("40")
        @Min(value = 1, message = "easygram.update.webhook.max-connections must be >= 1")
        @Max(value = 100, message = "easygram.update.webhook.max-connections must be <= 100")
        Integer maxConnections,

        /** Whether to discard queued updates when the webhook is registered. */
        @DefaultValue("false")
        Boolean dropPendingUpdates,

        /** Whether to call {@code deleteWebhook} on application shutdown. */
        @DefaultValue("false")
        Boolean unregisterOnShutdown
) {
}
