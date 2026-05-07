package uz.osoncode.easygram.core.chatstate;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer-backed implementation of {@link BotChatStateMetrics}.
 *
 * <p>This class is the <em>only</em> class in the {@code core-chatstate} module that imports
 * Micrometer types. It is loaded exclusively from the
 * {@code ChatStateAutoConfiguration.WithMicrometerConfig} inner configuration, which is
 * guarded by {@code @ConditionalOnClass} so this class is never instantiated — and never
 * even loaded by the JVM — when {@code micrometer-core} is absent from the classpath.</p>
 *
 * <p>Registers the following counters against the supplied {@link MeterRegistry}:</p>
 * <ul>
 *   <li>{@code easygram.chatstate.get} tagged {@code result=hit} — successful look-ups.</li>
 *   <li>{@code easygram.chatstate.get} tagged {@code result=miss} — look-ups with no state.</li>
 *   <li>{@code easygram.chatstate.set} — state writes.</li>
 *   <li>{@code easygram.chatstate.clear} — state removals.</li>
 * </ul>
 *
 * @author Islom Mirsaburov
 * @since 0.0.7
 */
public class MicrometerBotChatStateMetrics implements BotChatStateMetrics {

    private final Counter getHitCounter;
    private final Counter getMissCounter;
    private final Counter setCounter;
    private final Counter clearCounter;

    /**
     * Creates a new instance and registers all counters against the given registry.
     *
     * @param registry the Micrometer {@link MeterRegistry} to register counters with
     */
    public MicrometerBotChatStateMetrics(MeterRegistry registry) {
        this.getHitCounter  = Counter.builder("easygram.chatstate.get").tag("result", "hit").register(registry);
        this.getMissCounter = Counter.builder("easygram.chatstate.get").tag("result", "miss").register(registry);
        this.setCounter     = Counter.builder("easygram.chatstate.set").register(registry);
        this.clearCounter   = Counter.builder("easygram.chatstate.clear").register(registry);
    }

    @Override
    public void recordGet(boolean hit) {
        if (hit) getHitCounter.increment(); else getMissCounter.increment();
    }

    @Override
    public void recordSet() {
        setCounter.increment();
    }

    @Override
    public void recordClear() {
        clearCounter.increment();
    }
}
