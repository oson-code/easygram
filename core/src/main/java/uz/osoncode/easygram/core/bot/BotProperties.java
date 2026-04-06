package uz.osoncode.easygram.core.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Common Telegram bot properties shared across all transports.
 *
 * <p>Bound from the {@code easygram} configuration prefix.
 * Validation is applied on startup — a missing or blank {@code token} causes a
 * {@link org.springframework.boot.context.properties.bind.validation.BindValidationException}
 * that prevents the application from starting.</p>
 *
 * @param token     the Bot API token issued by @BotFather; required for every transport
 * @param transport the active transport mechanism; defaults to {@link BotTransportType#LONG_POLLING}
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotTransportType
 */
@Validated
@ConfigurationProperties("easygram")
public record BotProperties(

        @NotBlank(message = "easygram.token must not be blank")
        String token,

        @NotNull(message = "easygram.transport must not be null")
        @DefaultValue("LONG_POLLING")
        BotTransportType transport
) {
}
