package com.fanzzi.backend.channel.util;

import java.text.Normalizer;
import java.util.Locale;

public class ChannelSlugUtil {

    public static String toSlugBase(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase(Locale.ROOT);
    }
}
