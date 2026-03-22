package uz.osoncode.easygram.core.handler.message.replybutton;

import uz.osoncode.easygram.core.model.BotRequest;

/**
 * Strategy interface for matching a {@link uz.osoncode.easygram.core.bind.annotation.BotReplyButton}
 * annotation against an incoming bot request.
 *
 * <p>The default implementation (registered in {@code core}) performs a simple exact-text comparison
 * between the annotation values and the incoming message text.</p>
 *
 * <p>When {@code core-i18n} is on the classpath it registers a replacement implementation that
 * treats annotation values as message-bundle keys and resolves them in the user's locale before
 * comparing — so a single {@code @BotReplyButton("btn.yes")} annotation handles all configured
 * languages automatically.</p>
 *
 * <p>Declare a bean of this type to provide a completely custom matching strategy:</p>
 * <pre>{@code
 * @Bean
 * public BotReplyButtonMatcher myMatcher() {
 *     return (values, request) -> {
 *         String text = request.getUpdate().getMessage().getText();
 *         return Arrays.asList(values).contains(text.toLowerCase());
 *     };
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@FunctionalInterface
public interface BotReplyButtonMatcher {

    /**
     * Returns {@code true} if the incoming bot request should be handled by a
     * {@code @BotReplyButton} method annotated with the given values.
     *
     * <p>Implementations may assume that the update already contains a text message —
     * the presence check is performed by the caller before this method is invoked.</p>
     *
     * @param values  the values declared on the {@code @BotReplyButton} annotation
     * @param request the current bot request; {@code request.getUpdate().getMessage().getText()} is non-null
     * @return {@code true} if the handler should process this request
     */
    boolean matches(String[] values, BotRequest request);
}
