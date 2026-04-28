package uz.osoncode.easygram.core.exceptionhandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.osoncode.easygram.core.model.BotRequest;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BotExceptionHandlerRegistryTest {

    private BotExceptionHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BotExceptionHandlerRegistry();
    }

    @Test
    void register_singleHandler_appearsInList() {
        BotExceptionMethodHandler<?> handler = handler(Exception.class, 0);
        registry.register(handler);
        assertThat(registry.getBotHandlers()).containsExactly(handler);
    }

    @Test
    void register_moreSpecificFirst_afterLessSpecific() {
        // IOException is deeper in hierarchy than Exception → should sort first
        BotExceptionMethodHandler<?> exceptionHandler = handler(Exception.class, 0);
        BotExceptionMethodHandler<?> ioHandler = handler(IOException.class, 0);

        registry.register(exceptionHandler);
        registry.register(ioHandler);

        assertThat(registry.getBotHandlers()).containsExactly(ioHandler, exceptionHandler);
    }

    @Test
    void register_equalSpecificity_sortedByPriority() {
        // Both handle IOException — lower priority number (0) wins
        BotExceptionMethodHandler<?> local = handler(IOException.class, 0);
        BotExceptionMethodHandler<?> global = handler(IOException.class, 1);

        registry.register(global);
        registry.register(local);

        assertThat(registry.getBotHandlers()).containsExactly(local, global);
    }

    @Test
    void register_threeHandlers_sortedBySpecificityThenPriority() {
        BotExceptionMethodHandler<?> exHandler = handler(Exception.class, 0);
        BotExceptionMethodHandler<?> rteHandler = handler(RuntimeException.class, 0);
        BotExceptionMethodHandler<?> ioHandler = handler(IOException.class, 0);

        registry.register(exHandler);
        registry.register(rteHandler);
        registry.register(ioHandler);

        // Exception must be last (least specific)
        List<BotExceptionMethodHandler<?>> handlers = registry.getBotHandlers();
        assertThat(handlers).hasSize(3);
        assertThat(handlers.get(handlers.size() - 1)).isSameAs(exHandler);
    }

    @Test
    void emptyRegistry_returnsEmptyList() {
        assertThat(registry.getBotHandlers()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> BotExceptionMethodHandler<T> handler(
            Class<T> type, int priority) {
        BotExceptionMethodHandler<T> m = mock(BotExceptionMethodHandler.class);
        when(m.getExceptionType()).thenReturn(type);
        when(m.getPriority()).thenReturn(priority);
        when(m.supports(org.mockito.ArgumentMatchers.any(Throwable.class),
                org.mockito.ArgumentMatchers.any(BotRequest.class))).thenReturn(true);
        return m;
    }
}
