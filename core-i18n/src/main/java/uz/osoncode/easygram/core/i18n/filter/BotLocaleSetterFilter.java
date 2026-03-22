package uz.osoncode.easygram.core.i18n.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import uz.osoncode.easygram.core.filter.BotFilter;
import uz.osoncode.easygram.core.filter.BotFilterChain;
import uz.osoncode.easygram.core.i18n.BotLocaleResolver;
import uz.osoncode.easygram.core.model.BotRequest;
import uz.osoncode.easygram.core.model.BotResponse;

import java.util.Locale;

/**
 * {@link BotFilter} that sets the resolved {@link Locale} on Spring's
 * {@link LocaleContextHolder} for the duration of the current bot request.
 *
 * <p>This makes the locale available to any Spring component that calls
 * {@link LocaleContextHolder#getLocale()} — including Spring's own
 * {@code MessageSource}, Spring MVC validators, and custom beans — without
 * requiring a direct reference to the {@link BotRequest}.</p>
 *
 * <p>The locale is always reset after the filter chain completes
 * (in a {@code finally} block) to prevent locale leakage across requests
 * on the same thread.</p>
 *
 * <p>Runs at order {@link Integer#MIN_VALUE} + 100, after
 * {@code uz.osoncode.easygram.core.filter.BotContextSetterFilter}
 * (which populates {@code BotRequest.user}) but before all application filters.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class BotLocaleSetterFilter implements BotFilter {

    private final BotLocaleResolver localeResolver;

    /**
     * Returns the order of this filter: {@link Integer#MIN_VALUE} + 100.
     *
     * @return filter order
     */
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 100;
    }

    /**
     * Resolves the locale, stores it on {@link LocaleContextHolder}, invokes the
     * remaining filter chain, and resets the locale context afterwards.
     *
     * @param botRequest  the current bot request
     * @param botResponse the mutable response accumulator
     * @param filterChain the remaining filter chain
     */
    @Override
    public void doFilter(BotRequest botRequest, BotResponse botResponse, BotFilterChain filterChain) {
        Locale locale = localeResolver.resolve(botRequest);
        LocaleContextHolder.setLocale(locale);
        try {
            filterChain.doFilter(botRequest, botResponse);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
