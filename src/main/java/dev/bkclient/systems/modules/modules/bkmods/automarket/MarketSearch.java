package dev.bkclient.systems.modules.modules.bkmods.automarket;

import dev.bkclient.utility.inventory.ItemStackExtractor;
import java.text.Normalizer;
import java.util.Locale;

/** UI filtering only: does not alter stored names or purchase requirements. */
public final class MarketSearch {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("&(?=[0-9A-FK-ORXa-fk-orx])");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile("\\p{M}");
    private static final java.util.regex.Pattern BK_RE_2 = java.util.regex.Pattern.compile("\\s+");
    private MarketSearch() {}

    public static String normalize(String text) {
        if (text == null) return "";
        String readable = ItemStackExtractor.toReadablePlain(
                BK_RE_0.matcher(text).replaceAll("\u00a7"));
        return BK_RE_2.matcher(BK_RE_1.matcher(Normalizer.normalize(readable, Normalizer.Form.NFD)).replaceAll("")
                .replace('\u0142', 'l').replace('\u0141', 'L')
                .replace('\u202f', ' ')).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
    }

    public static boolean matches(String name, String material, String query) {
        String normalizedQuery = normalize(query);
        return normalizedQuery.isEmpty()
                || normalize(name).contains(normalizedQuery)
                || normalize(material).contains(normalizedQuery);
    }
}
