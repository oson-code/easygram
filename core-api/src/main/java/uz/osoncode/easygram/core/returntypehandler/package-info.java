/**
 * Return-type handler SPI for the Easygram framework.
 *
 * <p>A {@link uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler} translates the value
 * returned by a {@link uz.osoncode.easygram.core.stereotype.BotController} handler method into one or
 * more {@link org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod} calls that are
 * queued in the {@link uz.osoncode.easygram.core.model.BotResponse} and later executed by
 * {@code BotApiMethodsSenderFilter}.</p>
 *
 * <h2>Built-in return types</h2>
 *
 * <table border="1">
 *   <tr><th>Return type</th><th>Handler</th><th>Module</th></tr>
 *   <tr><td>{@code void}</td><td>{@code BotVoidReturnHandler}</td><td>core</td></tr>
 *   <tr><td>{@code String}</td><td>{@code BotStringReturnHandler}</td><td>core</td></tr>
 *   <tr><td>{@link uz.osoncode.easygram.core.reply.PlainReply}</td><td>{@code BotPlainReplyReturnTypeHandler}</td><td>core</td></tr>
 *   <tr><td>{@link uz.osoncode.easygram.core.reply.PlainTextTemplate}</td><td>{@code BotPlainTextTemplateReturnTypeHandler}</td><td>core</td></tr>
 *   <tr><td>{@code BotApiMethod<?>}</td><td>{@code BotBotApiMethodReturnHandler}</td><td>core</td></tr>
 *   <tr><td>{@code Collection<BotApiMethod<?>>}</td><td>{@code BotBotApiMethodsReturnHandler}</td><td>core</td></tr>
 *   <tr><td>{@code Collection<Object>} (mixed)</td><td>{@code BotMixedCollectionReturnTypeHandler}</td><td>core</td></tr>
 *   <tr><td>{@code LocalizedReply}</td><td>{@code BotLocalizedReplyReturnTypeHandler}</td><td>core-i18n</td></tr>
 *   <tr><td>{@code LocalizedTemplate}</td><td>{@code BotLocalizedTemplateReturnTypeHandler}</td><td>core-i18n</td></tr>
 * </table>
 *
 * <h2>Adding a custom return-type handler</h2>
 * <p>Implement {@link uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler} and register
 * it as a Spring {@code @Bean}. The framework auto-collects all beans of this type and evaluates them
 * in order — the first handler whose {@code supportsReturnType(Method)} returns {@code true} is used.</p>
 *
 * <pre>{@code
 * @Component
 * public class MyTypeReturnHandler implements BotReturnTypeHandler {
 *
 *     @Override
 *     public boolean supportsReturnType(Method method) {
 *         return MyType.class.isAssignableFrom(method.getReturnType());
 *     }
 *
 *     @Override
 *     public void handleReturnType(BotRequest req, BotResponse res, Object value) {
 *         MyType result = (MyType) value;
 *         res.addBotApiMethod(SendMessage.builder()
 *                 .chatId(req.getChat().getId())
 *                 .text(result.toText())
 *                 .build());
 *     }
 * }
 * }</pre>
 *
 * <p>To also participate in mixed-collection dispatch, override
 * {@link uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler#supportsElement(Object)}.</p>
 */
package uz.osoncode.easygram.core.returntypehandler;
