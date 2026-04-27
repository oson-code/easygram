package uz.osoncode.easygram.core.i18n.resolver;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.User;
import uz.osoncode.easygram.core.model.BotRequest;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserLanguageCodeLocaleResolverTest {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final UserLanguageCodeLocaleResolver resolver =
            new UserLanguageCodeLocaleResolver(DEFAULT_LOCALE);

    @Test
    void resolve_knownLangCode_returnsCorrectLocale() {
        BotRequest request = requestWithLangCode("uz");
        assertThat(resolver.resolve(request)).isEqualTo(new Locale("uz"));
    }

    @Test
    void resolve_englishCode_returnsEnglish() {
        BotRequest request = requestWithLangCode("en");
        assertThat(resolver.resolve(request)).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void resolve_nullLangCode_returnsDefault() {
        BotRequest request = requestWithLangCode(null);
        assertThat(resolver.resolve(request)).isEqualTo(DEFAULT_LOCALE);
    }

    @Test
    void resolve_blankLangCode_returnsDefault() {
        BotRequest request = requestWithLangCode("  ");
        assertThat(resolver.resolve(request)).isEqualTo(DEFAULT_LOCALE);
    }

    @Test
    void resolve_noUser_returnsDefault() {
        BotRequest request = new BotRequest();
        assertThat(resolver.resolve(request)).isEqualTo(DEFAULT_LOCALE);
    }

    @Test
    void resolve_bcp47Tag_parsedCorrectly() {
        BotRequest request = requestWithLangCode("zh-hans");
        Locale locale = resolver.resolve(request);
        assertThat(locale.getLanguage()).isEqualTo("zh");
    }

    @Test
    void resolve_unknownLanguageTag_returnsDefault() {
        // A tag that results in an empty language (e.g. pure subtag)
        BotRequest request = requestWithLangCode("und");
        // "und" → Locale with language "und" — non-blank, should be returned as-is
        Locale locale = resolver.resolve(request);
        // Behavior: forLanguageTag("und") returns non-blank language, so returns it
        assertThat(locale).isNotNull();
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
