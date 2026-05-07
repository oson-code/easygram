package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotExceptionHandler;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.exceptionhandler.BotMethodExceptionHandlerLoader;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.stereotype.BotController;
import uz.osoncode.easygram.core.stereotype.BotControllerAdvice;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests verifying that {@link BotMethodExceptionHandlerLoader} fails fast at
 * startup when two {@code @BotExceptionHandler} methods share the same exception type and
 * effective chat-state condition within the same priority group.
 *
 * <p>The duplicate check mirrors the behaviour of {@link BotHandlerLoader} for routing
 * annotations: identical conditions → {@link BeanCreationException}; different conditions
 * (e.g. different states) → context loads normally.</p>
 *
 * @since 0.0.8
 */
class DuplicateExceptionHandlerConditionTest {

    private static final String TOKEN_PROP = "easygram.token=test-token";

    // ── scenario 1: two methods in the same @BotController, same exception ───

    @Test
    void duplicateExceptionHandler_sameController_throwsBeanCreationException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(SameExceptionSameController.class)
                .run(ctx -> {
                    BotHandlerLoader handlerLoader = ctx.getBean(BotHandlerLoader.class);
                    handlerLoader.run(null);
                    BotMethodExceptionHandlerLoader exLoader = ctx.getBean(BotMethodExceptionHandlerLoader.class);
                    assertThatThrownBy(() -> exLoader.run(null))
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("IOException")
                            .hasMessageContaining("Duplicate @BotExceptionHandler mapping detected");
                });
    }

    // ── scenario 2: same exception, different chat states — NOT a conflict ───

    @Test
    void sameExceptionDifferentChatStates_noException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(SameExceptionDifferentStates.class)
                .run(ctx -> {
                    BotHandlerLoader handlerLoader = ctx.getBean(BotHandlerLoader.class);
                    handlerLoader.run(null);
                    BotMethodExceptionHandlerLoader exLoader = ctx.getBean(BotMethodExceptionHandlerLoader.class);
                    assertThatNoException().isThrownBy(() -> exLoader.run(null));
                });
    }

    // ── scenario 3: same exception across two @BotController beans ───────────

    @Test
    void duplicateExceptionHandler_differentControllers_throwsBeanCreationException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(IOExceptionControllerA.class, IOExceptionControllerB.class)
                .run(ctx -> {
                    BotHandlerLoader handlerLoader = ctx.getBean(BotHandlerLoader.class);
                    handlerLoader.run(null);
                    BotMethodExceptionHandlerLoader exLoader = ctx.getBean(BotMethodExceptionHandlerLoader.class);
                    assertThatThrownBy(() -> exLoader.run(null))
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("IOException")
                            .hasMessageContaining("Duplicate @BotExceptionHandler mapping detected");
                });
    }

    // ── scenario 4: same exception across two @BotControllerAdvice beans ─────

    @Test
    void duplicateExceptionHandler_twoAdviceBeans_throwsBeanCreationException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(IOExceptionAdviceA.class, IOExceptionAdviceB.class)
                .run(ctx -> {
                    BotHandlerLoader handlerLoader = ctx.getBean(BotHandlerLoader.class);
                    handlerLoader.run(null);
                    BotMethodExceptionHandlerLoader exLoader = ctx.getBean(BotMethodExceptionHandlerLoader.class);
                    assertThatThrownBy(() -> exLoader.run(null))
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("IOException")
                            .hasMessageContaining("Duplicate @BotExceptionHandler mapping detected");
                });
    }

    // ── scenario 5: one @BotController + one @BotControllerAdvice — NOT a conflict

    @Test
    void sameExceptionControllerAndAdvice_noException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(IOExceptionControllerA.class, IOExceptionAdviceA.class)
                .run(ctx -> {
                    BotHandlerLoader handlerLoader = ctx.getBean(BotHandlerLoader.class);
                    handlerLoader.run(null);
                    BotMethodExceptionHandlerLoader exLoader = ctx.getBean(BotMethodExceptionHandlerLoader.class);
                    assertThatNoException().isThrownBy(() -> exLoader.run(null));
                });
    }

    // ── test beans ────────────────────────────────────────────────────────────

    @BotController
    public static class SameExceptionSameController {

        @BotExceptionHandler(IOException.class)
        public void onIOFirst(IOException ex) {}

        @BotExceptionHandler(IOException.class)
        public void onIOSecond(IOException ex) {}
    }

    @BotController
    public static class SameExceptionDifferentStates {

        @BotExceptionHandler(IOException.class)
        @BotChatState("STATE_A")
        public void onIOInStateA(IOException ex) {}

        @BotExceptionHandler(IOException.class)
        @BotChatState("STATE_B")
        public void onIOInStateB(IOException ex) {}
    }

    @BotController
    public static class IOExceptionControllerA {

        @BotExceptionHandler(IOException.class)
        public void onIO(IOException ex) {}
    }

    @BotController
    public static class IOExceptionControllerB {

        @BotExceptionHandler(IOException.class)
        public void onIO(IOException ex) {}
    }

    @BotControllerAdvice
    public static class IOExceptionAdviceA {

        @BotExceptionHandler(IOException.class)
        public void onIO(IOException ex) {}
    }

    @BotControllerAdvice
    public static class IOExceptionAdviceB {

        @BotExceptionHandler(IOException.class)
        public void onIO(IOException ex) {}
    }
}
