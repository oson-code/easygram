/**
 * Core request/response model classes passed through the Easygram filter and handler pipeline.
 *
 * <h2>Key types</h2>
 * <ul>
 *   <li>{@link uz.osoncode.easygram.core.model.BotRequest} — the mutable context object carrying the incoming
 *       {@link org.telegram.telegrambots.meta.api.objects.Update}, the resolved
 *       {@link org.telegram.telegrambots.meta.api.objects.User} and
 *       {@link org.telegram.telegrambots.meta.api.objects.chat.Chat}, the
 *       {@link org.telegram.telegrambots.meta.generics.TelegramClient}, and any exception raised during processing</li>
 *   <li>{@link uz.osoncode.easygram.core.model.BotResponse} — an accumulator for
 *       {@link org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod} instances queued
 *       by handler return-type handlers and executed by {@code BotApiMethodsSenderFilter}</li>
 *   <li>{@link uz.osoncode.easygram.core.model.BotMetadata} — read-only metadata about the authenticated bot
 *       (ID, username, first name, transport type) available for injection into handler methods</li>
 * </ul>
 *
 * <p>Both {@link uz.osoncode.easygram.core.model.BotRequest} and
 * {@link uz.osoncode.easygram.core.model.BotResponse} are created once per incoming update
 * and passed through the entire filter chain and handler invocation.
 * They should not be stored beyond the scope of a single update's processing.</p>
 */
package uz.osoncode.easygram.core.model;
