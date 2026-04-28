package uz.osoncode.easygram.core.chatstate.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import uz.osoncode.easygram.core.chatstate.BotChatStateService;
import static org.mockito.Mockito.mock;
import uz.osoncode.easygram.core.chatstate.InMemoryBotChatStateService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChatStateAutoConfiguration}.
 */
class ChatStateAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ChatStateAutoConfiguration.class));

    @Test
    void defaultConfiguration_registersInMemoryService() {
        runner.run(context -> assertThat(context).hasSingleBean(BotChatStateService.class));
    }

    @Test
    void defaultConfiguration_registeredBeanIsInMemory() {
        runner.run(context ->
                assertThat(context.getBean(BotChatStateService.class))
                        .isInstanceOf(InMemoryBotChatStateService.class));
    }

    @Test
    void userProvidedBean_suppressesDefault() {
        BotChatStateService custom = mock(BotChatStateService.class);

        runner.withBean(BotChatStateService.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(BotChatStateService.class);
                    assertThat(context.getBean(BotChatStateService.class)).isSameAs(custom);
                });
    }
}
