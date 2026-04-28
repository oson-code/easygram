package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotText;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.BotFilterChain;
import uz.osoncode.easygram.core.filter.DefaultBotFilterChain;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;
import uz.osoncode.easygram.core.stereotype.BotController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BotFilter} in the filter chain.
 */
class BotFilterIntegrationTest {

    private static final AtomicBoolean FILTER_EXECUTED = new AtomicBoolean(false);
    private static final AtomicBoolean HANDLER_EXECUTED = new AtomicBoolean(false);
    private static final AtomicBoolean SHORT_CIRCUIT_FILTER_EXECUTED = new AtomicBoolean(false);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("easygram.token=test-token")
            .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
            .withUserConfiguration(SimpleController.class);

    @Test
    void customFilter_isExecutedBeforeHandler() throws Exception {
        FILTER_EXECUTED.set(false);
        HANDLER_EXECUTED.set(false);

        runner.withBean(BotFilter.class, () -> new TrackingFilter())
                .run(context -> {
                    context.getBean(BotHandlerLoader.class).run(null);
                    dispatch(context, "hi");
                    assertThat(FILTER_EXECUTED.get()).isTrue();
                    assertThat(HANDLER_EXECUTED.get()).isTrue();
                });
    }

    @Test
    void shortCircuitFilter_preventsHandlerExecution() throws Exception {
        HANDLER_EXECUTED.set(false);
        SHORT_CIRCUIT_FILTER_EXECUTED.set(false);

        runner.withBean("shortCircuit", BotFilter.class, () -> new ShortCircuitFilter())
                .run(context -> {
                    context.getBean(BotHandlerLoader.class).run(null);
                    dispatch(context, "hi");
                    assertThat(SHORT_CIRCUIT_FILTER_EXECUTED.get()).isTrue();
                    assertThat(HANDLER_EXECUTED.get()).isFalse();
                });
    }

    @Test
    void filterWithShouldFilterFalse_isSkipped() throws Exception {
        FILTER_EXECUTED.set(false);
        HANDLER_EXECUTED.set(false);

        BotFilter skippedFilter = new BotFilter() {
            @Override
            public void doFilter(BotRequest req, BotResponse res, BotFilterChain chain) {
                FILTER_EXECUTED.set(true);
                chain.doFilter(req, res);
            }

            @Override
            public boolean shouldFilter(BotRequest req, BotResponse res) {
                return false;  // always skip
            }
        };

        runner.withBean(BotFilter.class, () -> skippedFilter)
                .run(context -> {
                    context.getBean(BotHandlerLoader.class).run(null);
                    dispatch(context, "hi");
                    assertThat(FILTER_EXECUTED.get()).isFalse();
                    assertThat(HANDLER_EXECUTED.get()).isTrue();
                });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void dispatch(
            org.springframework.context.ApplicationContext ctx, String text) throws Exception {
        List<BotFilter> filters = new ArrayList<>(ctx.getBeansOfType(BotFilter.class).values());
        BotDispatcher dispatcher = ctx.getBean(BotDispatcher.class);
        BotExceptionHandlerRegistry exReg = ctx.getBean(BotExceptionHandlerRegistry.class);
        DefaultBotFilterChain chain = new DefaultBotFilterChain(filters, dispatcher, exReg);
        Update update = BotCommandRoutingIntegrationTest.buildMessageUpdate(text, 1L);
        BotRequest request = new BotRequest();
        request.setUpdate(update);
        chain.doFilter(request, new BotResponse());
    }

    // ── test filters ──────────────────────────────────────────────────────────

    public static class TrackingFilter implements BotFilter {
        @Override
        public void doFilter(BotRequest req, BotResponse res, BotFilterChain chain) {
            FILTER_EXECUTED.set(true);
            chain.doFilter(req, res);
        }
    }

    public static class ShortCircuitFilter implements BotFilter {
        @Override
        public void doFilter(BotRequest req, BotResponse res, BotFilterChain chain) {
            SHORT_CIRCUIT_FILTER_EXECUTED.set(true);
            // intentionally NOT calling chain.doFilter to short-circuit
        }

        @Override
        public int getOrder() {
            return Integer.MIN_VALUE; // execute before other filters
        }
    }

    // ── test controller ───────────────────────────────────────────────────────

    @BotController
    public static class SimpleController {

        @BotText("hi")
        public void onHi() {
            HANDLER_EXECUTED.set(true);
        }
    }
}
