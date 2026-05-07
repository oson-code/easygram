package uz.osoncode.easygram.core.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.TelegramUrl;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory;
import uz.osoncode.easygram.core.bot.BotConfigurer;
import uz.osoncode.easygram.core.bot.BotTransportType;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.dynamiccallback.BotDynamicCallbackQueryService;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.exceptionhandler.BotMethodExceptionHandlerLoader;
import uz.osoncode.easygram.core.filter.BotApiMethodsSenderFilter;
import uz.osoncode.easygram.core.filter.BotContextSetterFilter;
import uz.osoncode.easygram.core.filter.BotMdcFilter;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.handler.BotHandlerRegistry;
import uz.osoncode.easygram.core.handler.BotMethodHandlerFactory;
import uz.osoncode.easygram.core.handler.inlinequery.BotInlineQueryMatcher;
import uz.osoncode.easygram.core.handler.invocation.ChatStateUpdateFilter;
import uz.osoncode.easygram.core.handler.invocation.MarkupApplicationFilter;
import uz.osoncode.easygram.core.handler.invocation.MethodInvocationFilter;
import uz.osoncode.easygram.core.handler.invocation.ReturnTypeDispatchFilter;
import uz.osoncode.easygram.core.handler.metadataresolver.BotMetaDataResolverFactory;
import uz.osoncode.easygram.core.markup.BotMarkupFactory;
import uz.osoncode.easygram.core.markup.BotMarkupRegistry;
import uz.osoncode.easygram.core.provider.EasygramExecutorServiceProvider;
import uz.osoncode.easygram.core.provider.EasygramObjectMapperProvider;
import uz.osoncode.easygram.core.provider.EasygramOkHttpClientProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramClientProvider;
import uz.osoncode.easygram.core.provider.EasygramTelegramUrlProvider;
import uz.osoncode.easygram.core.returntypehandler.BotMixedCollectionReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotPlainReplyReturnTypeHandler;
import uz.osoncode.easygram.core.returntypehandler.BotReturnTypeHandlerFactory;
import uz.osoncode.easygram.core.returntypehandler.BotReplyActionChain;
import uz.osoncode.easygram.core.returntypehandler.action.AnswerCallbackQueryReplyAction;
import uz.osoncode.easygram.core.returntypehandler.action.EditMessageReplyAction;
import uz.osoncode.easygram.core.returntypehandler.action.SendMessageReplyAction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CoreAutoConfiguration}.
 *
 * <p>Verifies both the presence of every auto-configured bean and that each
 * {@code @ConditionalOnMissingBean} bean is properly suppressed when the user
 * provides their own implementation.</p>
 */
class CoreAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=" + BOT_TOKEN)
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class));

    // ── registry beans ────────────────────────────────────────────────────────

    @Test
    void registersHandlerRegistry() {
        runner.run(context -> assertThat(context).hasSingleBean(BotHandlerRegistry.class));
    }

    @Test
    void registersExceptionHandlerRegistry() {
        runner.run(context -> assertThat(context).hasSingleBean(BotExceptionHandlerRegistry.class));
    }

    @Test
    void registersDispatcher() {
        runner.run(context -> assertThat(context).hasSingleBean(BotDispatcher.class));
    }

    @Test
    void registersMarkupRegistry() {
        runner.run(context -> assertThat(context).hasSingleBean(BotMarkupRegistry.class));
    }

    @Test
    void userProvidedDispatcher_suppressesDefault() {
        BotDispatcher custom = new BotDispatcher(new BotHandlerRegistry());
        runner.withBean(BotDispatcher.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotDispatcher.class);
                    assertThat(context.getBean(BotDispatcher.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedHandlerRegistry_suppressesDefault() {
        BotHandlerRegistry custom = new BotHandlerRegistry();
        runner.withBean(BotHandlerRegistry.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotHandlerRegistry.class);
                    assertThat(context.getBean(BotHandlerRegistry.class)).isSameAs(custom);
                });
    }

    @Test
    void userProvidedMarkupRegistry_suppressesDefault() {
        BotMarkupRegistry custom = mock(BotMarkupRegistry.class);
        runner.withBean(BotMarkupRegistry.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotMarkupRegistry.class);
                    assertThat(context.getBean(BotMarkupRegistry.class)).isSameAs(custom);
                });
    }

    // ── URL provider ──────────────────────────────────────────────────────────

    @Test
    void noTelegramUrlProperties_providesDefaultUrl() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(EasygramTelegramUrlProvider.class);
            TelegramUrl url = context.getBean(EasygramTelegramUrlProvider.class).provide();
            assertThat(url).isEqualTo(TelegramUrl.DEFAULT_URL);
        });
    }

    @Test
    void telegramUrlProperties_hostSet_buildsCustomUrl() {
        runner.withPropertyValues(
                        "easygram.telegram-url.host=my-local-bot-api.example.com",
                        "easygram.telegram-url.port=8443",
                        "easygram.telegram-url.schema=https",
                        "easygram.telegram-url.test-server=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramTelegramUrlProvider.class);
                    TelegramUrl url = context.getBean(EasygramTelegramUrlProvider.class).provide();
                    assertThat(url.getHost()).isEqualTo("my-local-bot-api.example.com");
                    assertThat(url.getPort()).isEqualTo(8443);
                    assertThat(url.getSchema()).isEqualTo("https");
                    assertThat(url.isTestServer()).isFalse();
                });
    }

    @Test
    void userProvidedTelegramUrlProvider_suppressesPropertyDriven() {
        TelegramUrl custom = new TelegramUrl("https", "custom.example.com", 9443, false);
        EasygramTelegramUrlProvider customProvider = () -> custom;
        runner.withPropertyValues("easygram.telegram-url.host=should-be-ignored.example.com")
                .withBean(EasygramTelegramUrlProvider.class, () -> customProvider)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramTelegramUrlProvider.class);
                    assertThat(context.getBean(EasygramTelegramUrlProvider.class).provide().getHost())
                            .isEqualTo("custom.example.com");
                });
    }

    // ── infrastructure providers ──────────────────────────────────────────────

    @Test
    void registersObjectMapper() {
        runner.run(context -> assertThat(context).hasSingleBean(ObjectMapper.class));
    }

    @Test
    void userProvidedObjectMapper_suppressesDefault() {
        ObjectMapper custom = new ObjectMapper();
        runner.withBean(ObjectMapper.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(ObjectMapper.class);
                    assertThat(context.getBean(ObjectMapper.class)).isSameAs(custom);
                });
    }

    @Test
    void registersObjectMapperProvider() {
        runner.run(context -> assertThat(context).hasSingleBean(EasygramObjectMapperProvider.class));
    }

    @Test
    void userProvidedObjectMapperProvider_suppressesDefault() {
        EasygramObjectMapperProvider custom = () -> new ObjectMapper();
        runner.withBean(EasygramObjectMapperProvider.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramObjectMapperProvider.class);
                    assertThat(context.getBean(EasygramObjectMapperProvider.class)).isSameAs(custom);
                });
    }

    @Test
    void registersOkHttpClientProvider() {
        runner.run(context -> assertThat(context).hasSingleBean(EasygramOkHttpClientProvider.class));
    }

    @Test
    void userProvidedOkHttpClientProvider_suppressesDefault() {
        EasygramOkHttpClientProvider custom = mock(EasygramOkHttpClientProvider.class);
        runner.withBean(EasygramOkHttpClientProvider.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramOkHttpClientProvider.class);
                    assertThat(context.getBean(EasygramOkHttpClientProvider.class)).isSameAs(custom);
                });
    }

    @Test
    void registersExecutorServiceProvider() {
        runner.run(context -> assertThat(context).hasSingleBean(EasygramExecutorServiceProvider.class));
    }

    @Test
    void userProvidedExecutorServiceProvider_suppressesDefault() {
        EasygramExecutorServiceProvider custom = mock(EasygramExecutorServiceProvider.class);
        runner.withBean(EasygramExecutorServiceProvider.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramExecutorServiceProvider.class);
                    assertThat(context.getBean(EasygramExecutorServiceProvider.class)).isSameAs(custom);
                });
    }

    @Test
    void registersTelegramClientProvider() {
        runner.run(context -> assertThat(context).hasSingleBean(EasygramTelegramClientProvider.class));
    }

    @Test
    void userProvidedTelegramClientProvider_suppressesDefault() {
        EasygramTelegramClientProvider custom = mock(EasygramTelegramClientProvider.class);
        runner.withBean(EasygramTelegramClientProvider.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(EasygramTelegramClientProvider.class);
                    assertThat(context.getBean(EasygramTelegramClientProvider.class)).isSameAs(custom);
                });
    }

    @Test
    void registersBotConfigurer() {
        runner.run(context -> assertThat(context).hasSingleBean(BotConfigurer.class));
    }

    @Test
    void userProvidedBotConfigurer_suppressesDefault() {
        BotConfigurer custom = new BotConfigurer(new ObjectMapper(), BotTransportType.LONG_POLLING);
        runner.withBean(BotConfigurer.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotConfigurer.class);
                    assertThat(context.getBean(BotConfigurer.class)).isSameAs(custom);
                });
    }

    // ── markup ────────────────────────────────────────────────────────────────

    @Test
    void registersMarkupFactory() {
        runner.run(context -> assertThat(context).hasSingleBean(BotMarkupFactory.class));
    }

    @Test
    void userProvidedMarkupFactory_suppressesDefault() {
        BotMarkupFactory custom = mock(BotMarkupFactory.class);
        runner.withBean(BotMarkupFactory.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotMarkupFactory.class);
                    assertThat(context.getBean(BotMarkupFactory.class)).isSameAs(custom);
                });
    }

    // ── filters ───────────────────────────────────────────────────────────────

    @Test
    void registersMdcFilter() {
        runner.run(context -> assertThat(context).hasSingleBean(BotMdcFilter.class));
    }

    @Test
    void userProvidedMdcFilter_suppressesDefault() {
        BotMdcFilter custom = mock(BotMdcFilter.class);
        runner.withBean(BotMdcFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotMdcFilter.class);
                    assertThat(context.getBean(BotMdcFilter.class)).isSameAs(custom);
                });
    }

    @Test
    void registersContextSetterFilter() {
        runner.run(context -> assertThat(context).hasSingleBean(BotContextSetterFilter.class));
    }

    @Test
    void userProvidedContextSetterFilter_suppressesDefault() {
        BotContextSetterFilter custom = mock(BotContextSetterFilter.class);
        runner.withBean(BotContextSetterFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotContextSetterFilter.class);
                    assertThat(context.getBean(BotContextSetterFilter.class)).isSameAs(custom);
                });
    }

    @Test
    void registersApiMethodsSenderFilter() {
        runner.run(context -> assertThat(context).hasSingleBean(BotApiMethodsSenderFilter.class));
    }

    @Test
    void userProvidedApiMethodsSenderFilter_suppressesDefault() {
        BotApiMethodsSenderFilter custom = mock(BotApiMethodsSenderFilter.class);
        runner.withBean(BotApiMethodsSenderFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotApiMethodsSenderFilter.class);
                    assertThat(context.getBean(BotApiMethodsSenderFilter.class)).isSameAs(custom);
                });
    }

    @Test
    void registersMethodInvocationFilter() {
        runner.run(context -> assertThat(context).hasSingleBean(MethodInvocationFilter.class));
    }

    @Test
    void userProvidedMethodInvocationFilter_suppressesDefault() {
        MethodInvocationFilter custom = mock(MethodInvocationFilter.class);
        runner.withBean(MethodInvocationFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(MethodInvocationFilter.class);
                    assertThat(context.getBean(MethodInvocationFilter.class)).isSameAs(custom);
                });
    }

    @Test
    void registersMarkupApplicationFilter() {
        runner.run(context -> assertThat(context).hasSingleBean(MarkupApplicationFilter.class));
    }

    @Test
    void userProvidedMarkupApplicationFilter_suppressesDefault() {
        MarkupApplicationFilter custom = mock(MarkupApplicationFilter.class);
        runner.withBean(MarkupApplicationFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(MarkupApplicationFilter.class);
                    assertThat(context.getBean(MarkupApplicationFilter.class)).isSameAs(custom);
                });
    }

    @Test
    void registersReturnTypeDispatchFilter() {
        runner.run(context -> assertThat(context).hasSingleBean(ReturnTypeDispatchFilter.class));
    }

    @Test
    void userProvidedReturnTypeDispatchFilter_suppressesDefault() {
        ReturnTypeDispatchFilter custom = mock(ReturnTypeDispatchFilter.class);
        runner.withBean(ReturnTypeDispatchFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(ReturnTypeDispatchFilter.class);
                    assertThat(context.getBean(ReturnTypeDispatchFilter.class)).isSameAs(custom);
                });
    }

    @Test
    void registersChatStateUpdateFilter() {
        runner.run(context -> assertThat(context).hasSingleBean(ChatStateUpdateFilter.class));
    }

    @Test
    void userProvidedChatStateUpdateFilter_suppressesDefault() {
        ChatStateUpdateFilter custom = mock(ChatStateUpdateFilter.class);
        runner.withBean(ChatStateUpdateFilter.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatStateUpdateFilter.class);
                    assertThat(context.getBean(ChatStateUpdateFilter.class)).isSameAs(custom);
                });
    }

    // ── factories ─────────────────────────────────────────────────────────────

    @Test
    void registersArgumentResolverFactory() {
        runner.run(context -> assertThat(context).hasSingleBean(BotArgumentResolverFactory.class));
    }

    @Test
    void userProvidedArgumentResolverFactory_suppressesDefault() {
        BotArgumentResolverFactory custom = mock(BotArgumentResolverFactory.class);
        runner.withBean(BotArgumentResolverFactory.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotArgumentResolverFactory.class);
                    assertThat(context.getBean(BotArgumentResolverFactory.class)).isSameAs(custom);
                });
    }

    @Test
    void registersReturnTypeHandlerFactory() {
        runner.run(context -> assertThat(context).hasSingleBean(BotReturnTypeHandlerFactory.class));
    }

    @Test
    void userProvidedReturnTypeHandlerFactory_suppressesDefault() {
        BotReturnTypeHandlerFactory custom = mock(BotReturnTypeHandlerFactory.class);
        runner.withBean(BotReturnTypeHandlerFactory.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotReturnTypeHandlerFactory.class);
                    assertThat(context.getBean(BotReturnTypeHandlerFactory.class)).isSameAs(custom);
                });
    }

    @Test
    void registersMetaDataResolverFactory() {
        runner.run(context -> assertThat(context).hasSingleBean(BotMetaDataResolverFactory.class));
    }

    @Test
    void userProvidedMetaDataResolverFactory_suppressesDefault() {
        BotMetaDataResolverFactory custom = mock(BotMetaDataResolverFactory.class);
        runner.withBean(BotMetaDataResolverFactory.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotMetaDataResolverFactory.class);
                    assertThat(context.getBean(BotMetaDataResolverFactory.class)).isSameAs(custom);
                });
    }

    @Test
    void registersMethodHandlerFactory() {
        runner.run(context -> assertThat(context).hasSingleBean(BotMethodHandlerFactory.class));
    }

    @Test
    void userProvidedMethodHandlerFactory_suppressesDefault() {
        BotMethodHandlerFactory custom = mock(BotMethodHandlerFactory.class);
        runner.withBean(BotMethodHandlerFactory.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotMethodHandlerFactory.class);
                    assertThat(context.getBean(BotMethodHandlerFactory.class)).isSameAs(custom);
                });
    }

    // ── loaders ───────────────────────────────────────────────────────────────

    @Test
    void registersHandlerLoader() {
        runner.run(context -> assertThat(context).hasSingleBean(BotHandlerLoader.class));
    }

    @Test
    void userProvidedHandlerLoader_suppressesDefault() {
        BotHandlerLoader custom = mock(BotHandlerLoader.class);
        runner.withBean(BotHandlerLoader.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotHandlerLoader.class);
                    assertThat(context.getBean(BotHandlerLoader.class)).isSameAs(custom);
                });
    }

    @Test
    void registersExceptionHandlerLoader() {
        runner.run(context -> assertThat(context).hasSingleBean(BotMethodExceptionHandlerLoader.class));
    }

    @Test
    void userProvidedExceptionHandlerLoader_suppressesDefault() {
        BotMethodExceptionHandlerLoader custom = mock(BotMethodExceptionHandlerLoader.class);
        runner.withBean(BotMethodExceptionHandlerLoader.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotMethodExceptionHandlerLoader.class);
                    assertThat(context.getBean(BotMethodExceptionHandlerLoader.class)).isSameAs(custom);
                });
    }

    // ── services ──────────────────────────────────────────────────────────────

    @Test
    void registersDynamicCallbackQueryService() {
        runner.run(context -> assertThat(context).hasSingleBean(BotDynamicCallbackQueryService.class));
    }

    @Test
    void userProvidedDynamicCallbackQueryService_suppressesDefault() {
        BotDynamicCallbackQueryService custom = mock(BotDynamicCallbackQueryService.class);
        runner.withBean(BotDynamicCallbackQueryService.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotDynamicCallbackQueryService.class);
                    assertThat(context.getBean(BotDynamicCallbackQueryService.class)).isSameAs(custom);
                });
    }

    // ── matchers ──────────────────────────────────────────────────────────────

    @Test
    void registersInlineQueryMatcher() {
        runner.run(context -> assertThat(context).hasSingleBean(BotInlineQueryMatcher.class));
    }

    @Test
    void userProvidedInlineQueryMatcher_suppressesDefault() {
        BotInlineQueryMatcher custom = mock(BotInlineQueryMatcher.class);
        runner.withBean(BotInlineQueryMatcher.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotInlineQueryMatcher.class);
                    assertThat(context.getBean(BotInlineQueryMatcher.class)).isSameAs(custom);
                });
    }

    // ── return-type handlers ──────────────────────────────────────────────────

    @Test
    void registersMixedCollectionReturnTypeHandler() {
        runner.run(context -> assertThat(context).hasSingleBean(BotMixedCollectionReturnTypeHandler.class));
    }

    @Test
    void userProvidedMixedCollectionReturnTypeHandler_suppressesDefault() {
        BotMixedCollectionReturnTypeHandler custom = mock(BotMixedCollectionReturnTypeHandler.class);
        runner.withBean(BotMixedCollectionReturnTypeHandler.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotMixedCollectionReturnTypeHandler.class);
                    assertThat(context.getBean(BotMixedCollectionReturnTypeHandler.class)).isSameAs(custom);
                });
    }

    @Test
    void registersPlainReplyReturnTypeHandler() {
        runner.run(context -> assertThat(context).hasSingleBean(BotPlainReplyReturnTypeHandler.class));
    }

    @Test
    void userProvidedPlainReplyReturnTypeHandler_suppressesDefault() {
        BotPlainReplyReturnTypeHandler custom = mock(BotPlainReplyReturnTypeHandler.class);
        runner.withBean(BotPlainReplyReturnTypeHandler.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotPlainReplyReturnTypeHandler.class);
                    assertThat(context.getBean(BotPlainReplyReturnTypeHandler.class)).isSameAs(custom);
                });
    }

    // ── reply actions ─────────────────────────────────────────────────────────

    @Test
    void registersSendMessageReplyAction() {
        runner.run(context -> assertThat(context).hasSingleBean(SendMessageReplyAction.class));
    }

    @Test
    void userProvidedSendMessageReplyAction_suppressesDefault() {
        SendMessageReplyAction custom = mock(SendMessageReplyAction.class);
        runner.withBean(SendMessageReplyAction.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(SendMessageReplyAction.class);
                    assertThat(context.getBean(SendMessageReplyAction.class)).isSameAs(custom);
                });
    }

    @Test
    void registersEditMessageReplyAction() {
        runner.run(context -> assertThat(context).hasSingleBean(EditMessageReplyAction.class));
    }

    @Test
    void userProvidedEditMessageReplyAction_suppressesDefault() {
        EditMessageReplyAction custom = mock(EditMessageReplyAction.class);
        runner.withBean(EditMessageReplyAction.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(EditMessageReplyAction.class);
                    assertThat(context.getBean(EditMessageReplyAction.class)).isSameAs(custom);
                });
    }

    @Test
    void registersAnswerCallbackQueryReplyAction() {
        runner.run(context -> assertThat(context).hasSingleBean(AnswerCallbackQueryReplyAction.class));
    }

    @Test
    void userProvidedAnswerCallbackQueryReplyAction_suppressesDefault() {
        AnswerCallbackQueryReplyAction custom = mock(AnswerCallbackQueryReplyAction.class);
        runner.withBean(AnswerCallbackQueryReplyAction.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(AnswerCallbackQueryReplyAction.class);
                    assertThat(context.getBean(AnswerCallbackQueryReplyAction.class)).isSameAs(custom);
                });
    }

    @Test
    void registersReplyActionChain() {
        runner.run(context -> assertThat(context).hasSingleBean(BotReplyActionChain.class));
    }

    @Test
    void userProvidedReplyActionChain_suppressesDefault() {
        BotReplyActionChain custom = mock(BotReplyActionChain.class);
        runner.withBean(BotReplyActionChain.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotReplyActionChain.class);
                    assertThat(context.getBean(BotReplyActionChain.class)).isSameAs(custom);
                });
    }
}
