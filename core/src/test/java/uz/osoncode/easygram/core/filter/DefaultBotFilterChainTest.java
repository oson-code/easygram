package uz.osoncode.easygram.core.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.osoncode.easygram.core.dispatcher.BotDispatcher;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionHandlerRegistry;
import uz.osoncode.easygram.core.exceptionhandler.BotExceptionMethodHandler;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultBotFilterChainTest {

    @Mock
    private BotDispatcher dispatcher;

    @Mock
    private BotExceptionHandlerRegistry exceptionRegistry;

    private BotRequest request;
    private BotResponse response;

    @BeforeEach
    void setUp() {
        request = new BotRequest();
        response = new BotResponse();
    }

    @Test
    void noFilters_delegatesToDispatcher() throws Exception {
        DefaultBotFilterChain chain = new DefaultBotFilterChain(List.of(), dispatcher, exceptionRegistry);
        chain.doFilter(request, response);
        verify(dispatcher).dispatch(request, response);
    }

    @Test
    void singleFilter_isInvokedThenDispatcher() throws Exception {
        BotFilter filter = mock(BotFilter.class);
        when(filter.shouldFilter(any(), any())).thenReturn(true);
        // make the mock filter forward to next chain link so dispatcher is reached
        doAnswer(inv -> {
            BotFilterChain next = inv.getArgument(2);
            next.doFilter(request, response);
            return null;
        }).when(filter).doFilter(eq(request), eq(response), any(BotFilterChain.class));

        DefaultBotFilterChain chain = new DefaultBotFilterChain(List.of(filter), dispatcher, exceptionRegistry);
        chain.doFilter(request, response);

        verify(filter).doFilter(eq(request), eq(response), any(BotFilterChain.class));
        verify(dispatcher).dispatch(request, response);
    }

    @Test
    void filter_shouldFilterFalse_isSkipped() throws Exception {
        BotFilter filter = mock(BotFilter.class);
        when(filter.shouldFilter(any(), any())).thenReturn(false);

        DefaultBotFilterChain chain = new DefaultBotFilterChain(List.of(filter), dispatcher, exceptionRegistry);
        chain.doFilter(request, response);

        verify(filter, never()).doFilter(any(), any(), any());
        verify(dispatcher).dispatch(request, response);
    }

    @Test
    void dispatcherThrows_exceptionHandlerInvoked() throws Exception {
        RuntimeException ex = new RuntimeException("dispatch failed");
        doThrow(ex).when(dispatcher).dispatch(any(), any());

        @SuppressWarnings("unchecked")
        BotExceptionMethodHandler<RuntimeException> exHandler = mock(BotExceptionMethodHandler.class);
        when(exHandler.supports(ex, request)).thenReturn(true);
        when(exceptionRegistry.getBotHandlers()).thenReturn(List.of(exHandler));

        DefaultBotFilterChain chain = new DefaultBotFilterChain(List.of(), dispatcher, exceptionRegistry);
        chain.doFilter(request, response);

        verify(exHandler).handle(request, response, ex);
    }

    @Test
    void dispatcherThrows_noHandler_rethrows() throws Exception {
        RuntimeException ex = new RuntimeException("unhandled");
        doThrow(ex).when(dispatcher).dispatch(any(), any());
        when(exceptionRegistry.getBotHandlers()).thenReturn(List.of());

        DefaultBotFilterChain chain = new DefaultBotFilterChain(List.of(), dispatcher, exceptionRegistry);
        // Fix #4: must rethrow when no exception handler matches
        assertThatThrownBy(() -> chain.doFilter(request, response))
                .isSameAs(ex);
        assertThat(request.getThrowable()).isSameAs(ex);
    }
}
