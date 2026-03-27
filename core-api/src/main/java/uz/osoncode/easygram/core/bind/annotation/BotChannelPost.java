package uz.osoncode.easygram.core.bind.annotation;

import java.lang.annotation.*;

/**
 * Maps a handler method to channel posts received by the bot.
 *
 * <p>Annotate a method with {@code @BotChannelPost} to handle any update whose
 * {@code channel_post} field is populated.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BotChannelPost {
}
