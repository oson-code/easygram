package uz.osoncode.easygram.core.returntypehandler;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.osoncode.easygram.core.markup.BotMarkupContext;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Shared utility for building outgoing Telegram API methods from return type handlers.
 *
 * <p>When {@code editMessage} is {@code true} and the current request originates from a
 * callback query, the helper emits:</p>
 * <ol>
 *   <li>{@link EditMessageText} — to update the message text.</li>
 *   <li>{@link EditMessageReplyMarkup} — to update the inline keyboard (when a markup is
 *       present or {@code removeMarkup} is {@code true}).</li>
 * </ol>
 *
 * <p>In all other cases a single {@link SendMessage} is added to the response.</p>
 *
 * <p><b>Keyboard constraint in edit context:</b> only {@link InlineKeyboardMarkup} is
 * supported by {@link EditMessageReplyMarkup}. Non-inline keyboards are silently ignored.
 * When {@code removeMarkup} is {@code true} in edit context, an empty
 * {@link InlineKeyboardMarkup} is used to clear the inline buttons.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.2
 */
@Slf4j
public final class BotReplyMessageHelper {

    private BotReplyMessageHelper() {}

    /**
     * Adds the appropriate Telegram API method(s) to {@code botResponse}.
     *
     * <p>In edit context (editMessage &amp;&amp; hasCallbackQuery) this may add up to two methods:
     * {@link EditMessageText} for text and {@link EditMessageReplyMarkup} for the keyboard.
     * Otherwise a single {@link SendMessage} is added.</p>
     *
     * @param botResponse  the response object to add method(s) to
     * @param botRequest   the current bot request
     * @param text         the resolved message text
     * @param editMessage  when {@code true} and the request has a callback query, edits the original message
     * @param keyboard     the keyboard to attach directly, or {@code null}
     * @param removeMarkup when {@code true}, removes keyboard (or clears inline keyboard in edit context)
     * @param registry     optional markup registry for ID-based keyboard resolution
     * @param markupId     the pre-registered markup ID, or {@code null}
     * @param markupParams parameters forwarded to the markup factory, or {@code null}
     * @param options      delivery options (parse mode, etc.); use {@link SendReplyOptions#NONE} for defaults
     * @since 0.0.7 ({@code options} replaces the former {@code parseMode} parameter)
     */
    public static void addReply(
            BotResponse botResponse,
            BotRequest botRequest,
            String text,
            boolean editMessage,
            ReplyKeyboard keyboard,
            boolean removeMarkup,
            Optional<BotMarkupRegistry> registry,
            String markupId,
            Map<String, Object> markupParams,
            SendReplyOptions options) {

        if (editMessage && botRequest.getUpdate().hasCallbackQuery()) {
            addEditMethods(botResponse, botRequest, text, keyboard, removeMarkup, registry, markupId, markupParams, options);
        } else {
            addSendMessage(botResponse, botRequest, text, keyboard, removeMarkup, registry, markupId, markupParams, options);
        }
    }

    private static void addSendMessage(
            BotResponse botResponse,
            BotRequest botRequest,
            String text,
            ReplyKeyboard keyboard,
            boolean removeMarkup,
            Optional<BotMarkupRegistry> registry,
            String markupId,
            Map<String, Object> markupParams,
            SendReplyOptions options) {

        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(Objects.requireNonNull(botRequest.getChat(),
                        "Cannot send reply: no chat associated with this update").getId())
                .text(text);

        if (Objects.nonNull(options) && Objects.nonNull(options.parseMode())) {
            builder.parseMode(options.parseMode());
        }
        if (Objects.nonNull(options) && Objects.nonNull(options.disableNotification())) {
            builder.disableNotification(options.disableNotification());
        }
        if (Objects.nonNull(options) && Objects.nonNull(options.protectContent())) {
            builder.protectContent(options.protectContent());
        }
        if (Objects.nonNull(options) && Objects.nonNull(options.messageThreadId())) {
            builder.messageThreadId(options.messageThreadId());
        }
        if (Objects.nonNull(options) && Objects.nonNull(options.replyParameters())) {
            builder.replyParameters(options.replyParameters());
        }
        if (Objects.nonNull(options) && Objects.nonNull(options.linkPreviewOptions())) {
            builder.linkPreviewOptions(options.linkPreviewOptions());
        }
        if (removeMarkup) {
            builder.replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
        } else if (Objects.nonNull(keyboard)) {
            builder.replyMarkup(keyboard);
        } else if (Objects.nonNull(markupId)) {
            applyRegistryMarkup(botRequest, registry, markupId, markupParams,
                    markup -> builder.replyMarkup(markup));
        }
        botResponse.addBotApiMethod(builder.build());
    }

