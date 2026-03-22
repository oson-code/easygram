package uz.osoncode.easygram.core.markup;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe in-memory implementation of {@link BotMarkupRegistry}.
 *
 * <p>Stores markup factories in two separate {@link ConcurrentHashMap}s:</p>
 * <ul>
 *   <li>{@code factories} — keyed by markup ID (registered via {@code @BotMarkup})</li>
 *   <li>{@code stateFactories} — keyed by chat state name (registered when a
 *       {@code @BotMarkup} method is also annotated with {@code @BotChatState})</li>
 * </ul>
 *
 * <p>State is local to the JVM and not shared across instances. For distributed
 * deployments, provide a custom {@link BotMarkupRegistry} {@code @Bean} to
 * replace this default.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public class InMemoryBotMarkupRegistry implements BotMarkupRegistry {

    private final ConcurrentHashMap<String, Function<BotRequest, ReplyKeyboard>> factories =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Function<BotRequest, ReplyKeyboard>> stateFactories =
            new ConcurrentHashMap<>();

    @Override
    public void register(String id, Function<BotRequest, ReplyKeyboard> factory) {
        factories.put(id, factory);
    }

    @Override
    public ReplyKeyboard resolve(String id, BotRequest request) {
        Function<BotRequest, ReplyKeyboard> factory = factories.get(id);
        return factory != null ? factory.apply(request) : null;
    }

    @Override
    public boolean contains(String id) {
        return factories.containsKey(id);
    }

    @Override
    public void registerForState(String state, Function<BotRequest, ReplyKeyboard> factory) {
        stateFactories.put(state, factory);
    }

    @Override
    public ReplyKeyboard resolveByState(String state, BotRequest request) {
        Function<BotRequest, ReplyKeyboard> factory = stateFactories.get(state);
        return factory != null ? factory.apply(request) : null;
    }
}
