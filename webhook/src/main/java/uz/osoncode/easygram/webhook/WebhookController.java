package uz.osoncode.easygram.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.provider.EasygramObjectMapperProvider;
import uz.osoncode.easygram.core.util.Strings;

import java.util.Objects;

/**
 * REST controller that receives incoming Telegram webhook update payloads.
 *
 * <p>Telegram delivers updates as HTTP POST requests to the URL registered via
 * {@link org.telegram.telegrambots.meta.api.methods.updates.SetWebhook}. This controller
 * listens on the path configured by the {@code easygram.update.webhook.path} property
 * (defaults to {@code /webhook}).</p>
 *
 * <p>If {@link EasygramWebhookProperties#secretToken()} is set, every request is validated against
 * the {@code X-Telegram-Bot-Api-Secret-Token} header that Telegram attaches to each delivery.
 * Requests with a missing or mismatched token are rejected with {@code 401 Unauthorized}.</p>
 *
 * <p>Accepted updates are deserialized via {@link EasygramObjectMapperProvider} and forwarded to
 * {@link WebhookBot#handleUpdate(Update)} for processing.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookBot webhookBot;
    private final EasygramWebhookProperties webhookBotProperties;
    private final EasygramObjectMapperProvider objectMapperProvider;

    /**
     * Handles an incoming Telegram update delivered via webhook.
     *
     * <p>The path is resolved from the {@code easygram.update.webhook.path} property at startup
     * (defaults to {@code /webhook}). The method:</p>
     * <ol>
     *   <li>Validates the {@code X-Telegram-Bot-Api-Secret-Token} header when a secret token
     *       is configured.</li>
     *   <li>Deserializes the raw JSON body into an {@link Update} using the configured
     *       {@code ObjectMapper}.</li>
     *   <li>Forwards the update to {@link WebhookBot#handleUpdate(Update)}.</li>
     * </ol>
     *
     * @param body        the raw JSON payload sent by Telegram
     * @param secretToken the value of the {@code X-Telegram-Bot-Api-Secret-Token} header,
     *                    or {@code null} if the header is absent
     * @return {@code 200 OK} on success, {@code 401 Unauthorized} if secret token validation
     *         fails, or {@code 500 Internal Server Error} if deserialization fails
     */
    @PostMapping("${easygram.update.webhook.path:/webhook}")
    public ResponseEntity<Void> receiveUpdate(
            @RequestBody String body,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken) {

        if (Strings.isNotBlank(webhookBotProperties.secretToken())
                && !webhookBotProperties.secretToken().equals(secretToken)) {
            log.warn("Rejected webhook request: invalid or missing secret token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Update update = objectMapperProvider.provide().readValue(body, Update.class);
            webhookBot.handleUpdate(update);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to deserialize webhook update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Failed to process webhook update", e);
        }

        return ResponseEntity.ok().build();
    }
}
