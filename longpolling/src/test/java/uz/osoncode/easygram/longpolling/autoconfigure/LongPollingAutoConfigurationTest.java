package uz.osoncode.easygram.longpolling.autoconfigure;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.provider.BotOkHttpClientProvider;
import uz.osoncode.easygram.core.provider.BotTelegramClientProvider;
import uz.osoncode.easygram.longpolling.LongPollingBot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LongPollingAutoConfiguration}.
 *
 * <p>Both {@link BotTelegramClientProvider} and {@link BotOkHttpClientProvider} are stubbed so
 * that no real HTTP calls are made: {@code Bot.afterPropertiesSet()} gets a fake {@link User}
 * from the mocked {@code TelegramClient}, while {@code BotSession.start()} gets an
 * {@code OkHttpClient} whose interceptor returns {@code {"ok":true,"result":true}} for every
 * request (covering the internal {@code deleteWebhook} call).</p>
 */
class LongPollingAutoConfigurationTest {

    private static final String BOT_TOKEN = "test-token";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=" + BOT_TOKEN)
            .withConfiguration(AutoConfigurations.of(
                    CoreAutoConfiguration.class,
                    LongPollingAutoConfiguration.class
            ))
            .withBean(BotTelegramClientProvider.class, LongPollingAutoConfigurationTest::fakeTelegramClientProvider)
            .withBean(BotOkHttpClientProvider.class, LongPollingAutoConfigurationTest::fakeOkHttpClientProvider);

    @Test
    void defaultTransport_registersLongPollingBot() {
        runner.run(context -> assertThat(context).hasSingleBean(LongPollingBot.class));
    }

    @Test
    void explicitLongPollingTransport_registersLongPollingBot() {
        runner.withPropertyValues("easygram.update.transport=LONG_POLLING")
                .run(context -> assertThat(context).hasSingleBean(LongPollingBot.class));
    }

    @Test
    void webhookTransport_doesNotRegisterLongPollingBot() {
        runner.withPropertyValues("easygram.update.transport=WEBHOOK")
                .run(context -> assertThat(context).doesNotHaveBean(LongPollingBot.class));
    }

    @Test
    void userProvidedLongPollingBot_suppressesDefault() {
        runner.withBean(LongPollingBot.class, () -> mock(LongPollingBot.class))
                .run(context -> assertThat(context).hasSingleBean(LongPollingBot.class));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static BotTelegramClientProvider fakeTelegramClientProvider() {
        TelegramClient client = mock(TelegramClient.class);
        User fakeUser = User.builder()
                .id(123L)
                .firstName("TestBot")
                .userName("test_bot")
                .isBot(true)
                .build();
        try {
            doReturn(fakeUser).when(client).execute(any(GetMe.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return token -> client;
    }

    /** Returns an {@link OkHttpClient} whose interceptor always responds with a Telegram OK body. */
    private static BotOkHttpClientProvider fakeOkHttpClientProvider() {
        OkHttpClient fakeClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create("{\"ok\":true,\"result\":true}",
                                MediaType.get("application/json")))
                        .build())
                .build();
        return () -> fakeClient;
    }
}
