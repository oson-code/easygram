package uz.osoncode.easygram.core.chatstate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InMemoryBotChatStateServiceTest {

    private InMemoryBotChatStateService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryBotChatStateService();
    }

    @Test
    void getState_noState_returnsNull() {
        assertThat(service.getState(1L)).isNull();
    }

    @Test
    void setState_thenGetState_returnsState() {
        service.setState(1L, "WAITING_INPUT");
        assertThat(service.getState(1L)).isEqualTo("WAITING_INPUT");
    }

    @Test
    void setState_nullState_throwsIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.setState(1L, (String) null));
    }

    @Test
    void setState_overwrite_returnsNewState() {
        service.setState(1L, "FIRST");
        service.setState(1L, "SECOND");
        assertThat(service.getState(1L)).isEqualTo("SECOND");
    }

    @Test
    void clearState_existingChat_returnsNull() {
        service.setState(1L, "WAITING");
        service.clearState(1L);
        assertThat(service.getState(1L)).isNull();
    }

    @Test
    void clearState_unknownChat_doesNotThrow() {
        service.clearState(999L); // should be no-op
    }

    @Test
    void multipleChats_stateIsolated() {
        service.setState(1L, "STATE_A");
        service.setState(2L, "STATE_B");
        assertThat(service.getState(1L)).isEqualTo("STATE_A");
        assertThat(service.getState(2L)).isEqualTo("STATE_B");
    }
}
