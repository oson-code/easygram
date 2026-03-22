package uz.osoncode.easygram.core.bot;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Immutable configuration record that carries shared infrastructure objects used to configure
 * and initialize the bot framework components.
 * An instance of this record is typically created once at application startup and injected
 * into framework internals that require JSON serialization support.
 *
 * <p>The {@link #transportType()} field identifies which update-delivery mechanism is active,
 * allowing components and user code to branch on transport type with full IDE autocompletion:</p>
 * <pre>{@code
 * if (botConfigurer.transportType() == BotTransportType.LONG_POLLING) { ... }
 * }</pre>
 *
 * @param objectMapper  the Jackson {@link ObjectMapper} used for JSON serialization/deserialization
 * @param transportType the transport mechanism used to deliver Telegram updates
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public record BotConfigurer(

        /**
         * The Jackson {@link ObjectMapper} instance used for JSON serialization and deserialization
         * within the bot framework.
         */
        ObjectMapper objectMapper,

        /**
         * The transport mechanism used to deliver Telegram updates to this bot instance.
         *
         * @see BotTransportType
         */
        BotTransportType transportType
) {
}
