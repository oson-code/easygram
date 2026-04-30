package uz.osoncode.easygram.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.Objects;

/**
 * Built-in filter that populates the {@link BotRequest} context with the {@code Chat}
 * and {@code User} objects extracted from the incoming Telegram {@link Update}.
 *
 * <p>This filter runs first in the chain (order {@link BotFilterOrder#CONTEXT_SETTER}) so that
 * all subsequent filters and handlers can rely on {@link BotRequest#getChat()} and
 * {@link BotRequest#getUser()} being set. It inspects every possible update type
 * (messages, callback queries, inline queries, channel posts, business events, etc.)
 * and sets the chat and user where available. Update types whose chat or user cannot
 * be resolved are silently skipped.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Slf4j
public class BotContextSetterFilter implements BotFilter {

    /**
     * Returns the order of this filter.
     *
     * <p>Uses {@link BotFilterOrder#CONTEXT_SETTER} so this filter always runs just after
     * {@link BotMdcFilter}, ensuring the request context is fully populated and MDC
     * update-id is already set.
     *
     * @return {@link BotFilterOrder#CONTEXT_SETTER}.
     */
    @Override
    public int getOrder() {
        return BotFilterOrder.CONTEXT_SETTER;
    }

    /**
     * Extracts the {@code Chat} and {@code User} from the Telegram {@link Update} and
     * stores them on {@code botRequest}, then continues the filter chain.
     *
     * <p>The method checks each known update type in turn. For update types that carry
     * a chat (e.g. message, callback query, edited message), both chat and user are set.
     * For update types that carry only a user (e.g. inline query, shipping query),
     * only the user is set. Update types where neither is available are ignored.
     *
     * @param botRequest  the current bot request; chat and user fields are set by this method.
     * @param botResponse the response object passed through the chain unchanged.
     * @param filterChain the remaining filter chain to invoke after context population.
     */
    @Override
    public void doFilter(BotRequest botRequest, BotResponse botResponse, BotFilterChain filterChain) {
        Update update = botRequest.getUpdate();

        if (update.hasMessage()) {
            botRequest.setChat(update.getMessage().getChat());
            botRequest.setUser(update.getMessage().getFrom());

        } else if (update.hasCallbackQuery()) {
            // getMessage() is null for inline-mode callbacks (inlineMessageId is set instead)
            if (Objects.nonNull(update.getCallbackQuery().getMessage())) {
                botRequest.setChat(update.getCallbackQuery().getMessage().getChat());
            }
            botRequest.setUser(update.getCallbackQuery().getFrom());

        } else if (update.hasEditedMessage()) {
            botRequest.setChat(update.getEditedMessage().getChat());
            botRequest.setUser(update.getEditedMessage().getFrom());

        } else if (update.hasInlineQuery()) {
            // InlineQuery has no Chat object in the Telegram API — only chatType (String)
            botRequest.setUser(update.getInlineQuery().getFrom());

        } else if (update.hasChosenInlineQuery()) {
            // ChosenInlineResult has no Chat object in the Telegram API
            botRequest.setUser(update.getChosenInlineQuery().getFrom());

        } else if (update.hasChannelPost()) {
            botRequest.setChat(update.getChannelPost().getChat());
            botRequest.setUser(update.getChannelPost().getFrom());

        } else if (update.hasEditedChannelPost()) {
            botRequest.setChat(update.getEditedChannelPost().getChat());
            botRequest.setUser(update.getEditedChannelPost().getFrom());

        } else if (update.hasShippingQuery()) {
            // ShippingQuery has no Chat object in the Telegram API
            botRequest.setUser(update.getShippingQuery().getFrom());

        } else if (update.hasPreCheckoutQuery()) {
            // PreCheckoutQuery has no Chat object in the Telegram API
            botRequest.setUser(update.getPreCheckoutQuery().getFrom());

        } else if (update.hasPoll()) {
            // Poll updates carry no user or chat reference

        } else if (update.hasPollAnswer()) {
            // PollAnswer has no Chat object in the Telegram API
            botRequest.setUser(update.getPollAnswer().getUser());

        } else if (update.hasMyChatMember()) {
            botRequest.setChat(update.getMyChatMember().getChat());
            botRequest.setUser(update.getMyChatMember().getFrom());

        } else if (update.hasChatMember()) {
            botRequest.setChat(update.getChatMember().getChat());
            botRequest.setUser(update.getChatMember().getFrom());

        } else if (update.hasChatJoinRequest()) {
            botRequest.setChat(update.getChatJoinRequest().getChat());
            botRequest.setUser(update.getChatJoinRequest().getUser());

        } else if (update.hasBusinessConnection()) {
            // BusinessConnection has userChatId (Long) but no Chat object
            botRequest.setUser(update.getBusinessConnection().getUser());

        } else if (update.hasBusinessMessage()) {
            botRequest.setChat(update.getBusinessMessage().getChat());
            botRequest.setUser(update.getBusinessMessage().getFrom());

        } else if (update.hasEditedBusinessMessage()) {
            // NOTE: telegrambots-meta:9.5.0 upstream typo — the method is spelled
            // 'getEditedBuinessMessage()' ("Buiness" not "Business"). The correctly-spelled
            // variant 'getEditedBusinessMessage()' does not exist on Update.
            botRequest.setChat(update.getEditedBuinessMessage().getChat());
            botRequest.setUser(update.getEditedBuinessMessage().getFrom());

        } else if (update.hasDeletedBusinessMessage()) {
            botRequest.setChat(update.getDeletedBusinessMessages().getChat());
            // DeletedBusinessMessages has no from/user field in the Telegram API

        } else if (update.hasPaidMediaPurchased()) {
            // PaidMediaPurchased has no Chat object in the Telegram API
            botRequest.setUser(update.getPaidMediaPurchased().getUser());
        }

        if (log.isDebugEnabled()) {
            Long chatId = Objects.nonNull(botRequest.getChat()) ? botRequest.getChat().getId() : null;
            Long userId = Objects.nonNull(botRequest.getUser()) ? botRequest.getUser().getId() : null;
            log.debug("Update context resolved: updateId={} chatId={} userId={}",
                    update.getUpdateId(), chatId, userId);
        }

        // Enrich MDC immediately so all downstream filters and handlers see user/chat keys.
        if (Objects.nonNull(botRequest.getUser())) {
            MDC.put(BotMdcFilter.MDC_USER_ID, String.valueOf(botRequest.getUser().getId()));
        }
        if (Objects.nonNull(botRequest.getChat())) {
            MDC.put(BotMdcFilter.MDC_CHAT_ID, String.valueOf(botRequest.getChat().getId()));
        }

        filterChain.doFilter(botRequest, botResponse);
    }
}
