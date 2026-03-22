/**
 * Easygram framework core engine — annotation-driven routing, filter pipeline,
 * argument resolution, and return-type handling for Telegram bots.
 *
 * <p>This module provides the processing engine that powers every Easygram bot. It depends
 * on {@code core-api} for all contracts and wires them together into a runnable
 * Spring Boot autoconfiguration.</p>
 *
 * <h2>Primary responsibilities</h2>
 * <ol>
 *   <li><strong>Routing</strong> — scans {@code @BotController} beans at startup, builds a
 *       registry of {@link uz.osoncode.easygram.core.handler.BotMethodHandler} instances, and
 *       dispatches each incoming {@code Update} to the best-matching handler method via
 *       {@link uz.osoncode.easygram.core.dispatcher.BotDispatcher}</li>
 *   <li><strong>Filter pipeline</strong> — collects all {@link uz.osoncode.easygram.core.filter.BotFilter}
 *       beans, sorts them by order, and runs them as a chain around each update's handler dispatch</li>
 *   <li><strong>Argument resolution</strong> — resolves handler method parameters from the current
 *       {@link uz.osoncode.easygram.core.model.BotRequest} using registered
 *       {@link uz.osoncode.easygram.core.argumentresolver.BotArgumentResolver} beans</li>
 *   <li><strong>Return-type handling</strong> — translates handler return values to
 *       {@link org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod} calls via
 *       registered {@link uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler} beans</li>
 *   <li><strong>Exception handling</strong> — dispatches exceptions thrown during handler execution
 *       to {@code @BotExceptionHandler} methods within the same {@code @BotController} or a global
 *       {@code @BotControllerAdvice}</li>
 * </ol>
 *
 * <h2>Package overview</h2>
 * <ul>
 *   <li>{@code uz.osoncode.easygram.core.autoconfigure} — Spring Boot {@code @AutoConfiguration} class</li>
 *   <li>{@code uz.osoncode.easygram.core.bot} — abstract {@link uz.osoncode.easygram.core.bot.Bot} base class and {@link uz.osoncode.easygram.core.bot.BotProperties}</li>
 *   <li>{@code uz.osoncode.easygram.core.dispatcher} — {@link uz.osoncode.easygram.core.dispatcher.BotDispatcher}</li>
 *   <li>{@code uz.osoncode.easygram.core.filter} — built-in filter implementations</li>
 *   <li>{@code uz.osoncode.easygram.core.handler} — handler loading, registry, and invocation</li>
 *   <li>{@code uz.osoncode.easygram.core.argumentresolver} — built-in argument resolver implementations</li>
 *   <li>{@code uz.osoncode.easygram.core.returntypehandler} — built-in return-type handler implementations</li>
 *   <li>{@code uz.osoncode.easygram.core.markup} — markup loader and in-memory registry</li>
 *   <li>{@code uz.osoncode.easygram.core.exceptionhandler} — exception handler registry and invocation</li>
 * </ul>
 *
 * @see uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration
 * @see uz.osoncode.easygram.core.bot.Bot
 * @see uz.osoncode.easygram.core.dispatcher.BotDispatcher
 */
package uz.osoncode.easygram.core;
