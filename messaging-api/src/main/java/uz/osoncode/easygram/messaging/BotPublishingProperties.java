package uz.osoncode.easygram.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the Telegram Bot messaging integration.
 *
 * <p>Properties are bound from the {@code telegram.bot.messaging} prefix in the application
 * configuration (e.g. {@code application.yml} or {@code application.properties}).</p>
 *
 * <p>Example {@code application.yml} snippet:</p>
 * <pre>{@code
 * telegram:
 *   bot:
 *     messaging:
 *       forward-only: false
 * }</pre>
 *
 * @param forwardOnly when {@code true}, the update is published to the broker and the bot's
 *                    own handler pipeline is skipped (the update is not dispatched to any
 *                    {@code @BotController} method). When {@code false} (the default), the
 *                    update is published <em>and</em> continues through the normal processing
 *                    pipeline.
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@ConfigurationProperties("telegram.bot.messaging")
public record BotPublishingProperties(

        /**
         * Whether to stop the filter chain after publishing.
         * {@code false} (default) = publish then continue processing;
         * {@code true} = publish only, skip bot handlers.
         */
        @DefaultValue("false")
        Boolean forwardOnly
) {
}
