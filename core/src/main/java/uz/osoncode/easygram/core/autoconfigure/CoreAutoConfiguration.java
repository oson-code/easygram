package uz.osoncode.easygram.core.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.TelegramUrl;

import java.util.Objects;

import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory;
import uz.osoncode.easygram.core.argumentresolver.BotCallbackQueryDataArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotMessageArgumentResolver;
import uz.osoncode.easygram.core.markup.BotMarkupFactory;
import uz.osoncode.easygram.core.markup.BotMarkupLoader;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.markup.DefaultBotMarkupFactory;
import uz.osoncode.easygram.core.markup.InMemoryBotMarkupRegistry;
import uz.osoncode.easygram.core.returntypehandler.BotPlainReplyReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReplyAction;
import uz.osoncode.easygram.core.returntypehandler.BotReplyActionChain;

import jakarta.validation.Validator;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uz.osoncode.easygram.core.argumentresolver.BotChatArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotCommandArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotCommandQueryParamBotArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotContactArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotLocationArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotMarkupContextArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotRequestArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotResponseArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotTelegramClientArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotTextArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotThrowableArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotUpdateArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotUserArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotMetadataArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotInlineQueryArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotChosenInlineResultArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotShippingQueryArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotPreCheckoutQueryArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotPollArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotPollAnswerArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotChatMemberUpdatedArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotChatJoinRequestArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotBusinessConnectionArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotBusinessMessagesDeletedArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotInlineQueryValueArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotChosenInlineResultIdArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotShippingPayloadArgumentResolver;
import uz.osoncode.easygram.core.argumentresolver.BotPreCheckoutPayloadArgumentResolver;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.EasygramProperties;
import uz.osoncode.easygram.core.bot.EasygramTelegramUrlProperties;
import uz.osoncode.easygram.core.bot.EasygramUpdateProperties;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.exceptionhandler.BotMethodExceptionHandlerLoader;
import uz.osoncode.easygram.core.filter.BotApiMethodsSenderFilter;
import uz.osoncode.easygram.core.filter.BotMdcFilter;
import uz.osoncode.easygram.core.filter.BotContextSetterFilter;
import uz.osoncode.easygram.core.provider.EasygramExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.EasygramObjectMapperProvider;
import uz.osoncode.easygram.core.provider.EasygramOkHttpClientProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramUrlProvider;
import uz.osoncode.easygram.core.handler.BotHandlerConditionContributor;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.handler.BotHandlerRegistry;
import uz.osoncode.easygram.core.handler.BotMethodHandlerFactory;
import uz.osoncode.easygram.core.handler.DefaultBotMethodHandlerFactory;
import uz.osoncode.easygram.core.argumentresolver.BotDynamicCallbackDataArgumentResolver;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackQueryService;
import uz.osoncode.easygram.core.dynamiccallback.InMemoryBotDynamicCallbackQueryService;
import uz.osoncode.easygram.core.handler.callbackquery.metadataresolver.BotCallbackQueryMetaDataResolver;
import uz.osoncode.easygram.core.handler.callbackquery.metadataresolver.BotDefaultCallbackQueryMetaDataResolver;
import uz.osoncode.easygram.core.handler.callbackquery.metadataresolver.BotDynamicCallbackQueryMetaDataResolver;
import uz.osoncode.easygram.core.handler.defaulthandler.metadataresolver.BotDefaultHandlerMetaDataResolver;
import uz.osoncode.easygram.core.handler.invocation.BotHandlerInvocationFilter;
import uz.osoncode.easygram.core.handler.invocation.ChatStateUpdateFilter;
import uz.osoncode.easygram.core.handler.invocation.MarkupApplicationFilter;
import uz.osoncode.easygram.core.handler.invocation.MethodInvocationFilter;
import uz.osoncode.easygram.core.handler.invocation.ReturnTypeDispatchFilter;
import uz.osoncode.easygram.core.handler.message.command.metadataresolver.BotCommandMetaDataResolver;
import uz.osoncode.easygram.core.handler.message.command.metadataresolver.BotDefaultCommandMetaDataResolver;
import uz.osoncode.easygram.core.handler.message.contact.metadataresolver.BotContactMetaDataResolver;
import uz.osoncode.easygram.core.handler.message.location.metadataresolver.BotLocationMetaDataResolver;
import uz.osoncode.easygram.core.handler.editedmessage.metadataresolver.BotEditedMessageMetaDataResolver;
import uz.osoncode.easygram.core.handler.channelpost.metadataresolver.BotChannelPostMetaDataResolver;
import uz.osoncode.easygram.core.handler.editedchannelpost.metadataresolver.BotEditedChannelPostMetaDataResolver;
import uz.osoncode.easygram.core.handler.inlinequery.BotInlineQueryMatcher;
import uz.osoncode.easygram.core.handler.inlinequery.metadataresolver.BotInlineQueryMetaDataResolver;
import uz.osoncode.easygram.core.handler.choseninlineresult.metadataresolver.BotChosenInlineResultMetaDataResolver;
import uz.osoncode.easygram.core.handler.shippingquery.metadataresolver.BotShippingQueryMetaDataResolver;
import uz.osoncode.easygram.core.handler.precheckoutquery.metadataresolver.BotPreCheckoutQueryMetaDataResolver;
import uz.osoncode.easygram.core.handler.poll.metadataresolver.BotPollMetaDataResolver;
import uz.osoncode.easygram.core.handler.pollanswer.metadataresolver.BotPollAnswerMetaDataResolver;
import uz.osoncode.easygram.core.handler.mychatmember.metadataresolver.BotMyChatMemberMetaDataResolver;
import uz.osoncode.easygram.core.handler.chatmember.metadataresolver.BotChatMemberUpdateMetaDataResolver;
import uz.osoncode.easygram.core.handler.chatjoinrequest.metadataresolver.BotChatJoinRequestMetaDataResolver;
import uz.osoncode.easygram.core.handler.businessconnection.metadataresolver.BotBusinessConnectionMetaDataResolver;
import uz.osoncode.easygram.core.handler.businessmessage.metadataresolver.BotBusinessMessageMetaDataResolver;
import uz.osoncode.easygram.core.handler.editedbusinessmessage.metadataresolver.BotEditedBusinessMessageMetaDataResolver;
import uz.osoncode.easygram.core.handler.deletedbusinessmessages.metadataresolver.BotDeletedBusinessMessagesMetaDataResolver;
import uz.osoncode.easygram.core.handler.paidmediapurchased.metadataresolver.BotPaidMediaPurchasedMetaDataResolver;
import uz.osoncode.easygram.core.handler.message.replybutton.BotReplyButtonMatcher;
import uz.osoncode.easygram.core.handler.message.replybutton.metadataresolver.BotReplyButtonMetaDataResolver;
import uz.osoncode.easygram.core.handler.message.text.metadataresolver.BotTextDefaultMetaDataResolver;
import uz.osoncode.easygram.core.handler.message.text.metadataresolver.BotTextMetaDataResolver;
import uz.osoncode.easygram.core.handler.message.text.metadataresolver.BotTextPatternMetaDataResolver;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataDefaultResolver;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolverFactory;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataSpecResolver;
import uz.osoncode.easygram.core.returntypehandler.BotBotApiMethodReturnHandler;
import uz.osoncode.easygram.core.returntypehandler.BotBotApiMethodsReturnHandler;
import uz.osoncode.easygram.core.returntypehandler.BotMixedCollectionReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory;
import uz.osoncode.easygram.core.returntypehandler.BotStringReturnHandler;
import uz.osoncode.easygram.core.returntypehandler.BotVoidReturnHandler;
import uz.osoncode.easygram.core.bot.BotTransportStartupValidator;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationContext;

