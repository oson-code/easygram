package uz.osoncode.easygram.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata about the bot processing the current request.
 * Contains information like the bot's token, ID, and username.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotMetadata {

    /** The authentication token of the bot. */
    private String token;

    /** The unique identifier of the bot. */
    private Long id;

    /** The username of the bot (without the @ symbol). */
    private String username;

}
