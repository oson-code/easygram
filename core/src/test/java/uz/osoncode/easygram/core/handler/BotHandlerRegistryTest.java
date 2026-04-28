package uz.osoncode.easygram.core.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;

class BotHandlerRegistryTest {

    private BotHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BotHandlerRegistry();
    }

    @Test
    void registerState_addsToStateHandlers() {
        BotHandler h = stubHandler(1, true);
        registry.registerState(h);
        assertThat(registry.getStateHandlers()).containsExactly(h);
        assertThat(registry.getBotHandlers()).isEmpty();
        assertThat(registry.getDefaultHandlers()).isEmpty();
    }

    @Test
    void register_addsToBotHandlers() {
        BotHandler h = stubHandler(1, true);
        registry.register(h);
        assertThat(registry.getBotHandlers()).containsExactly(h);
        assertThat(registry.getStateHandlers()).isEmpty();
        assertThat(registry.getDefaultHandlers()).isEmpty();
    }

    @Test
    void registerDefault_addsToDefaultHandlers() {
        BotHandler h = stubHandler(1, true);
        registry.registerDefault(h);
        assertThat(registry.getDefaultHandlers()).containsExactly(h);
        assertThat(registry.getStateHandlers()).isEmpty();
        assertThat(registry.getBotHandlers()).isEmpty();
    }

    @Test
    void register_multipleHandlers_sortedByOrder() {
        BotHandler highPriority = stubHandler(10, true);
        BotHandler lowPriority = stubHandler(100, true);
        registry.register(lowPriority);
        registry.register(highPriority);
        assertThat(registry.getBotHandlers()).containsExactly(highPriority, lowPriority);
    }

    @Test
    void registerState_multipleHandlers_sortedByOrder() {
        BotHandler first = stubHandler(1, true);
        BotHandler second = stubHandler(5, true);
        BotHandler third = stubHandler(3, true);
        registry.registerState(second);
        registry.registerState(first);
        registry.registerState(third);
        assertThat(registry.getStateHandlers()).containsExactly(first, third, second);
    }

    @Test
    void registerDefault_multipleHandlers_sortedByOrder() {
        BotHandler a = stubHandler(50, true);
        BotHandler b = stubHandler(10, true);
        registry.registerDefault(a);
        registry.registerDefault(b);
        assertThat(registry.getDefaultHandlers()).containsExactly(b, a);
    }

    @Test
    void threeTiers_areIndependent() {
        BotHandler state = stubHandler(1, true);
        BotHandler specific = stubHandler(1, true);
        BotHandler fallback = stubHandler(1, true);
        registry.registerState(state);
        registry.register(specific);
        registry.registerDefault(fallback);
        assertThat(registry.getStateHandlers()).containsExactly(state);
        assertThat(registry.getBotHandlers()).containsExactly(specific);
        assertThat(registry.getDefaultHandlers()).containsExactly(fallback);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static BotHandler stubHandler(int order, boolean supports) {
        return new BotHandler() {
            @Override
            public int getOrder() { return order; }

            @Override
            public boolean supports(BotRequest botRequest) { return supports; }

            @Override
            public void handle(BotRequest botRequest, BotResponse botResponse)
                    throws InvocationTargetException, IllegalAccessException {}

            @Override
            public String info() { return "stub-order-" + order; }
        };
    }
}
