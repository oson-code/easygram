package uz.osoncode.easygram.core.handler.invocation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.bind.annotation.BotClearChatState;
import uz.osoncode.easygram.core.bind.annotation.BotClearMarkup;
import uz.osoncode.easygram.core.bind.annotation.BotForwardChatState;
import uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup;
import uz.osoncode.easygram.core.bind.annotation.BotParseMode;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.markup.MarkupAware;
import uz.osoncode.easygram.core.reply.PlainReply;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link BotHandlerInvocationFilter} that applies markup to the handler method's return value.
 *
 * <p>Runs at order {@link BotHandlerInvocationFilterOrder#MARKUP_APPLICATION}, just after
 * {@code MethodInvocationFilter} has set {@code ctx.returnValue}.</p>
 *
 * <h2>Markup resolution precedence (highest to lowest)</h2>
 * <ol>
 *   <li>{@link BotClearMarkup} on the method → always sends {@code ReplyKeyboardRemove},
 *       overriding any keyboard already set on the return value.</li>
 *   <li>{@code returnValue.getKeyboard() != null} → keyboard is attached directly
 *       (set by the handler via {@code .withKeyboard(keyboard)}); no further resolution
 *       is performed by this filter.</li>
 *   <li>{@code returnValue.getMarkupId() != null} → markup ID is set by the handler
 *       (via {@code .withMarkup("id")}); forwarded to the registry by the return-type
 *       handler downstream.</li>
 *   <li>{@link BotReplyMarkup} annotation → fallback; applied only when the return value
 *       carries no keyboard and no markup ID.</li>
 *   <li><strong>State-bound keyboard</strong> → lowest-priority fallback; attempted when
 *       none of the above are present. The effective state is determined as follows:
 *       <ul>
 *         <li>If {@link BotClearChatState} is on the method → no auto-keyboard.</li>
 *         <li>If {@link BotForwardChatState} is on the method → use its declared value
 *             as the state key (the actual state transition hasn't happened yet; it runs
 *             in {@code ChatStateUpdateFilter} which is ordered after this filter).</li>
 *         <li>Otherwise → look up the current state from {@link BotChatStateService}.</li>
 *       </ul>
 *       If a keyboard is registered for that state in {@link BotMarkupRegistry}, it is
 *       attached via {@code withKeyboard(keyboard)} — <em>unless</em> the return value
 *       has {@code isEditMessage() == true} and the resolved keyboard is not an
 *       {@code InlineKeyboardMarkup}: Telegram's edit-message API only supports inline
 *       keyboards, so a non-inline state-bound keyboard is skipped in that context.
 *   </li>
 * </ol>
 *
 * <h2>Supported return types</h2>
 * <ul>
 *   <li>{@link MarkupAware} — transformed via immutable builder methods; {@code ctx.returnValue}
 *       is replaced with the transformed copy.</li>
 *   <li>{@link String} — when a markup action is required, wrapped in a {@link PlainReply} so
 *       that downstream {@code ReturnTypeDispatchFilter} routes it through
 *       {@code BotPlainReplyReturnTypeHandler}. Left unchanged when no markup action applies.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
public class MarkupApplicationFilter implements BotHandlerInvocationFilter {

    private final Optional<BotMarkupRegistry> markupRegistry;
    private final Optional<BotChatStateService> chatStateService;

    /**
     * {@inheritDoc}
     *
     * @return {@link BotHandlerInvocationFilterOrder#MARKUP_APPLICATION}
     */
    @Override
    public int getOrder() {
        return BotHandlerInvocationFilterOrder.MARKUP_APPLICATION;
    }

    /**
     * Applies markup to the return value according to the precedence rules described in the
     * class-level Javadoc, then advances the chain.
     *
     * @param context the mutable invocation context
     * @param chain   the remaining filter chain
     */
    @Override
    public void invoke(BotHandlerInvocationContext context, BotHandlerInvocationChain chain) throws InvocationTargetException, IllegalAccessException {
        Object returnValue = context.getReturnValue();

        if (Objects.nonNull(returnValue)) {
            BotClearMarkup clearMarkup = AnnotationUtils.findAnnotation(context.getMethod(), BotClearMarkup.class);
            BotReplyMarkup replyMarkup = AnnotationUtils.findAnnotation(context.getMethod(), BotReplyMarkup.class);
            BotParseMode parseModeAnnotation = AnnotationUtils.findAnnotation(context.getMethod(), BotParseMode.class);

            if (returnValue instanceof MarkupAware markupAware) {
                if (Objects.nonNull(clearMarkup)) {
                    log.debug("Clearing markup for method '{}'", context.getMethod().getName());
                    context.setReturnValue(markupAware.removeMarkup());
                } else if (Objects.nonNull(replyMarkup)
                        && Objects.isNull(markupAware.getKeyboard())
                        && Objects.isNull(markupAware.getMarkupId())) {
                    log.debug("Applying @BotReplyMarkup '{}' to method '{}'",
                            replyMarkup.value(), context.getMethod().getName());
                    context.setReturnValue(markupAware.withMarkup(replyMarkup.value()));
                } else if (Objects.isNull(markupAware.getKeyboard()) && Objects.isNull(markupAware.getMarkupId())) {
                    ReplyKeyboard stateKeyboard = resolveStateKeyboard(context);
                    if (Objects.nonNull(stateKeyboard)
                            && (!markupAware.isEditMessage() || stateKeyboard instanceof InlineKeyboardMarkup)) {
                        log.debug("Applying state-bound keyboard to method '{}'", context.getMethod().getName());
                        context.setReturnValue(markupAware.withKeyboard(stateKeyboard));
                    }
                }
                // Apply parseMode to whatever MarkupAware is now in context (original or modified)
                if (Objects.nonNull(parseModeAnnotation) && context.getReturnValue() instanceof MarkupAware updated) {
                    context.setReturnValue(updated.withParseMode(parseModeAnnotation.value()));
                }
            } else if (returnValue instanceof String text) {
                if (Objects.nonNull(clearMarkup)) {
                    log.debug("Clearing markup (String return) for method '{}'", context.getMethod().getName());
                    PlainReply plain = PlainReply.of(text).removeMarkup();
                    if (Objects.nonNull(parseModeAnnotation)) plain = (PlainReply) plain.withParseMode(parseModeAnnotation.value());
                    context.setReturnValue(plain);
                } else if (Objects.nonNull(replyMarkup)) {
                    log.debug("Applying @BotReplyMarkup '{}' to String return of method '{}'",
                            replyMarkup.value(), context.getMethod().getName());
                    PlainReply plain = PlainReply.of(text).withMarkup(replyMarkup.value());
                    if (Objects.nonNull(parseModeAnnotation)) plain = (PlainReply) plain.withParseMode(parseModeAnnotation.value());
                    context.setReturnValue(plain);
                } else {
                    ReplyKeyboard stateKeyboard = resolveStateKeyboard(context);
                    if (Objects.nonNull(stateKeyboard)) {
                        log.debug("Applying state-bound keyboard to String return of method '{}'",
                                context.getMethod().getName());
                        PlainReply plain = PlainReply.of(text).withKeyboard(stateKeyboard);
                        if (Objects.nonNull(parseModeAnnotation)) plain = (PlainReply) plain.withParseMode(parseModeAnnotation.value());
                        context.setReturnValue(plain);
                    } else if (Objects.nonNull(parseModeAnnotation)) {
                        // No markup action, but @BotParseMode is present: wrap String in PlainReply to carry parseMode
                        context.setReturnValue(PlainReply.of(text).withParseMode(parseModeAnnotation.value()));
                    }
                }
            }
        }

        chain.proceed(context);
    }

    /**
     * Resolves the state-bound keyboard for the effective next state of the handler.
     *
     * <p>Returns {@code null} if no {@link BotMarkupRegistry} is present, if the
     * handler clears the state, or if no keyboard is registered for the determined state.</p>
     */
    private ReplyKeyboard resolveStateKeyboard(BotHandlerInvocationContext context) {
        return markupRegistry.map(registry -> {
            BotClearChatState clearState =
                    AnnotationUtils.findAnnotation(context.getMethod(), BotClearChatState.class);
            if (Objects.nonNull(clearState)) {
                return null;
            }

            String effectiveState;
            BotForwardChatState forwardState =
                    AnnotationUtils.findAnnotation(context.getMethod(), BotForwardChatState.class);
            if (Objects.nonNull(forwardState)) {
                effectiveState = forwardState.value();
            } else {
                effectiveState = chatStateService
                        .filter(s -> Objects.nonNull(context.getRequest().getChat()))
                        .map(s -> s.getState(context.getRequest().getChat().getId()))
                        .orElse(null);
            }

            if (Objects.isNull(effectiveState)) {
                return null;
            }
            return registry.resolveByState(effectiveState, context.getRequest());
        }).orElse(null);
    }
}
