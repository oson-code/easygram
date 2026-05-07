package uz.osoncode.easygram.core.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.autoconfigure.CoreAutoConfiguration;
import uz.osoncode.easygram.core.bind.annotation.BotCommand;
import uz.osoncode.easygram.core.bind.annotation.BotDefaultHandler;
import uz.osoncode.easygram.core.chatstate.BotChatState;
import uz.osoncode.easygram.core.handler.BotHandlerLoader;
import uz.osoncode.easygram.core.stereotype.BotController;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests verifying that {@link BotHandlerLoader} fails fast at startup
 * when two handler methods share the same routing condition (annotation type + value +
 * effective chat state + tier), analogous to Spring MVC rejecting duplicate
 * {@code @RequestMapping} paths.
 *
 * @since 0.0.7
 */
class DuplicateHandlerConditionTest {

    private static final String TOKEN_PROP = "easygram.token=test-token";

    // ── scenario 1: same @BotCommand in the same controller ───────────────────

    @Test
    void duplicateCommand_sameController_throwsBeanCreationException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(SameCommandSameController.class)
                .run(ctx -> {
                    BotHandlerLoader loader = ctx.getBean(BotHandlerLoader.class);
                    assertThatThrownBy(() -> loader.run(null))
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("/start")
                            .hasMessageContaining("Duplicate handler mapping detected");
                });
    }

    // ── scenario 2: same @BotCommand across two controllers ──────────────────

    @Test
    void duplicateCommand_differentControllers_throwsBeanCreationException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(StartCommandControllerA.class, StartCommandControllerB.class)
                .run(ctx -> {
                    BotHandlerLoader loader = ctx.getBean(BotHandlerLoader.class);
                    assertThatThrownBy(() -> loader.run(null))
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("/start")
                            .hasMessageContaining("Duplicate handler mapping detected");
                });
    }

    // ── scenario 3: same command but different @BotChatState — NOT a conflict ─

    @Test
    void sameCommand_differentChatStates_noException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(SameCommandDifferentStates.class)
                .run(ctx -> {
                    BotHandlerLoader loader = ctx.getBean(BotHandlerLoader.class);
                    assertThatNoException().isThrownBy(() -> loader.run(null));
                });
    }

    // ── scenario 4: different command values — NOT a conflict ─────────────────

    @Test
    void differentCommandValues_noException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(DifferentCommands.class)
                .run(ctx -> {
                    BotHandlerLoader loader = ctx.getBean(BotHandlerLoader.class);
                    assertThatNoException().isThrownBy(() -> loader.run(null));
                });
    }

    // ── scenario 5a: two @BotDefaultHandler without state — conflict ──────────

    @Test
    void duplicateDefaultHandler_noState_throwsBeanCreationException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(TwoDefaultHandlersNoState.class)
                .run(ctx -> {
                    BotHandlerLoader loader = ctx.getBean(BotHandlerLoader.class);
                    assertThatThrownBy(() -> loader.run(null))
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("Duplicate handler mapping detected");
                });
    }

    // ── scenario 5b: two @BotDefaultHandler with different states — NOT a conflict

    @Test
    void duplicateDefaultHandler_differentStates_noException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(TwoDefaultHandlersDifferentStates.class)
                .run(ctx -> {
                    BotHandlerLoader loader = ctx.getBean(BotHandlerLoader.class);
                    assertThatNoException().isThrownBy(() -> loader.run(null));
                });
    }

    // ── scenario 5c: two @BotDefaultHandler with same state — conflict ─────────

    @Test
    void duplicateDefaultHandler_sameState_throwsBeanCreationException() {
        new ApplicationContextRunner()
                .withPropertyValues(TOKEN_PROP)
                .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
                .withUserConfiguration(TwoDefaultHandlersSameState.class)
                .run(ctx -> {
                    BotHandlerLoader loader = ctx.getBean(BotHandlerLoader.class);
                    assertThatThrownBy(() -> loader.run(null))
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("Duplicate handler mapping detected");
                });
    }

    // ── test controllers ──────────────────────────────────────────────────────

    @BotController
    public static class SameCommandSameController {

        @BotCommand("/start")
        public void onStartFirst() {}

        @BotCommand("/start")
        public void onStartSecond() {}
    }

    @BotController
    public static class StartCommandControllerA {

        @BotCommand("/start")
        public void onStart() {}
    }

    @BotController
    public static class StartCommandControllerB {

        @BotCommand("/start")
        public void onStart() {}
    }

    @BotController
    public static class SameCommandDifferentStates {

        @BotCommand("/start")
        @BotChatState("STATE_A")
        public void onStartInStateA() {}

        @BotCommand("/start")
        @BotChatState("STATE_B")
        public void onStartInStateB() {}
    }

    @BotController
    public static class DifferentCommands {

        @BotCommand("/start")
        public void onStart() {}

        @BotCommand("/help")
        public void onHelp() {}
    }

    @BotController
    public static class TwoDefaultHandlersNoState {

        @BotDefaultHandler
        public void onDefaultFirst() {}

        @BotDefaultHandler
        public void onDefaultSecond() {}
    }

    @BotController
    public static class TwoDefaultHandlersDifferentStates {

        @BotDefaultHandler
        @BotChatState("WAITING")
        public void onDefaultInWaiting() {}

        @BotDefaultHandler
        @BotChatState("RUNNING")
        public void onDefaultInRunning() {}
    }

    @BotController
    public static class TwoDefaultHandlersSameState {

        @BotDefaultHandler
        @BotChatState("WAITING")
        public void onDefaultFirst() {}

        @BotDefaultHandler
        @BotChatState("WAITING")
        public void onDefaultSecond() {}
    }
}
