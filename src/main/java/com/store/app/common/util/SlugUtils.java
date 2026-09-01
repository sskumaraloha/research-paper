package com.store.app.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * URL-slug generation shared by catalog entities.
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    /**
     * Lower-cases, strips accents, and collapses everything that is not
     * a letter or digit into single hyphens.
     *
     * @param fallback returned when nothing slug-worthy remains
     */
    public static String slugify(String input, String fallback) {
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? fallback : slug;
    }
}