/**
 * Spring Boot auto-configuration class that registers all core framework beans.
 *
 * <p>Every bean is annotated with {@link ConditionalOnMissingBean} (where applicable) so that
 * consumers of the library can replace any individual component by simply declaring their own
 * bean of the same type in the application context. The following categories of beans are
 * registered:</p>
 *
 * <ul>
 *   <li><strong>Argument resolvers</strong> — resolve method parameters for handler methods
 *       (e.g. {@link BotChatArgumentResolver}, {@link BotCommandArgumentResolver}).</li>
 *   <li><strong>Return type handlers</strong> — convert the value returned by a handler method
 *       into {@code BotApiMethod} entries on the response
 *       (e.g. {@link BotStringReturnHandler}, {@link BotBotApiMethodReturnHandler}).</li>
 *   <li><strong>Metadata resolvers</strong> — map handler method annotations to routing metadata
 *       (e.g. {@link BotCommandMetaDataResolver}, {@link BotTextMetaDataResolver}).</li>
 *   <li><strong>Filters</strong> — pre/post processing of every update
 *       ({@link BotContextSetterFilter}, {@link BotApiMethodsSenderFilter}).</li>
 *   <li><strong>Dispatcher and registries</strong> — core routing infrastructure
 *       ({@link BotDispatcher}, {@link BotHandlerRegistry}, {@link BotExceptionHandlerRegistry}).</li>
 *   <li><strong>Loaders</strong> — scan the application context for handler and exception-handler
 *       beans and register them ({@link BotHandlerLoader}, {@link BotMethodExceptionHandlerLoader}).</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@AutoConfiguration
@EnableConfigurationProperties({EasygramProperties.class, EasygramUpdateProperties.class, EasygramTelegramUrlProperties.class})
public class CoreAutoConfiguration {

    /**
     * Registers the handler registry used to store and look up handler method descriptors.
     *
     * @return a new {@link BotHandlerRegistry} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotHandlerRegistry botHandlerRegistry() {
        return new BotHandlerRegistry();
    }

    /**
     * Registers the exception handler registry used to store and look up exception handler method descriptors.
     *
     * @return a new {@link BotExceptionHandlerRegistry} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotExceptionHandlerRegistry botExceptionHandlerRegistry() {
        return new BotExceptionHandlerRegistry();
    }

    /**
     * Registers the argument resolver that extracts callback query data from the bot request.
     *
     * @return a new {@link BotCallbackQueryDataArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotCallbackQueryDataArgumentResolver.class)
    public BotCallbackQueryDataArgumentResolver botCallbackQueryDataArgumentResolver() {
        return new BotCallbackQueryDataArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects {@link uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackData}
     * into handler method parameters.
     *
     * @return a new {@link BotDynamicCallbackDataArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotDynamicCallbackDataArgumentResolver.class)
    public BotDynamicCallbackDataArgumentResolver botDynamicCallbackDataArgumentResolver() {
        return new BotDynamicCallbackDataArgumentResolver();
    }

    /**
     * Registers the argument resolver that extracts the {@code Chat} object from the bot request.
     *
     * @return a new {@link BotChatArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChatArgumentResolver.class)
    public BotChatArgumentResolver botChatArgumentResolver() {
        return new BotChatArgumentResolver();
    }

    /**
     * Registers the argument resolver that extracts the bot command (e.g. {@code /start}) from the bot request.
     *
     * @return a new {@link BotCommandArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotCommandArgumentResolver.class)
    public BotCommandArgumentResolver botCommandArgumentResolver() {
        return new BotCommandArgumentResolver();
    }

    /**
     * Registers the argument resolver that extracts the {@code Contact} object from the bot request.
     *
     * @return a new {@link BotContactArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotContactArgumentResolver.class)
    public BotContactArgumentResolver botContactArgumentResolver() {
        return new BotContactArgumentResolver();
    }

    /**
     * Registers the argument resolver that extracts the {@code Location} object from the bot request.
     *
     * @return a new {@link BotLocationArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotLocationArgumentResolver.class)
    public BotLocationArgumentResolver botLocationArgumentResolver() {
        return new BotLocationArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the full {@link uz.osoncode.easygram.core.model.BotRequest} into handler methods.
     *
     * @return a new {@link BotRequestArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotRequestArgumentResolver.class)
    public BotRequestArgumentResolver botRequestArgumentResolver() {
        return new BotRequestArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects {@link uz.osoncode.easygram.core.markup.BotMarkupContext}
     * into {@code @BotMarkup} factory methods, providing the runtime parameters passed via
     * {@link uz.osoncode.easygram.core.markup.MarkupAware#withMarkup(String, java.util.Map)}.
     *
     * @return a new {@link BotMarkupContextArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotMarkupContextArgumentResolver.class)
    public BotMarkupContextArgumentResolver botMarkupContextArgumentResolver() {
        return new BotMarkupContextArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the mutable {@link uz.osoncode.easygram.core.model.BotResponse} into handler methods.
     *
     * @return a new {@link BotResponseArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotResponseArgumentResolver.class)
    public BotResponseArgumentResolver botResponseArgumentResolver() {
        return new BotResponseArgumentResolver();
    }

    /**
     * Registers the argument resolver that extracts plain message text from the bot request.
     *
     * @return a new {@link BotTextArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotTextArgumentResolver.class)
    public BotTextArgumentResolver botTextArgumentResolver() {
        return new BotTextArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code TelegramClient} into handler methods.
     *
     * @return a new {@link BotTelegramClientArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotTelegramClientArgumentResolver.class)
    public BotTelegramClientArgumentResolver botTelegramClientArgumentResolver() {
        return new BotTelegramClientArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@link Throwable} into exception handler methods.
     *
     * @return a new {@link BotThrowableArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotThrowableArgumentResolver.class)
    public BotThrowableArgumentResolver botThrowableArgumentResolver() {
        return new BotThrowableArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the raw Telegram {@code Update} into handler methods.
     *
     * @return a new {@link BotUpdateArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotUpdateArgumentResolver.class)
    public BotUpdateArgumentResolver botUpdateArgumentResolver() {
        return new BotUpdateArgumentResolver();
    }


    /**
     * Registers the argument resolver that injects the Telegram {@code Message} into handler methods.
     *
     * @return a new {@link BotMessageArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotMessageArgumentResolver.class)
    public BotMessageArgumentResolver botMessageArgumentResolver() {
        return new BotMessageArgumentResolver();
    }

    /**
     * Registers the argument resolver that extracts the {@code User} object from the bot request.
     *
     * @return a new {@link BotUserArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotUserArgumentResolver.class)
    public BotUserArgumentResolver botUserArgumentResolver() {
        return new BotUserArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code InlineQuery} object.
     *
     * @return a new {@link BotInlineQueryArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotInlineQueryArgumentResolver.class)
    public BotInlineQueryArgumentResolver botInlineQueryArgumentResolver() {
        return new BotInlineQueryArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code ChosenInlineQuery} object.
     *
     * @return a new {@link BotChosenInlineResultArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChosenInlineResultArgumentResolver.class)
    public BotChosenInlineResultArgumentResolver botChosenInlineResultArgumentResolver() {
        return new BotChosenInlineResultArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code ShippingQuery} object.
     *
     * @return a new {@link BotShippingQueryArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotShippingQueryArgumentResolver.class)
    public BotShippingQueryArgumentResolver botShippingQueryArgumentResolver() {
        return new BotShippingQueryArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code PreCheckoutQuery} object.
     *
     * @return a new {@link BotPreCheckoutQueryArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotPreCheckoutQueryArgumentResolver.class)
    public BotPreCheckoutQueryArgumentResolver botPreCheckoutQueryArgumentResolver() {
        return new BotPreCheckoutQueryArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code Poll} object.
     *
     * @return a new {@link BotPollArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotPollArgumentResolver.class)
    public BotPollArgumentResolver botPollArgumentResolver() {
        return new BotPollArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code PollAnswer} object.
     *
     * @return a new {@link BotPollAnswerArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotPollAnswerArgumentResolver.class)
    public BotPollAnswerArgumentResolver botPollAnswerArgumentResolver() {
        return new BotPollAnswerArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code ChatMemberUpdated} object.
     *
     * @return a new {@link BotChatMemberUpdatedArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChatMemberUpdatedArgumentResolver.class)
    public BotChatMemberUpdatedArgumentResolver botChatMemberUpdatedArgumentResolver() {
        return new BotChatMemberUpdatedArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code ChatJoinRequest} object.
     *
     * @return a new {@link BotChatJoinRequestArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChatJoinRequestArgumentResolver.class)
    public BotChatJoinRequestArgumentResolver botChatJoinRequestArgumentResolver() {
        return new BotChatJoinRequestArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code BusinessConnection} object.
     *
     * @return a new {@link BotBusinessConnectionArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotBusinessConnectionArgumentResolver.class)
    public BotBusinessConnectionArgumentResolver botBusinessConnectionArgumentResolver() {
        return new BotBusinessConnectionArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the {@code BusinessMessagesDeleted} object.
     *
     * @return a new {@link BotBusinessMessagesDeletedArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotBusinessMessagesDeletedArgumentResolver.class)
    public BotBusinessMessagesDeletedArgumentResolver botBusinessMessagesDeletedArgumentResolver() {
        return new BotBusinessMessagesDeletedArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the inline query text string.
     *
     * @return a new {@link BotInlineQueryValueArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotInlineQueryValueArgumentResolver.class)
    public BotInlineQueryValueArgumentResolver botInlineQueryValueArgumentResolver() {
        return new BotInlineQueryValueArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the chosen inline result ID string.
     *
     * @return a new {@link BotChosenInlineResultIdArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChosenInlineResultIdArgumentResolver.class)
    public BotChosenInlineResultIdArgumentResolver botChosenInlineResultIdArgumentResolver() {
        return new BotChosenInlineResultIdArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the shipping query invoice payload string.
     *
     * @return a new {@link BotShippingPayloadArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotShippingPayloadArgumentResolver.class)
    public BotShippingPayloadArgumentResolver botShippingPayloadArgumentResolver() {
        return new BotShippingPayloadArgumentResolver();
    }

    /**
     * Registers the argument resolver that injects the pre-checkout query invoice payload string.
     *
     * @return a new {@link BotPreCheckoutPayloadArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotPreCheckoutPayloadArgumentResolver.class)
    public BotPreCheckoutPayloadArgumentResolver botPreCheckoutPayloadArgumentResolver() {
        return new BotPreCheckoutPayloadArgumentResolver();
    }


    /**
     * Registers the factory that selects the appropriate {@link BotArgumentResolver} for each
     * handler method parameter.
     *
     * @param resolvers all {@link BotArgumentResolver} beans discovered in the application context
     * @return a new {@link BotArgumentResolverFactory} wrapping the provided resolvers
     */
    @Bean
    @ConditionalOnMissingBean
    public BotArgumentResolverFactory botArgumentResolverFactory(List<BotArgumentResolver> resolvers) {
        return new BotArgumentResolverFactory(resolvers);
    }

    /**
     * Registers the return type handler that handles {@code void} handler methods (no-op).
     *
     * @return a new {@link BotVoidReturnHandler} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotVoidReturnHandler.class)
    public BotVoidReturnHandler botVoidReturnHandler() {
        return new BotVoidReturnHandler();
    }

    /**
     * Registers the return type handler that wraps a returned {@link String} in a {@code SendMessage}.
     *
     * <p>When a {@link BotMarkupRegistry} bean is present, the handler will resolve and attach
     * markups declared via {@link uz.osoncode.easygram.core.bind.annotation.BotReplyMarkup} or
     * remove the keyboard when {@link uz.osoncode.easygram.core.bind.annotation.BotClearMarkup}
     * is present on the handler method.</p>
     *
     * @param markupRegistry optional markup registry for resolving markup IDs
     * @return a new {@link BotStringReturnHandler} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotStringReturnHandler.class)
    public BotStringReturnHandler botStringReturnHandler(Optional<BotMarkupRegistry> markupRegistry) {
        return new BotStringReturnHandler(markupRegistry);
    }

    /**
     * Registers the return type handler that forwards a single returned {@code BotApiMethod} to the response.
     *
     * @return a new {@link BotBotApiMethodReturnHandler} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotBotApiMethodReturnHandler.class)
    public BotBotApiMethodReturnHandler botBotApiMethodReturnHandler() {
        return new BotBotApiMethodReturnHandler();
    }

    /**
     * Registers the return type handler that forwards a returned collection of {@code BotApiMethod}
     * instances to the response.
     *
     * @return a new {@link BotBotApiMethodsReturnHandler} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotBotApiMethodsReturnHandler.class)
    public BotBotApiMethodsReturnHandler botBotApiMethodsReturnHandler() {
        return new BotBotApiMethodsReturnHandler();
    }

    /**
     * Registers the return type handler that processes heterogeneous collections — each element
     * is dispatched at runtime to the first registered handler whose
     * {@link BotReturnTypeHandler#supportsElement(Object)} returns {@code true}.
     *
     * <p>The handler list is injected lazily to avoid a circular bean-dependency, since
     * {@link BotMixedCollectionReturnTypeHandler} is itself a {@link BotReturnTypeHandler}.</p>
     *
     * @param handlers all {@link BotReturnTypeHandler} beans; injected lazily
     * @return a new {@link BotMixedCollectionReturnTypeHandler} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotMixedCollectionReturnTypeHandler.class)
    public BotMixedCollectionReturnTypeHandler botMixedCollectionReturnTypeHandler(
            @Lazy List<BotReturnTypeHandler> handlers) {
        return new BotMixedCollectionReturnTypeHandler(handlers);
    }

    /**
     * Registers the factory that selects the appropriate {@link BotReturnTypeHandler} for each
     * handler method's return type.
     *
     * @param handlers all {@link BotReturnTypeHandler} beans discovered in the application context
     * @return a new {@link BotReturnTypeHandlerFactory} wrapping the provided handlers
     */
    @Bean
    @ConditionalOnMissingBean
    public BotReturnTypeHandlerFactory botReturnTypeHandlerFactory(List<BotReturnTypeHandler> handlers) {
        return new BotReturnTypeHandlerFactory(handlers);
    }

    /**
     * Registers the metadata resolver that maps {@code @BotCallbackQuery} annotations to routing metadata.
     *
     * @return a new {@link BotCallbackQueryMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotCallbackQueryMetaDataResolver.class)
    public BotCallbackQueryMetaDataResolver botCallbackQueryMetaDataResolver() {
        return new BotCallbackQueryMetaDataResolver();
    }

    /**
     * Registers the default (fallback) metadata resolver for callback query handlers.
     *
     * @return a new {@link BotDefaultCallbackQueryMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotDefaultCallbackQueryMetaDataResolver.class)
    public BotDefaultCallbackQueryMetaDataResolver botDefaultCallbackQueryMetaDataResolver() {
        return new BotDefaultCallbackQueryMetaDataResolver();
    }

    /**
     * Registers the default in-memory implementation of {@link BotDynamicCallbackQueryService}.
     * Replace this bean with a custom implementation (e.g., Redis-backed) to change persistence behaviour.
     *
     * @return a new {@link InMemoryBotDynamicCallbackQueryService} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotDynamicCallbackQueryService.class)
    public InMemoryBotDynamicCallbackQueryService inMemoryBotDynamicCallbackQueryService() {
        return new InMemoryBotDynamicCallbackQueryService();
    }

    /**
     * Registers the metadata resolver that maps {@code @BotDynamicCallbackQuery} annotations
     * to routing metadata by consulting {@link BotDynamicCallbackQueryService}.
     *
     * @param dynamicCallbackQueryService the service used to resolve payloads by callback key
     * @return a new {@link BotDynamicCallbackQueryMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotDynamicCallbackQueryMetaDataResolver.class)
    public BotDynamicCallbackQueryMetaDataResolver botDynamicCallbackQueryMetaDataResolver(
            BotDynamicCallbackQueryService dynamicCallbackQueryService) {
        return new BotDynamicCallbackQueryMetaDataResolver(dynamicCallbackQueryService);
    }

    /**
     * Registers the metadata resolver that maps {@code @BotCommand} annotations to routing metadata.
     *
     * @return a new {@link BotCommandMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotCommandMetaDataResolver.class)
    public BotCommandMetaDataResolver botCommandMetaDataResolver() {
        return new BotCommandMetaDataResolver();
    }

    /**
     * Registers the default (fallback) metadata resolver for command handlers.
     *
     * @return a new {@link BotDefaultCommandMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotDefaultCommandMetaDataResolver.class)
    public BotDefaultCommandMetaDataResolver botDefaultCommandMetaDataResolver() {
        return new BotDefaultCommandMetaDataResolver();
    }

    /**
     * Registers the metadata resolver that maps {@code @BotContact} annotations to routing metadata.
     *
     * @return a new {@link BotContactMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotContactMetaDataResolver.class)
    public BotContactMetaDataResolver botContactMetaDataResolver() {
        return new BotContactMetaDataResolver();
    }

    /**
     * Registers the metadata resolver that maps {@code @BotLocation} annotations to routing metadata.
     *
     * @return a new {@link BotLocationMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotLocationMetaDataResolver.class)
    public BotLocationMetaDataResolver botLocationMetaDataResolver() {
        return new BotLocationMetaDataResolver();
    }

    /**
     * Registers the metadata resolver that maps {@code @BotText} annotations to routing metadata.
     *
     * @return a new {@link BotTextMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotTextMetaDataResolver.class)
    public BotTextMetaDataResolver botTextMetaDataResolver() {
        return new BotTextMetaDataResolver();
    }

    /**
     * Registers the default (fallback) metadata resolver for text message handlers.
     *
     * @return a new {@link BotTextDefaultMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotTextDefaultMetaDataResolver.class)
    public BotTextDefaultMetaDataResolver botTextDefaultMetaDataResolver() {
        return new BotTextDefaultMetaDataResolver();
    }

    /**
     * Registers the metadata resolver that maps {@code @BotTextPattern} annotations to
     * routing metadata using regular-expression matching.
     *
     * <p>The resolver compiles and caches {@link java.util.regex.Pattern} objects so each
     * distinct pattern string is compiled at most once per application lifetime.
     *
     * @return a new {@link BotTextPatternMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotTextPatternMetaDataResolver.class)
    public BotTextPatternMetaDataResolver botTextPatternMetaDataResolver() {
        return new BotTextPatternMetaDataResolver();
    }

    /**
     * Registers the default {@link BotReplyButtonMatcher} that performs exact case-sensitive
     * text comparison between annotation values and the incoming message text.
     *
     * <p>This bean is suppressed when {@code core-i18n} is present on the classpath — in that
     * case {@code BotI18nAutoConfiguration} registers a locale-aware matcher instead.</p>
     *
     * @return a new exact-text {@link BotReplyButtonMatcher}
     */
    @Bean
    @ConditionalOnMissingBean(BotReplyButtonMatcher.class)
    public BotReplyButtonMatcher botReplyButtonMatcher() {
        return (values, request) -> {
            String text = request.getUpdate().getMessage().getText();
            for (String value : values) {
                if (text.equals(value)) return true;
            }
            return false;
        };
    }

    /**
     * Registers the metadata resolver that maps {@link uz.osoncode.easygram.core.bind.annotation.BotReplyButton}
     * annotations to routing metadata, delegating the actual text matching to the configured
     * {@link BotReplyButtonMatcher}.
     *
     * @param matcher the matching strategy to use
     * @return a new {@link BotReplyButtonMetaDataResolver}
     */
    @Bean
    @ConditionalOnMissingBean(BotReplyButtonMetaDataResolver.class)
    public BotReplyButtonMetaDataResolver botReplyButtonMetaDataResolver(BotReplyButtonMatcher matcher) {
        return new BotReplyButtonMetaDataResolver(matcher);
    }

    /**
     * Registers the metadata resolver for the catch-all default handler annotation.
     *
     * @return a new {@link BotDefaultHandlerMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotDefaultHandlerMetaDataResolver.class)
    public BotDefaultHandlerMetaDataResolver botDefaultHandlerMetaDataResolver() {
        return new BotDefaultHandlerMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotEditedMessage} annotation.
     *
     * @return a new {@link BotEditedMessageMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotEditedMessageMetaDataResolver.class)
    public BotEditedMessageMetaDataResolver botEditedMessageMetaDataResolver() {
        return new BotEditedMessageMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotChannelPost} annotation.
     *
     * @return a new {@link BotChannelPostMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChannelPostMetaDataResolver.class)
    public BotChannelPostMetaDataResolver botChannelPostMetaDataResolver() {
        return new BotChannelPostMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotEditedChannelPost} annotation.
     *
     * @return a new {@link BotEditedChannelPostMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotEditedChannelPostMetaDataResolver.class)
    public BotEditedChannelPostMetaDataResolver botEditedChannelPostMetaDataResolver() {
        return new BotEditedChannelPostMetaDataResolver();
    }

    /**
     * Registers the default {@link BotInlineQueryMatcher} that performs exact case-sensitive
     * comparison between annotation values and the incoming inline query text.
     *
     * <p>This bean is suppressed when {@code core-i18n} is present on the classpath — in that
     * case {@code BotI18nAutoConfiguration} registers a locale-aware matcher instead.</p>
     *
     * @return a new exact-text {@link BotInlineQueryMatcher}
     */
    @Bean
    @ConditionalOnMissingBean(BotInlineQueryMatcher.class)
    public BotInlineQueryMatcher botInlineQueryMatcher() {
        return (values, request) -> {
            String queryText = request.getUpdate().getInlineQuery().getQuery();
            for (String value : values) {
                if (Objects.equals(value, queryText)) return true;
            }
            return false;
        };
    }

    /**
     * Registers the metadata resolver for the {@code @BotInlineQuery} annotation.
     *
     * @param matcher the matching strategy to use
     * @return a new {@link BotInlineQueryMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotInlineQueryMetaDataResolver.class)
    public BotInlineQueryMetaDataResolver botInlineQueryMetaDataResolver(BotInlineQueryMatcher matcher) {
        return new BotInlineQueryMetaDataResolver(matcher);
    }

    /**
     * Registers the metadata resolver for the {@code @BotChosenInlineResult} annotation.
     *
     * @return a new {@link BotChosenInlineResultMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChosenInlineResultMetaDataResolver.class)
    public BotChosenInlineResultMetaDataResolver botChosenInlineResultMetaDataResolver() {
        return new BotChosenInlineResultMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotShippingQuery} annotation.
     *
     * @return a new {@link BotShippingQueryMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotShippingQueryMetaDataResolver.class)
    public BotShippingQueryMetaDataResolver botShippingQueryMetaDataResolver() {
        return new BotShippingQueryMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotPreCheckoutQuery} annotation.
     *
     * @return a new {@link BotPreCheckoutQueryMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotPreCheckoutQueryMetaDataResolver.class)
    public BotPreCheckoutQueryMetaDataResolver botPreCheckoutQueryMetaDataResolver() {
        return new BotPreCheckoutQueryMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotPoll} annotation.
     *
     * @return a new {@link BotPollMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotPollMetaDataResolver.class)
    public BotPollMetaDataResolver botPollMetaDataResolver() {
        return new BotPollMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotPollAnswer} annotation.
     *
     * @return a new {@link BotPollAnswerMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotPollAnswerMetaDataResolver.class)
    public BotPollAnswerMetaDataResolver botPollAnswerMetaDataResolver() {
        return new BotPollAnswerMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotMyChatMember} annotation.
     *
     * @return a new {@link BotMyChatMemberMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotMyChatMemberMetaDataResolver.class)
    public BotMyChatMemberMetaDataResolver botMyChatMemberMetaDataResolver() {
        return new BotMyChatMemberMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotChatMemberUpdate} annotation.
     *
     * @return a new {@link BotChatMemberUpdateMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChatMemberUpdateMetaDataResolver.class)
    public BotChatMemberUpdateMetaDataResolver botChatMemberUpdateMetaDataResolver() {
        return new BotChatMemberUpdateMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotChatJoinRequest} annotation.
     *
     * @return a new {@link BotChatJoinRequestMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotChatJoinRequestMetaDataResolver.class)
    public BotChatJoinRequestMetaDataResolver botChatJoinRequestMetaDataResolver() {
        return new BotChatJoinRequestMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotBusinessConnection} annotation.
     *
     * @return a new {@link BotBusinessConnectionMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotBusinessConnectionMetaDataResolver.class)
    public BotBusinessConnectionMetaDataResolver botBusinessConnectionMetaDataResolver() {
        return new BotBusinessConnectionMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotBusinessMessage} annotation.
     *
     * @return a new {@link BotBusinessMessageMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotBusinessMessageMetaDataResolver.class)
    public BotBusinessMessageMetaDataResolver botBusinessMessageMetaDataResolver() {
        return new BotBusinessMessageMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotEditedBusinessMessage} annotation.
     *
     * @return a new {@link BotEditedBusinessMessageMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotEditedBusinessMessageMetaDataResolver.class)
    public BotEditedBusinessMessageMetaDataResolver botEditedBusinessMessageMetaDataResolver() {
        return new BotEditedBusinessMessageMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotDeletedBusinessMessages} annotation.
     *
     * @return a new {@link BotDeletedBusinessMessagesMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotDeletedBusinessMessagesMetaDataResolver.class)
    public BotDeletedBusinessMessagesMetaDataResolver botDeletedBusinessMessagesMetaDataResolver() {
        return new BotDeletedBusinessMessagesMetaDataResolver();
    }

    /**
     * Registers the metadata resolver for the {@code @BotPaidMediaPurchased} annotation.
     *
     * @return a new {@link BotPaidMediaPurchasedMetaDataResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotPaidMediaPurchasedMetaDataResolver.class)
    public BotPaidMediaPurchasedMetaDataResolver botPaidMediaPurchasedMetaDataResolver() {
        return new BotPaidMediaPurchasedMetaDataResolver();
    }


    /**
     * Registers the argument resolver that extracts query parameters appended to a bot command
     * (e.g. the {@code ref} part of {@code /start?ref=abc}).
     *
     * @param botConfigurer the configurer that provides the {@code ObjectMapper} used for parameter parsing
     * @return a new {@link BotCommandQueryParamBotArgumentResolver} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotCommandQueryParamBotArgumentResolver botCommandQueryParamBotArgumentResolver(BotConfigurer botConfigurer) {
        return new BotCommandQueryParamBotArgumentResolver(botConfigurer);
    }

    /**
     * Registers the factory that selects the appropriate metadata resolver for each handler
     * method annotation during handler loading.
     *
     * @param specResolvers    all {@link BotMetaDataSpecResolver} beans for specific annotations
     * @param defaultResolvers all {@link BotMetaDataDefaultResolver} beans for default/fallback annotations
     * @return a new {@link BotMetaDataResolverFactory} wrapping the provided resolvers
     */
    @Bean
    @ConditionalOnMissingBean
    public BotMetaDataResolverFactory botMetaDataResolverFactory(
            List<BotMetaDataSpecResolver<? extends Annotation>> specResolvers,
            List<BotMetaDataDefaultResolver<? extends Annotation>> defaultResolvers) {
        return new BotMetaDataResolverFactory(specResolvers, defaultResolvers);
    }

    /**
     * Registers the MDC correlation filter that sets {@code bot.update.id},
     * {@code bot.transport}, {@code bot.user.id}, and {@code bot.chat.id} keys in the
     * Mapped Diagnostic Context for every incoming update, enabling correlated log output
     * across the entire processing pipeline.
     *
     * @param botConfigurer provides the active transport type for the {@code bot.transport} key
     * @return a new {@link BotMdcFilter} instance
     * @since 0.0.4
     */
    @Bean
    @ConditionalOnMissingBean
    public BotMdcFilter botMdcFilter(BotConfigurer botConfigurer) {
        return new BotMdcFilter(botConfigurer);
    }

    /**
     * Registers the filter that populates the thread-local bot context before handler invocation.
     *
     * @return a new {@link BotContextSetterFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotContextSetterFilter botContextSetterFilter() {
        return new BotContextSetterFilter();
    }

    /**
     * Registers the filter that sends all {@code BotApiMethod} entries accumulated in
     * {@link uz.osoncode.easygram.core.model.BotResponse} to the Telegram API after
     * handler invocation.
     *
     * @return a new {@link BotApiMethodsSenderFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotApiMethodsSenderFilter botApiMethodsSenderFilter() {
        return new BotApiMethodsSenderFilter();
    }

    /**
     * Registers the central dispatcher responsible for routing incoming updates to the
     * appropriate handler method.
     *
     * @param botHandlerRegistry the registry containing all registered handler descriptors
     * @return a new {@link BotDispatcher} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotDispatcher botDispatcher(BotHandlerRegistry botHandlerRegistry) {
        return new BotDispatcher(botHandlerRegistry);
    }

    /**
     * Registers the {@code MethodInvocationFilter} — first step in the handler invocation pipeline.
     * Resolves arguments, optionally validates them against Jakarta Bean Validation constraints,
     * and invokes the controller method reflectively.
     *
     * <p>When a {@link Validator} bean is present on the application context (e.g. because
     * {@code spring-boot-starter-validation} is on the classpath), resolved arguments are
     * validated before the handler method is called. Any constraint violations cause a
     * {@link jakarta.validation.ConstraintViolationException} to be thrown, which can be
     * caught by an {@code @BotExceptionHandler(ConstraintViolationException.class)} method.
     * When no {@code Validator} is present, validation is silently skipped.</p>
     *
     * @param botArgumentResolverFactory factory for resolving handler method parameters
     * @param validator                  optional Jakarta {@link Validator} for parameter validation
     * @return a new {@link MethodInvocationFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean(MethodInvocationFilter.class)
    public MethodInvocationFilter methodInvocationFilter(BotArgumentResolverFactory botArgumentResolverFactory,
                                                         Optional<Validator> validator) {
        return new MethodInvocationFilter(botArgumentResolverFactory, validator);
    }

    /**
     * Registers the {@code MarkupApplicationFilter} — applies markup to the handler method's
     * return value before dispatch.
     *
     * <p>Markup resolution precedence (highest first):</p>
     * <ol>
     *   <li>{@code @BotClearMarkup} — always removes markup</li>
     *   <li>{@code returnValue.getKeyboard()} — directly attached keyboard</li>
     *   <li>{@code returnValue.getMarkupId()} / {@code @BotReplyMarkup} — registry ID lookup</li>
     *   <li>State-bound keyboard — auto-attached from {@link BotMarkupRegistry} using the
     *       handler's effective next state (read from {@code @BotForwardChatState} or from
     *       {@link uz.osoncode.easygram.core.chatstate.BotChatStateService})</li>
     * </ol>
     *
     * @param markupRegistry   optional registry for state-bound keyboard lookup
     * @param chatStateService optional chat-state service for reading the current state
     * @return a new {@link MarkupApplicationFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean(MarkupApplicationFilter.class)
    public MarkupApplicationFilter markupApplicationFilter(
            Optional<BotMarkupRegistry> markupRegistry,
            Optional<BotChatStateService> chatStateService) {
        return new MarkupApplicationFilter(markupRegistry, chatStateService);
    }

    /**
     * Registers the {@code ReturnTypeDispatchFilter} — selects the appropriate
     * {@link BotReturnTypeHandler} and dispatches the return value.
     *
     * @param botReturnTypeHandlerFactory factory for selecting return-type handlers
     * @return a new {@link ReturnTypeDispatchFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean(ReturnTypeDispatchFilter.class)
    public ReturnTypeDispatchFilter returnTypeDispatchFilter(BotReturnTypeHandlerFactory botReturnTypeHandlerFactory) {
        return new ReturnTypeDispatchFilter(botReturnTypeHandlerFactory);
    }

    /**
     * Registers the {@code ChatStateUpdateFilter} — applies {@code @BotForwardChatState} and
     * {@code @BotClearChatState} after the handler has been dispatched.
     *
     * @param chatStateService optional chat-state service
     * @return a new {@link ChatStateUpdateFilter} instance
     */
    @Bean
    @ConditionalOnMissingBean(ChatStateUpdateFilter.class)
    public ChatStateUpdateFilter chatStateUpdateFilter(Optional<BotChatStateService> chatStateService) {
        return new ChatStateUpdateFilter(chatStateService);
    }

    /**
     * Registers the default {@link BotMethodHandlerFactory} responsible for constructing
     * {@link uz.osoncode.easygram.core.handler.BotMethodHandler} instances during handler loading.
     *
     * <p>The factory encapsulates condition building ({@link uz.osoncode.easygram.core.handler.BotHandlerCondition}
     * composition), invocation filter wiring, and {@code BotMethodHandler} instantiation.
     * Override this bean to customise how handler instances are created without touching
     * {@link BotHandlerLoader}.</p>
     *
     * @param chatStateService      optional chat state service used by
     *                              {@link uz.osoncode.easygram.core.handler.BotChatStateCondition}
     * @param invocationFilters     ordered list of invocation filters passed to every handler
     * @param conditionContributors optional condition contributors extending handler matching
     * @return a new {@link DefaultBotMethodHandlerFactory} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotMethodHandlerFactory.class)
    public BotMethodHandlerFactory botMethodHandlerFactory(
            Optional<BotChatStateService> chatStateService,
            List<BotHandlerInvocationFilter> invocationFilters,
            List<BotHandlerConditionContributor> conditionContributors) {
        return new DefaultBotMethodHandlerFactory(chatStateService, invocationFilters, conditionContributors);
    }

    /**
     * Registers the handler loader that scans the application context for {@code @BotController}
     * beans and registers their handler methods in the {@link BotHandlerRegistry}.
     *
     * <p>Handler construction is fully delegated to {@link BotMethodHandlerFactory}.
     * Override that bean to customise how handlers are built, or override this bean to
     * customise how handlers are scanned and registered.</p>
     *
     * @param applicationContext         the Spring application context used to discover handler beans
     * @param botHandlerRegistry         registry where discovered handlers are stored
     * @param botMetaDataResolverFactory factory for resolving handler method routing metadata
     * @param botMethodHandlerFactory    factory for constructing each {@link uz.osoncode.easygram.core.handler.BotMethodHandler}
     * @return a new {@link BotHandlerLoader} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotHandlerLoader botHandlerLoader(
            ApplicationContext applicationContext,
            BotHandlerRegistry botHandlerRegistry,
            BotMetaDataResolverFactory botMetaDataResolverFactory,
            BotMethodHandlerFactory botMethodHandlerFactory) {
        return new BotHandlerLoader(applicationContext, botHandlerRegistry,
                botMetaDataResolverFactory, botMethodHandlerFactory);
    }

    /**
     * Registers the exception handler loader that scans the application context for
     * {@code @BotController} and {@code @BotControllerAdvice} beans and registers their
     * exception handler methods in the {@link BotExceptionHandlerRegistry}.
     *
     * <p>The optional {@link BotChatStateService} is injected so that exception handlers
     * annotated with {@link uz.osoncode.easygram.core.chatstate.BotChatState} are only
     * selected when the current chat is in the required state.</p>
     *
     * @param applicationContext          the Spring application context used to discover exception handler beans
     * @param botArgumentResolverFactory  factory for resolving exception handler method parameters
     * @param botExceptionHandlerRegistry registry where discovered exception handlers are stored
     * @param botReturnTypeHandlerFactory factory for resolving exception handler method return types
     * @param chatStateService            optional chat-state service; empty when the module is absent
     * @return a new {@link BotMethodExceptionHandlerLoader} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotMethodExceptionHandlerLoader botMethodExceptionHandlerLoader(
            ApplicationContext applicationContext,
            BotArgumentResolverFactory botArgumentResolverFactory,
            BotExceptionHandlerRegistry botExceptionHandlerRegistry,
            BotReturnTypeHandlerFactory botReturnTypeHandlerFactory,
            Optional<BotChatStateService> chatStateService,
            MarkupApplicationFilter markupApplicationFilter) {
        return new BotMethodExceptionHandlerLoader(applicationContext, botArgumentResolverFactory,
                botExceptionHandlerRegistry, botReturnTypeHandlerFactory, chatStateService, markupApplicationFilter);
    }


    /**
     * Registers a shared {@link BotConfigurer} backed by the application {@link ObjectMapper} and
     * the configured transport from {@link EasygramProperties}.
     *
     * <p>Consumers can override by declaring their own {@code BotConfigurer} bean.</p>
     *
     * @param botObjectMapperProvider provider for the shared {@link ObjectMapper}
     * @param botUpdateProperties     bot update configuration containing the transport type
     * @return a {@link BotConfigurer} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BotConfigurer botConfigurer(
            EasygramObjectMapperProvider botObjectMapperProvider,
            EasygramUpdateProperties botUpdateProperties
    ) {
        return new BotConfigurer(botObjectMapperProvider.provide(), botUpdateProperties.transport());
    }

    /**
     * Registers a shared {@link ObjectMapper} for use in argument resolution, return type handling,
     * and as the fallback value for {@link EasygramObjectMapperProvider}.
     *
     * @return a default {@link ObjectMapper} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // Infrastructure provider beans
    // -------------------------------------------------------------------------

    /**
     * Default {@link EasygramObjectMapperProvider} that wraps the {@link ObjectMapper} already
     * present in the application context (either framework-provided or user-declared).
     *
     * <p>Override this bean to supply a fully customised mapper without having to configure any
     * transport-specific bean.</p>
     *
     * @param objectMapper the {@link ObjectMapper} to wrap
     * @return a {@link EasygramObjectMapperProvider} backed by the given mapper
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramObjectMapperProvider botObjectMapperProvider(ObjectMapper objectMapper) {
        return () -> objectMapper;
    }

    /**
     * {@link EasygramTelegramUrlProvider} driven by {@code easygram.telegram-url.*} properties.
     *
     * <p>When {@code easygram.telegram-url.host} is set a {@link TelegramUrl} is built from the
     * configured {@code schema}, {@code host}, {@code port}, and {@code testServer} values.
     * Any field left blank keeps its default value from the {@link TelegramUrl} no-arg constructor.
     * When no host is configured the provider returns {@link TelegramUrl#DEFAULT_URL}.</p>
     *
     * <p>Declare your own {@code @Bean EasygramTelegramUrlProvider} to override this entirely.</p>
     *
     * @param telegramUrlProperties optional URL properties from {@code easygram.telegram-url}
     * @return a {@link EasygramTelegramUrlProvider} pointing at the configured or default URL
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramTelegramUrlProvider botTelegramUrlProvider(EasygramTelegramUrlProperties telegramUrlProperties) {
        if (telegramUrlProperties.host() != null) {
            TelegramUrl url = new TelegramUrl();
            if (telegramUrlProperties.schema() != null) {
                url.setSchema(telegramUrlProperties.schema());
            }
            url.setHost(telegramUrlProperties.host());
            if (telegramUrlProperties.port() != null) {
                url.setPort(telegramUrlProperties.port());
            }
            if (telegramUrlProperties.testServer() != null) {
                url.setTestServer(telegramUrlProperties.testServer());
            }
            return () -> url;
        }
        return () -> TelegramUrl.DEFAULT_URL;
    }

    /**
     * Default {@link EasygramOkHttpClientProvider} backed by a plain {@link OkHttpClient} with
     * default settings.
     *
     * <p>Override this bean to set custom timeouts, interceptors, or TLS configuration.</p>
     *
     * @return a {@link EasygramOkHttpClientProvider} backed by a default {@link OkHttpClient}
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramOkHttpClientProvider botOkHttpClientProvider() {
        OkHttpClient client = new OkHttpClient();
        return () -> client;
    }

    /**
     * Default {@link EasygramExecutorServiceProvider} backed by a bounded thread pool
     * with {@link ThreadPoolExecutor.CallerRunsPolicy} to provide natural backpressure.
     *
     * <p>The pool size is {@code max(2, availableProcessors())} so that every update is processed
     * concurrently on multi-core hosts. Telegram delivers up to 100 updates per long-poll request;
     * a single-threaded executor would serialize all of them and cap throughput to roughly
     * {@code 1000 / handlerLatencyMs} updates per second.</p>
     *
     * <p>The work queue is capped at 500 tasks. When the queue is full the polling thread
     * itself processes the update ({@link ThreadPoolExecutor.CallerRunsPolicy}), preventing
     * unbounded heap growth under sustained high load.</p>
     *
     * <p>Override this bean to supply a different {@link ExecutorService} — for example a virtual-
     * thread executor on JDK 21+, or a pool with custom rejection policy.</p>
     *
     * @return a {@link EasygramExecutorServiceProvider} backed by a bounded thread pool
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramExecutorServiceProvider botExecutorServiceProvider() {
        int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        AtomicInteger counter = new AtomicInteger(1);
        ExecutorService executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(500),
                r -> {
                    Thread t = new Thread(r, "easygram-update-" + counter.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        return () -> executor;
    }

    /**
     * Default {@link EasygramTelegramClientProvider} that builds an
     * {@link OkHttpTelegramClient} from the other provider beans.
     *
     * <p>Override this bean to supply a fully custom {@link org.telegram.telegrambots.meta.generics.TelegramClient}
     * (e.g. a test stub or a client backed by a different HTTP library).</p>
     *
     * @param objectMapperProvider provider for the Jackson {@link ObjectMapper}
     * @param okHttpClientProvider provider for the underlying {@link OkHttpClient}
     * @param telegramUrlProvider  provider for the Telegram API base URL
     * @return a {@link EasygramTelegramClientProvider} that constructs an {@link OkHttpTelegramClient}
     */
    @Bean
    @ConditionalOnMissingBean
    public EasygramTelegramClientProvider botTelegramClientProvider(
            EasygramObjectMapperProvider objectMapperProvider,
            EasygramOkHttpClientProvider okHttpClientProvider,
            EasygramTelegramUrlProvider telegramUrlProvider) {
        return botToken -> new OkHttpTelegramClient(
                objectMapperProvider.provide(),
                okHttpClientProvider.provide(),
                botToken,
                telegramUrlProvider.provide());
    }

    /**
     * Default in-memory markup registry.
     *
     * <p>Stores markup factories (registered via {@code @BotMarkup}) in a concurrent map.</p>
     *
     * @return a new {@link InMemoryBotMarkupRegistry}
     */
    @Bean
    @ConditionalOnMissingBean(BotMarkupRegistry.class)
    public BotMarkupRegistry botMarkupRegistry() {
        return new InMemoryBotMarkupRegistry();
    }

    /**
     * Registers the default {@link BotMarkupFactory} responsible for creating markup factory
     * functions from {@code @BotMarkup}-annotated methods.
     *
     * <p>The factory uses the framework's {@link BotArgumentResolverFactory} to resolve method
     * parameters at request time, enabling arbitrary parameter types in {@code @BotMarkup}
     * methods (e.g. {@code User}, {@code Chat}, {@code Locale}, custom resolver types).</p>
     *
     * <p>Override this bean to customise how markup methods are invoked without modifying
     * {@link BotMarkupLoader}.</p>
     *
     * @param botArgumentResolverFactory factory for resolving markup method parameters
     * @return a new {@link DefaultBotMarkupFactory} instance
     */
    @Bean
    @ConditionalOnMissingBean(BotMarkupFactory.class)
    public BotMarkupFactory botMarkupFactory(BotArgumentResolverFactory botArgumentResolverFactory) {
        return new DefaultBotMarkupFactory(botArgumentResolverFactory);
    }

    /**
     * Application runner that scans for {@code @BotConfiguration} beans and registers
     * their {@code @BotMarkup} methods in the registry.
     *
     * <p>Markup factory creation is fully delegated to {@link BotMarkupFactory}. Override
     * that bean to customise how markup methods are invoked, or override this bean to
     * customise how they are scanned and registered.</p>
     *
     * @param applicationContext the spring context to scan
     * @param registry           the registry to populate
     * @param botMarkupFactory   factory for creating markup factory functions
     * @return a {@link BotMarkupLoader}
     */
    @Bean
    @ConditionalOnMissingBean(BotMarkupLoader.class)
    public BotMarkupLoader botMarkupLoader(ApplicationContext applicationContext,
                                           BotMarkupRegistry registry,
                                           BotMarkupFactory botMarkupFactory) {
        return new BotMarkupLoader(applicationContext, registry, botMarkupFactory);
    }

    /**
     * Handles send-message dispatch for {@link uz.osoncode.easygram.core.reply.PlainReply}.
     *
     * @param markupRegistry optional registry for resolving markup IDs
     * @return a {@link uz.osoncode.easygram.core.returntypehandler.action.SendMessageReplyAction}
     * @since 0.0.6
     */
    @Bean
    @ConditionalOnMissingBean(uz.osoncode.easygram.core.returntypehandler.action.SendMessageReplyAction.class)
    public uz.osoncode.easygram.core.returntypehandler.action.SendMessageReplyAction sendMessageReplyAction(
            Optional<BotMarkupRegistry> markupRegistry) {
        return new uz.osoncode.easygram.core.returntypehandler.action.SendMessageReplyAction(markupRegistry);
    }

    /**
     * Handles edit-message dispatch for {@link uz.osoncode.easygram.core.reply.PlainReply}.
     *
     * @param markupRegistry optional registry for resolving markup IDs
     * @return an {@link uz.osoncode.easygram.core.returntypehandler.action.EditMessageReplyAction}
     * @since 0.0.6
     */
    @Bean
    @ConditionalOnMissingBean(uz.osoncode.easygram.core.returntypehandler.action.EditMessageReplyAction.class)
    public uz.osoncode.easygram.core.returntypehandler.action.EditMessageReplyAction editMessageReplyAction(
            Optional<BotMarkupRegistry> markupRegistry) {
        return new uz.osoncode.easygram.core.returntypehandler.action.EditMessageReplyAction(markupRegistry);
    }

    /**
     * Handles {@code answerCallbackQuery} dispatch for reply types.
     *
     * @return an {@link uz.osoncode.easygram.core.returntypehandler.action.AnswerCallbackQueryReplyAction}
     * @since 0.0.6
     */
    @Bean
    @ConditionalOnMissingBean(uz.osoncode.easygram.core.returntypehandler.action.AnswerCallbackQueryReplyAction.class)
    public uz.osoncode.easygram.core.returntypehandler.action.AnswerCallbackQueryReplyAction answerCallbackQueryReplyAction() {
        return new uz.osoncode.easygram.core.returntypehandler.action.AnswerCallbackQueryReplyAction();
    }

    /**
     * Assembles the {@link BotReplyActionChain} from all registered {@link BotReplyAction} beans.
     *
     * @param actions all {@link BotReplyAction} beans registered in the context
     * @return a {@link BotReplyActionChain} sorted by order
     * @since 0.0.6
     */
    @Bean
    @ConditionalOnMissingBean
    public BotReplyActionChain botReplyActionChain(List<BotReplyAction> actions) {
        return new BotReplyActionChain(actions);
    }

    /**
     * Return-type handler for {@link uz.osoncode.easygram.core.reply.PlainReply}.
     *
     * <p>Handles simple text replies and optionally attaches markups via ID.</p>
     *
     * @param replyActionChain the chain of reply actions to use for dispatch
     * @return a {@link BotPlainReplyReturnTypeHandler}
     */
    @Bean
    @ConditionalOnMissingBean(BotPlainReplyReturnTypeHandler.class)
    public BotPlainReplyReturnTypeHandler botPlainReplyReturnTypeHandler(BotReplyActionChain replyActionChain) {
        return new BotPlainReplyReturnTypeHandler(replyActionChain);
    }

    /**
     * Registers the startup validator that verifies the configured update transport is
     * consistent — for example, that a broker consumer library is present when
     * {@code KAFKA_CONSUMER} or {@code RABBIT_CONSUMER} is set.
     *
     * @param updateProperties  properties carrying the configured transport type
     * @param applicationContext the Spring context used to verify broker consumer beans
     * @return a new {@link BotTransportStartupValidator}
     * @since 0.0.7
     */
    @Bean
    @ConditionalOnMissingBean
    public BotTransportStartupValidator botTransportStartupValidator(
            EasygramUpdateProperties updateProperties,
            ApplicationContext applicationContext) {
        return new BotTransportStartupValidator(updateProperties, applicationContext);
    }
}