    private static void addEditMethods(
            BotResponse botResponse,
            BotRequest botRequest,
            String text,
            ReplyKeyboard keyboard,
            boolean removeMarkup,
            Optional<BotMarkupRegistry> registry,
            String markupId,
            Map<String, Object> markupParams,
            SendReplyOptions options) {

        MaybeInaccessibleMessage original =
                botRequest.getUpdate().getCallbackQuery().getMessage();
        Long chatId = original.getChatId();
        Integer messageId = original.getMessageId();

        // 1. Edit the message text
        EditMessageText.EditMessageTextBuilder<?, ?> editBuilder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text);
        if (Objects.nonNull(options) && Objects.nonNull(options.parseMode())) {
            editBuilder.parseMode(options.parseMode());
        }
        botResponse.addBotApiMethod(editBuilder.build());

        // 2. Update the inline keyboard separately via EditMessageReplyMarkup
        EditMessageReplyMarkup.EditMessageReplyMarkupBuilder markupBuilder =
                EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(messageId);

        if (removeMarkup) {
            markupBuilder.replyMarkup(InlineKeyboardMarkup.builder().build());
            botResponse.addBotApiMethod(markupBuilder.build());
        } else if (keyboard instanceof InlineKeyboardMarkup inlineMarkup) {
            markupBuilder.replyMarkup(inlineMarkup);
            botResponse.addBotApiMethod(markupBuilder.build());
        } else if (Objects.nonNull(markupId)) {
            applyRegistryMarkup(botRequest, registry, markupId, markupParams, markup -> {
                if (markup instanceof InlineKeyboardMarkup inlineMarkup) {
                    markupBuilder.replyMarkup(inlineMarkup);
                    botResponse.addBotApiMethod(markupBuilder.build());
                } else {
                    log.warn("Markup '{}' resolved to {} which is not supported by EditMessageReplyMarkup. " +
                             "Only InlineKeyboardMarkup can be used in edit-message context; keyboard was skipped.",
                            markupId, markup.getClass().getSimpleName());
                }
            });
        }
    }

    private static void applyRegistryMarkup(
            BotRequest botRequest,
            Optional<BotMarkupRegistry> registry,
            String markupId,
            Map<String, Object> markupParams,
            Consumer<ReplyKeyboard> applyFn) {

        registry.ifPresent(r -> {
            if (Objects.nonNull(markupParams)) {
                botRequest.setAttribute(BotMarkupContext.REQUEST_ATTRIBUTE_KEY, BotMarkupContext.of(markupParams));
            }
            try {
                ReplyKeyboard markup = r.resolve(markupId, botRequest);
                if (Objects.nonNull(markup)) {
                    applyFn.accept(markup);
                }
            } finally {
                if (Objects.nonNull(markupParams)) {
                    botRequest.setAttribute(BotMarkupContext.REQUEST_ATTRIBUTE_KEY, null);
                }
            }
        });
    }

    /**
     * Adds an {@link AnswerCallbackQuery} method to the response when the current update
     * originates from a callback query. Silently does nothing if the update is not a callback
     * query, making this call safe to use from any handler context.
     *
     * <p>The {@code text} parameter is used as the popup notification text shown to the user.
     * Pass {@code null} for a silent acknowledgment (spinner is dismissed without a popup).</p>
     *
     * @param botResponse  the response to add the method to; must not be {@code null}
     * @param botRequest   the current request; must not be {@code null}
     * @param text         the notification popup text; {@code null} for silent ack
     * @param showAlert    when {@code true} an alert dialog is shown instead of a toast
     * @param url          optional URL to open; {@code null} if not needed
     * @param cacheTime    client-side cache duration in seconds; {@code null} to use Telegram default
     * @since 0.0.5
     */
    public static void addCallbackAnswer(
            BotResponse botResponse,
            BotRequest botRequest,
            String text,
            boolean showAlert,
            String url,
            Integer cacheTime) {

        if (!botRequest.getUpdate().hasCallbackQuery()) {
            return;
        }
        String callbackQueryId = botRequest.getUpdate().getCallbackQuery().getId();
        AnswerCallbackQuery.AnswerCallbackQueryBuilder<?, ?> builder =
                AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId);
        if (Objects.nonNull(text)) {
            builder.text(text);
        }
        builder.showAlert(showAlert);
        if (Objects.nonNull(url)) {
            builder.url(url);
        }
        if (Objects.nonNull(cacheTime)) {
            builder.cacheTime(cacheTime);
        }
        botResponse.addBotApiMethod(builder.build());
    }
}
