package uz.osoncode.easygram.core.dynamiccallback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InMemoryBotDynamicCallbackQueryServiceTest {

    private InMemoryBotDynamicCallbackQueryService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryBotDynamicCallbackQueryService();
    }

    @Test
    void resolve_unknownKey_returnsNull() {
        assertThat(service.resolve("nonexistent")).isNull();
    }

    @Test
    void store_thenResolve_returnsStoredPayload() {
        BotDynamicCallbackData data = mock(BotDynamicCallbackData.class);
        service.store("cb_key", data);
        assertThat(service.resolve("cb_key")).isSameAs(data);
    }

    @Test
    void store_overwriteExisting_replacesValue() {
        BotDynamicCallbackData first = mock(BotDynamicCallbackData.class);
        BotDynamicCallbackData second = mock(BotDynamicCallbackData.class);
        service.store("key", first);
        service.store("key", second);
        assertThat(service.resolve("key")).isSameAs(second);
    }

    @Test
    void remove_existingKey_returnsNullAfter() {
        BotDynamicCallbackData data = mock(BotDynamicCallbackData.class);
        service.store("key", data);
        service.remove("key");
        assertThat(service.resolve("key")).isNull();
    }

    @Test
    void remove_unknownKey_doesNotThrow() {
        service.remove("nonexistent"); // should be no-op
    }

    @Test
    void store_multipleKeys_isolatedCorrectly() {
        BotDynamicCallbackData a = mock(BotDynamicCallbackData.class);
        BotDynamicCallbackData b = mock(BotDynamicCallbackData.class);
        service.store("a", a);
        service.store("b", b);
        assertThat(service.resolve("a")).isSameAs(a);
        assertThat(service.resolve("b")).isSameAs(b);
    }

    @Test
    void remove_oneKey_doesNotAffectOther() {
        BotDynamicCallbackData a = mock(BotDynamicCallbackData.class);
        BotDynamicCallbackData b = mock(BotDynamicCallbackData.class);
        service.store("a", a);
        service.store("b", b);
        service.remove("a");
        assertThat(service.resolve("a")).isNull();
        assertThat(service.resolve("b")).isSameAs(b);
    }
}
