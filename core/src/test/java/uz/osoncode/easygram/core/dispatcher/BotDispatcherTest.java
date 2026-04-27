package uz.osoncode.easygram.core.dispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.osoncode.easygram.core.handler.BotHandler;
import uz.osoncode.easygram.core.handler.BotHandlerRegistry;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotDispatcherTest {

    @Mock
    private BotHandlerRegistry registry;

    private BotDispatcher dispatcher;
    private BotRequest request;
    private BotResponse response;

    @BeforeEach
    void setUp() {
        dispatcher = new BotDispatcher(registry);
        request = new BotRequest();
        response = new BotResponse();
    }

    @Test
    void dispatch_stateHandlerMatches_invokesStateHandler() throws Exception {
        BotHandler stateHandler = mock(BotHandler.class);
        when(stateHandler.supports(request)).thenReturn(true);
        when(registry.getStateHandlers()).thenReturn(List.of(stateHandler));

        dispatcher.dispatch(request, response);

        verify(stateHandler).handle(request, response);
    }

    @Test
    void dispatch_noStateMatch_specificHandlerMatches() throws Exception {
        BotHandler specific = mock(BotHandler.class);
        when(specific.supports(request)).thenReturn(true);
        when(registry.getStateHandlers()).thenReturn(List.of());
        when(registry.getBotHandlers()).thenReturn(List.of(specific));

        dispatcher.dispatch(request, response);

        verify(specific).handle(request, response);
    }

    @Test
    void dispatch_noSpecificMatch_defaultHandlerMatches() throws Exception {
        BotHandler fallback = mock(BotHandler.class);
        when(fallback.supports(request)).thenReturn(true);
        when(registry.getStateHandlers()).thenReturn(List.of());
        when(registry.getBotHandlers()).thenReturn(List.of());
        when(registry.getDefaultHandlers()).thenReturn(List.of(fallback));

        dispatcher.dispatch(request, response);

        verify(fallback).handle(request, response);
    }

    @Test
    void dispatch_noHandlerFound_throwsIllegalStateException() {
        when(registry.getStateHandlers()).thenReturn(List.of());
        when(registry.getBotHandlers()).thenReturn(List.of());
        when(registry.getDefaultHandlers()).thenReturn(List.of());

        assertThatThrownBy(() -> dispatcher.dispatch(request, response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No handler found");
    }

    @Test
    void dispatch_stateHandlerNotSupporting_specificHandlerInvoked() throws Exception {
        BotHandler stateHandler = mock(BotHandler.class);
        when(stateHandler.supports(request)).thenReturn(false);

        BotHandler specific = mock(BotHandler.class);
        when(specific.supports(request)).thenReturn(true);

        when(registry.getStateHandlers()).thenReturn(List.of(stateHandler));
        when(registry.getBotHandlers()).thenReturn(List.of(specific));

        dispatcher.dispatch(request, response);

        verify(stateHandler, never()).handle(any(), any());
        verify(specific).handle(request, response);
    }
}
