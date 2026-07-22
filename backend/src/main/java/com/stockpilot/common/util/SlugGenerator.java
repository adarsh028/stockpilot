package com.stockpilot.common.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugGenerator {

    private SlugGenerator() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "org";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");
        return slug.isBlank() ? "org" : slug;
    }
}
