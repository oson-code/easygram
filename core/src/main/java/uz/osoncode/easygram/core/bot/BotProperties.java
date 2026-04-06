package uz.osoncode.easygram.core.bot;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Core Telegram bot properties shared across all transports.
 *
 * <p>Bound from the {@code easygram} configuration prefix. Validation is applied on
 * startup — a missing or blank {@code token} causes a
 * {@link org.springframework.boot.context.properties.bind.validation.BindValidationException}
 * that prevents the application from starting.</p>
 *
 * <p>The active update transport is configured separately via
 * {@link BotUpdateProperties} at the {@code easygram.update} prefix.</p>
 *
 * @param token the Bot API token issued by @BotFather; required for every transport
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotUpdateProperties
 */
@Validated
@ConfigurationProperties("easygram")
public record BotProperties(

        @NotBlank(message = "easygram.token must not be blank")
        String token
) {
}

