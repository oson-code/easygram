package uz.osoncode.easygram.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Metadata about the bot processing the current request.
 * Contains information like the bot's token, ID, and username.
 *
 * <p>This class is immutable: once constructed, its fields cannot be modified.
 * The token field is especially sensitive and must not be mutable on a shared object.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Value
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class BotMetadata {

    /** The authentication token of the bot. */
    String token;

    /** The unique identifier of the bot. */
    Long id;

    /** The username of the bot (without the @ symbol). */
    String username;

}
