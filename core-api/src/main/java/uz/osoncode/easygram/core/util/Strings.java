package uz.osoncode.easygram.core.util;

/**
 * Null-safe utility methods for {@link String} values.
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
public final class Strings {

    private Strings() {}

    /**
     * Returns {@code true} if the string is {@code null}, empty, or contains only whitespace.
     *
     * @param value the string to test; may be {@code null}
     * @return {@code true} if blank or {@code null}
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Returns {@code true} if the string is not {@code null} and contains at least one
     * non-whitespace character.
     *
     * @param value the string to test; may be {@code null}
     * @return {@code true} if not blank and not {@code null}
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}
