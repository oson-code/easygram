package uz.osoncode.easygram.core.returntypehandler;

import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.lang.reflect.Method;

/**
 * Convenience abstract base class for custom {@link BotReturnTypeHandler} implementations.
 *
 * <p>Extend this class rather than implementing {@link BotReturnTypeHandler} directly to get
 * sensible no-op defaults and clearer Javadoc guidance on which overloads to implement.</p>
 *
 * <h2>Implementing a custom return type handler</h2>
 * <ol>
 *   <li>Override {@link #supportsReturnType(Method)} to match your return type.</li>
 *   <li>Override {@link #handleReturnType(BotRequest, BotResponse, Object, Method)} if you
 *       need access to the originating method (e.g., to read annotations such as
 *       {@code @BotReplyMarkup}). Otherwise override the simpler three-argument
 *       {@link #handleReturnType(BotRequest, BotResponse, Object)}.</li>
 *   <li>Override {@link #supportsElement(Object)} only if your handler should also participate
 *       in heterogeneous-collection dispatch (see {@code BotMixedCollectionReturnTypeHandler}).
 *       Most custom handlers do NOT need this.</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Component
 * public class MyDomainReplyHandler extends BotReturnTypeHandlerAdapter {
 *
 *     @Override
 *     public boolean supportsReturnType(Method method) {
 *         return MyDomainReply.class.isAssignableFrom(method.getReturnType());
 *     }
 *
 *     @Override
 *     public void handleReturnType(BotRequest request, BotResponse response, Object value) {
 *         MyDomainReply reply = (MyDomainReply) value;
 *         response.addApiMethod(new SendMessage(reply.chatId(), reply.text()));
 *     }
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.7
 * @see BotReturnTypeHandler
 */
public abstract class BotReturnTypeHandlerAdapter implements BotReturnTypeHandler {

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation: returns {@code false}.
     * Override to enable element-level dispatch for heterogeneous collections.</p>
     */
    @Override
    public boolean supportsElement(Object element) {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation: delegates to the three-argument
     * {@link #handleReturnType(BotRequest, BotResponse, Object)}.
     * Override this method if you need to inspect annotations on the handler method.</p>
     */
    @Override
    public void handleReturnType(BotRequest botRequest, BotResponse botResponse, Object returnValue, Method method) {
        handleReturnType(botRequest, botResponse, returnValue);
    }
}
