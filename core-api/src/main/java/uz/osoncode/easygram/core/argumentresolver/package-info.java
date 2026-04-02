/**
 * Argument resolver SPI and built-in implementations for injecting parameters into
 * {@link uz.osoncode.easygram.core.stereotype.BotController} handler methods.
 *
 * <p>The framework inspects each handler method's parameters and finds a matching
 * {@link uz.osoncode.easygram.core.argumentresolver.BotArgumentResolver} for each one.
 * Resolvers are collected automatically from the Spring application context.</p>
 *
 * <h2>Built-in injectable parameters</h2>
 * <table border="1">
 *   <caption>Built-in injectable parameters</caption>
 *   <tr><th>Parameter type / annotation</th><th>Resolver</th></tr>
 *   <tr><td>{@link org.telegram.telegrambots.meta.api.objects.Update}</td><td>{@code BotUpdateArgumentResolver}</td></tr>
 *   <tr><td>{@link org.telegram.telegrambots.meta.api.objects.User}</td><td>{@code BotUserArgumentResolver}</td></tr>
 *   <tr><td>{@link org.telegram.telegrambots.meta.api.objects.chat.Chat}</td><td>{@code BotChatArgumentResolver}</td></tr>
 *   <tr><td>{@link org.telegram.telegrambots.meta.generics.TelegramClient}</td><td>{@code BotTelegramClientArgumentResolver}</td></tr>
 *   <tr><td>{@link uz.osoncode.easygram.core.model.BotRequest}</td><td>{@code BotRequestArgumentResolver}</td></tr>
 *   <tr><td>{@link uz.osoncode.easygram.core.model.BotResponse}</td><td>{@code BotResponseArgumentResolver}</td></tr>
 *   <tr><td>{@link uz.osoncode.easygram.core.model.BotMetadata}</td><td>{@code BotMetadataArgumentResolver}</td></tr>
 *   <tr><td>{@code Throwable} (subtype)</td><td>{@code BotThrowableArgumentResolver}</td></tr>
 *   <tr><td>{@link org.telegram.telegrambots.meta.api.objects.Contact}</td><td>{@code BotContactArgumentResolver}</td></tr>
 *   <tr><td>{@link org.telegram.telegrambots.meta.api.objects.location.Location}</td><td>{@code BotLocationArgumentResolver}</td></tr>
 *   <tr><td>{@code @BotCommandValue String}</td><td>{@code BotCommandArgumentResolver}</td></tr>
 *   <tr><td>{@code @BotCommandQueryParam("name") String}</td><td>{@code BotCommandQueryParamBotArgumentResolver}</td></tr>
 *   <tr><td>{@code @BotTextValue String}</td><td>{@code BotTextArgumentResolver}</td></tr>
 *   <tr><td>{@code @BotCallbackQueryData String}</td><td>{@code BotCallbackQueryDataArgumentResolver}</td></tr>
 *   <tr><td>{@code Locale} (core-i18n)</td><td>{@code BotLocaleArgumentResolver}</td></tr>
 * </table>
 *
 * <h2>Adding a custom argument resolver</h2>
 * <pre>{@code
 * @Component
 * public class CurrentUserResolver implements BotArgumentResolver {
 *
 *     @Override
 *     public boolean supportsParameter(Parameter parameter) {
 *         return CurrentUser.class.isAssignableFrom(
 *             ParameterUtils.effectiveType(parameter));
 *     }
 *
 *     @Override
 *     public Object resolveArgument(Parameter parameter, BotRequest request, BotResponse response) {
 *         return userRepository.findByTelegramId(request.getUser().getId());
 *     }
 * }
 * }</pre>
 */
package uz.osoncode.easygram.core.argumentresolver;
