package dev.bkclient.ui.clicksys.port.util;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public final class OptionSearchIndex {
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private final List<String> normalized;
    private String lastQuery, rawQuery;
    private int[] results = new int[0];
    public OptionSearchIndex(List<String> texts) { normalized = texts.stream().map(OptionSearchIndex::normalize).toList(); }
    public static String normalize(String text) {
        return MARKS.matcher(Normalizer.normalize(text.toLowerCase(Locale.ROOT).replace('ł', 'l'), Normalizer.Form.NFD))
                .replaceAll("").trim();
    }
    public int[] filter(String query) {
        if (query.equals(rawQuery)) return results;
        rawQuery = query;
        String value = normalize(query);
        if (!value.equals(lastQuery)) {
            lastQuery = value;
            String[] terms = value.isEmpty() ? new String[0] : value.split("\\s+");
            results = java.util.stream.IntStream.range(0, normalized.size())
                    .filter(i -> { for (String term : terms) if (!normalized.get(i).contains(term)) return false; return true; })
                    .toArray();
        }
        return results;
    }
    public static int firstVisible(float scroll, float rowHeight) { return Math.max(0, (int)(scroll / rowHeight) - 2); }
    public static int endVisible(float scroll, float height, float rowHeight, int count) {
        return Math.min(count, (int)Math.ceil((scroll + height) / rowHeight) + 2);
    }
}
