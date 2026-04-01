package uz.osoncode.easygram.core.handler.invocation;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import uz.osoncode.easygram.core.bind.annotation.BotClearChatState;
import uz.osoncode.easygram.core.bind.annotation.BotForwardChatState;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link BotHandlerInvocationFilter} that transitions the chat state after a handler method
 * has been successfully dispatched.
 *
 * <p>Runs last among the built-in filters
 * (order = {@link BotHandlerInvocationFilterOrder#CHAT_STATE_UPDATE}). It calls
 * {@code chain.proceed(context)} <strong>first</strong> — state transitions happen only
 * after the entire preceding pipeline (argument resolution, invocation, markup, dispatch)
 * has completed without throwing an exception.</p>
 *
 * <p>State transition rules (sourced from method annotations via {@link AnnotationUtils}):</p>
 * <ol>
 *   <li>If {@link BotClearChatState} is present → set state to {@code null}</li>
 *   <li>Else if {@link BotForwardChatState} is present → set state to {@code value()}</li>
 *   <li>If {@link BotChatStateService} is absent or the request has no chat → both annotations
 *       are silently ignored.</li>
 * </ol>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class ChatStateUpdateFilter implements BotHandlerInvocationFilter {

    private final Optional<BotChatStateService> chatStateService;

    /**
     * {@inheritDoc}
     *
     * @return {@link BotHandlerInvocationFilterOrder#CHAT_STATE_UPDATE}
     */
    @Override
    public int getOrder() {
        return BotHandlerInvocationFilterOrder.CHAT_STATE_UPDATE;
    }

    /**
     * Advances the chain first, then applies any chat-state transition annotation.
     *
     * @param context the mutable invocation context
     * @param chain   the remaining filter chain
     */
    @Override
    public void invoke(BotHandlerInvocationContext context, BotHandlerInvocationChain chain) throws InvocationTargetException, IllegalAccessException {
        chain.proceed(context);

        chatStateService.ifPresent(service -> {
            if (Objects.isNull(context.getRequest().getChat())) return;
            long chatId = context.getRequest().getChat().getId();

            BotClearChatState clearAnnotation =
                    AnnotationUtils.findAnnotation(context.getMethod(), BotClearChatState.class);
            if (Objects.nonNull(clearAnnotation)) {
                service.clearState(chatId);
                return;
            }
            BotForwardChatState forwardAnnotation =
                    AnnotationUtils.findAnnotation(context.getMethod(), BotForwardChatState.class);
            if (Objects.nonNull(forwardAnnotation)) {
                service.setState(chatId, forwardAnnotation.value());
            }
        });
    }
}
