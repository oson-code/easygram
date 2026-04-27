package uz.osoncode.easygram.core.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import uz.osoncode.easygram.core.i18n.resolver.UserLanguageCodeLocaleResolver;
import uz.osoncode.easygram.core.model.BotRequest;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BotMessageSourceTest {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final StaticMessageSource delegate = new StaticMessageSource();
    private final UserLanguageCodeLocaleResolver localeResolver =
            new UserLanguageCodeLocaleResolver(DEFAULT_LOCALE);
    private final BotMessageSource messageSource = new BotMessageSource(delegate, localeResolver);

    @Test
    void getMessage_englishUser_returnsEnglishMessage() {
        delegate.addMessage("welcome", Locale.ENGLISH, "Welcome!");
        BotRequest request = requestWithLangCode("en");
        assertThat(messageSource.getMessage("welcome", request)).isEqualTo("Welcome!");
    }

    @Test
    void getMessage_withArgs_substitutesArgs() {
        delegate.addMessage("greeting", Locale.ENGLISH, "Hello {0}!");
        BotRequest request = requestWithLangCode("en");
        assertThat(messageSource.getMessage("greeting", request, "Alice")).isEqualTo("Hello Alice!");
    }

    @Test
    void getMessage_noUser_fallsBackToDefault() {
        delegate.addMessage("msg", DEFAULT_LOCALE, "Default message");
        BotRequest request = new BotRequest(); // no user
        assertThat(messageSource.getMessage("msg", request)).isEqualTo("Default message");
    }

    @Test
    void getMessage_withExplicitLocale_usesLocale() {
        delegate.addMessage("bye", new Locale("uz"), "Xayr!");
        assertThat(messageSource.getMessage("bye", new Locale("uz"))).isEqualTo("Xayr!");
    }

    @Test
    void getMessage_unknownLangCode_fallsToDefaultLocale() {
        delegate.addMessage("hi", DEFAULT_LOCALE, "Hi!");
        BotRequest request = requestWithLangCode(null);
        assertThat(messageSource.getMessage("hi", request)).isEqualTo("Hi!");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static BotRequest requestWithLangCode(String langCode) {
        User user = mock(User.class);
        when(user.getLanguageCode()).thenReturn(langCode);
        BotRequest request = new BotRequest();
        request.setUser(user);
        return request;
    }
}
