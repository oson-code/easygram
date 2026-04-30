package uz.osoncode.easygram.longpolling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the long-polling transport.
 *
 * <pre>{@code
 * easygram:
 *   update:
 *     long-polling:
 *       limit: 100           # max updates per poll (1–100, default 100)
 *       timeout-seconds: 50  # Telegram long-poll timeout in seconds (default 50)
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.7
 */
@ConfigurationProperties(prefix = "easygram.update.long-polling")
public record EasygramLongPollingProperties(

        /**
         * Maximum number of updates to retrieve per {@code GetUpdates} request.
         * Accepted values: 1–100. Defaults to {@code 100}.
         */
        @DefaultValue("100") Integer limit,

        /**
         * Timeout in seconds for the Telegram long-poll connection.
         * Set to {@code 0} for short-polling (not recommended for production).
         * Defaults to {@code 50}.
         */
        @DefaultValue("50") Integer timeoutSeconds
) {
}
