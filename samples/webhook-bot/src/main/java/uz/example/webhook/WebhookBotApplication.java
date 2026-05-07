package uz.example.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uz.osoncode.easygram.core.provider.EasygramTelegramUrlProvider;

/**
 * Sample webhook bot that demonstrates overriding the {@link EasygramTelegramUrlProvider}
 * to point the bot at a custom (local) Bot API server.
 *
 * <p>Only the provider beans you actually need to customise must be declared.
 * Everything else falls back to the framework defaults.</p>
 */
@SpringBootApplication
public class WebhookBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookBotApplication.class, args);
    }

}
